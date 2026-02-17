// Copyright 2025-2026 Oğuzhan Topaloğlu
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.twistral.konoblo;


import java.io.PrintStream;
import java.math.*;
import java.util.*;
import java.util.function.*;

import static com.twistral.konoblo.CommonRestrictors.*;


public class KonobloConsole {

    public static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    // Lifecycle Related Objects
    private String greetingText;
    private Runnable exitFunction; // Always run at the end of each program
    private Runnable terminateFunction; // Only run when the program is intentionally terminated

    // State Related Objects
    private final HashMap<String, Consumer<KonobloConsole>> stateFunctions;
    private final HashMap<String, Supplier<String>> stateDirectors;
    private final Stack<String> stateStack;

    // Data (Object Instance) Storage
    private final HashMap<String, Object> storage;

    // IO Objects
    private final PrintStream outStream, errStream;
    private final boolean ownsStreams;

    private final Scanner scanner;
    private final boolean ownsScanner;


    /*//////////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  CONSTRUCTORS  ///////////////////////////*/
    /*//////////////////////////////////////////////////////////////////////*/


    private KonobloConsole(PrintStream outStream, PrintStream errStream, boolean ownsStreams,
                           Scanner scanner, boolean ownsScanner)
    {
        this.ownsStreams = ownsStreams;
        this.ownsScanner = ownsScanner;
        this.outStream = outStream;
        this.errStream = errStream;
        this.scanner = scanner;

        this.greetingText = DEF_GREETING_TEXT;
        this.storage = new HashMap<>(64);
        this.stateFunctions = new HashMap<>(64);
        this.stateDirectors = new HashMap<>(64);
        this.stateStack = new Stack<>();
        this.terminateFunction = () -> {};
        this.exitFunction = () -> {};
    }

    public KonobloConsole(PrintStream outStream, PrintStream errStream) {
        this(outStream, errStream, true, new Scanner(System.in), false);
    }

    public KonobloConsole(PrintStream outAndErrStream) {
        this(outAndErrStream, outAndErrStream, true, new Scanner(System.in), false);
    }

    public KonobloConsole(PrintStream outStream, PrintStream errStream, Scanner scanner) {
        this(outStream, errStream, true, scanner, true);
    }

    public KonobloConsole(PrintStream outAndErrStream, Scanner scanner) {
        this(outAndErrStream, outAndErrStream, true, scanner, true);
    }

    public KonobloConsole() {
        this(System.out, System.err, false, new Scanner(System.in), false);
    }


    /*///////////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  STATE METHODS  ///////////////////////////*/
    /*///////////////////////////////////////////////////////////////////////*/


    public void define(String stateID, Consumer<KonobloConsole> stateFunction) {
        Objects.requireNonNull(stateID, "stateID");
        Objects.requireNonNull(stateFunction, "stateFunction");

        // Will replace the old function if this was an already defined state
        this.stateFunctions.put(stateID, stateFunction);
    }


    // This Exception subclass is intentionally used to signal termination
    private static final class KonobloTerminateSignal extends RuntimeException {
        KonobloTerminateSignal() {
            // no message, no throwable chaining, no stacktrace => NO COST
            super(null, null, false, false);
        }
    }


    public void run(String entryStateID) {
        Objects.requireNonNull(entryStateID, "entryStateID");
        this.printlnIfValid(this.greetingText);

        try {
            this.stateStack.push(entryStateID);

            while (true) {
                String currentStateID = stateStack.peek();
                if (!this.stateFunctions.containsKey(currentStateID)) {
                    throw new KonobloException("State %s is not defined.", currentStateID);
                }

                // Run the current state's function
                Consumer<KonobloConsole> function = stateFunctions.get(currentStateID);
                function.accept(this);

                // Use the director to get the next ID and push it to the stateStack
                boolean exit = !stateDirectors.containsKey(currentStateID);
                if (exit) break;

                Supplier<String> director = stateDirectors.get(currentStateID);
                final String nextStateID = director.get();
                if (nextStateID == null) break;

                this.stateStack.push(nextStateID);
            }
        }
        catch (KonobloTerminateSignal ignored) {
            this.terminateFunction.run(); // intentional fast-exit
        }

        // Run the special and final exit function
        this.exitFunction.run();

        // Clean-up / close streams if needed
        if (ownsStreams) {
            boolean usesSameStream = outStream.equals(errStream);
            this.outStream.close();
            if (!usesSameStream) {
                this.errStream.close();
            }
        }

        // Clean-up / close scanner if needed
        if (ownsScanner) {
            this.scanner.close();
        }
    }


    public void direct(String stateID, Supplier<String> director) {
        Objects.requireNonNull(stateID, "stateID");
        Objects.requireNonNull(director, "director");

        // Only one director is allowed for each state
        if (this.stateDirectors.containsKey(stateID))
            throw new KonobloException("A director for stateID=%s is already defined!", stateID);

        this.stateDirectors.put(stateID, director);
    }


    public void direct(String srcStateID, String destStateID) {
        Objects.requireNonNull(destStateID, "destStateID");
        this.direct(srcStateID, () -> destStateID);
    }


    public void directBack(String stateID, int n) {
        final Supplier<String> director = () -> {
            final int m = this.stateStack.size();
            final int nextIndex = m - n - 1;

            if (nextIndex < 0) {
                throw new KonobloException("Stack cant go back %d times when it has %d items!", n, m);
            }

            return this.stateStack.get(nextIndex);
        };

        this.direct(stateID, director);
    }


    public static final class Option<K> {
        final K key;
        final String stateID;

        Option(K key, String stateID) {
            this.key = key;
            this.stateID = stateID;
        }
    }

    public Option<Integer> option(int key, String stateID) {
        return new Option<Integer>(key, stateID);
    }

    public Option<String> option(String key, String stateID) {
        return new Option<String>(key, stateID);
    }

    @SafeVarargs // to get rid of heap pollution warnings
    public final void directStrSelect(String stateID, String retryText,
                                      String restrictFailText, Option<String>... options)
    {
        Objects.requireNonNull(options, "options");

        Map<String, String> routes = new LinkedHashMap<>();
        for (Option<String> option : options) {
            Objects.requireNonNull(option, "option");
            if (routes.containsKey(option.key)) {
                throw new KonobloException("Duplicate option key: %s.", option.key);
            }
            routes.put(option.key, option.stateID);
        }

        this.direct(stateID, () -> {
            String input = this.requireString(retryText, routes::containsKey, restrictFailText);
            return routes.get(input);
        });
    }

    @SafeVarargs // to get rid of heap pollution warnings
    public final void directIntSelect(String stateID, String retryText,
                                      String restrictFailText, Option<Integer>... options)
    {
        Objects.requireNonNull(options, "options");

        Map<Integer, String> routes = new LinkedHashMap<>();
        for (Option<Integer> option : options) {
            Objects.requireNonNull(option, "option");
            if (routes.containsKey(option.key)) {
                throw new KonobloException("Duplicate option key: %s.", option.key);
            }
            routes.put(option.key, option.stateID);
        }

        this.direct(stateID, () -> {
            int input = this.requireInt(retryText, routes::containsKey, restrictFailText);
            return routes.get(input);
        });
    }


    /*/////////////////////////////////////////////////////////////////*/
    /*///////////////////////  STORAGE METHODS  ///////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/


    public void storeObject(String objectID, Object object) {
        this.storage.put(objectID, object);
    }


    public <T> T getObject(String objectID, Class<T> objectClass) {
        if (!this.storage.containsKey(objectID)) {
            throw new KonobloException("ID=%s was not found in the storage.", objectID);
        }

        final Object object = this.storage.get(objectID);

        if (!objectClass.isInstance(object)) {
            throw new KonobloException(
                "ID=%s holds %s, cannot cast into %s.", objectID,
                object.getClass().getName(), objectClass.getName()
            );
        }

        return objectClass.cast(object);
    }


    public void removeObject(String objectID) {
        storage.remove(objectID);
    }


    public void clearObjects() {
        storage.clear();
    }


    /*//////////////////////////////////////////////////////////////////////*/
    /*/////////////////////  PRINTING & ERROR METHODS  /////////////////////*/
    /*//////////////////////////////////////////////////////////////////////*/

    // IMPORTANT NOTE: instead of overriding every print(...) and println(...) function inside
    // PrintStream class I only added foo(Object) and foo() methods for simplicity. This wont cause
    // any performance problems since PrintStream's API was written in 1995 and Java has autoboxing
    // since that time. These methods point to the same functions in one way or another anyways.

    /* PRINTING METHODS: BINDINGS FOR OUTSTREAM */
    public void printf(Locale l, String format, Object... args) { outStream.printf(l, format, args); }
    public void printf(String format, Object... args) { outStream.printf(format, args); }
    public void println(Object x) { outStream.println(x); }
    public void println() { outStream.println(); }
    public void print(Object x) { outStream.print(x); }

    /* ERROR METHODS: BINDINGS FOR ERRSTREAM */
    public void errorf(Locale l, String format, Object... args) { errStream.printf(l, format, args); }
    public void errorf(String format, Object... args) { errStream.printf(format, args); }
    public void errorln(Object x) { errStream.println(x); }
    public void errorln() { errStream.println(); }
    public void error(Object x) { errStream.print(x); }


    /*/////////////////////////////////////////////////////////////*/
    /*/////////////////////  READING METHODS  /////////////////////*/
    /*/////////////////////////////////////////////////////////////*/

    // The following methods can and will throw an exception if something goes wrong
    // For %100 safe input reading use requiring methods

    public String readString() {
        return scanner.nextLine();
    }

    public double readDouble() {
        return Double.parseDouble(this.readString().trim());
    }

    public float readFloat() {
        return Float.parseFloat(this.readString().trim());
    }

    public BigDecimal readBigDecimal() {
        return new BigDecimal(this.readString().trim());
    }

    public boolean readBoolean(String trueString, String falseString, boolean ignoreCase) {
        if (trueString == null || falseString == null)
            throw new KonobloException("Invalid parameters for readBoolean.");
        if (trueString.equals(falseString))
            throw new KonobloException("trueString and falseString can't be the same.");
        if (trueString.trim().isEmpty() || falseString.trim().isEmpty())
            throw new KonobloException("trueString and falseString can't be empty.");

        final String input = this.readString().trim(); // can NEVER be null

        if (ignoreCase) {
            if (input.equalsIgnoreCase(trueString)) return true;
            if (input.equalsIgnoreCase(falseString)) return false;
        }
        else {
            if (input.equals(trueString)) return true;
            if (input.equals(falseString)) return false;
        }

        throw new InputMismatchException();
    }

    public boolean readBoolean() {
        return this.readBoolean("true", "false", true);
    }

    public byte readByte(int radix) {
        return Byte.parseByte(this.readString().trim(), radix);
    }

    public int readInt(int radix) {
        return Integer.parseInt(this.readString().trim(), radix);
    }

    public BigInteger readBigInteger(int radix) {
        return new BigInteger(this.readString().trim(), radix);
    }

    public long readLong(int radix) {
        return Long.parseLong(this.readString().trim(), radix);
    }

    public short readShort(int radix) {
        return Short.parseShort(this.readString().trim(), radix);
    }

    public byte readByte() { return this.readByte(10); }
    public int readInt() { return this.readInt(10); }
    public BigInteger readBigInteger() { return this.readBigInteger(10); }
    public long readLong() { return this.readLong(10); }
    public short readShort() { return this.readShort(10); }


    /*///////////////////////////////////////////////////////////////*/
    /*/////////////////////  REQUIRING METHODS  /////////////////////*/
    /*///////////////////////////////////////////////////////////////*/


    // This Exception subclass is intentionally used to reject inputs inside requireCore method
    private static final class KonobloInputReject extends RuntimeException {
        KonobloInputReject() {
            // no message, no throwable chaining, no stacktrace => NO COST
            super(null, null, false, false);
        }
    }


    private <T> T requireCore(Supplier<T> supplier,
                              Predicate<T> restrictor, String restrictFailText,
                              boolean useDefaultValue, T defaultValue,
                              String catchText, boolean doTerminate)
    {
        while (true) {
            try {
                T input = supplier.get();

                if (restrictor != null) { // restriction will happen
                    if (!restrictor.test(input)) {
                        // restriction FAILED: bad input
                        this.printlnIfValid(restrictFailText);
                        throw new KonobloInputReject();
                    }
                }

                return input;
            }
            // Catch #1 InputMismatchException: Real bad input was given (recoverable)
            //          KonobloInputReject: Good input was given but rejected via restrictions
            catch (InputMismatchException | KonobloInputReject e) {
                if (scanner.hasNextLine()) {
                    this.scanner.nextLine(); // CONSUME INVALID INPUT
                }

                if (useDefaultValue) {
                    return defaultValue;
                }

                this.printIfValid(catchText);

                if (doTerminate) {
                    throw new KonobloTerminateSignal();
                }
            }
            // Catch #2 NoSuchElementException: Input is exhausted (cannot recover, can terminate)
            catch (NoSuchElementException e) {
                throw new KonobloTerminateSignal();
            }
            // Catch #3 IllegalStateException: Scanner is broken/closed (cannot recover or terminate)
            catch (IllegalStateException e) {
                throw e;
            }
        }
    }


    /*//////////////////////// REQUIRE - ALL PARAMETER FUNCTIONS ////////////////////////*/

    public boolean requireBoolean(String retryText, String trueString,
                                  String falseString, boolean ignoreCase)
    {
        return this.requireCore(
                () -> this.readBoolean(trueString, falseString, ignoreCase), null, null,
                false, false, retryText, false
        );
    }

    public boolean requireBoolean(String retryText) {
        return this.requireCore(
                () -> this.readBoolean(), null, null, false, false, retryText, false
        );
    }

    public int requireInt(String retryText, Predicate<Integer> restrictor,
                          String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readInt(radix), restrictor, restrictFailText, false, 0, retryText, false
        );
    }

    public long requireLong(String retryText, Predicate<Long> restrictor,
                            String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readLong(radix), restrictor, restrictFailText, false, 0L, retryText, false
        );
    }

    public byte requireByte(String retryText, Predicate<Byte> restrictor,
                            String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readByte(radix), restrictor, restrictFailText,
                false, (byte)0, retryText, false
        );
    }

    public short requireShort(String retryText, Predicate<Short> restrictor,
                              String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readShort(radix), restrictor, restrictFailText,
                false, (short)0, retryText, false
        );
    }

    public BigInteger requireBigInteger(String retryText, Predicate<BigInteger> restrictor,
                                        String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readBigInteger(radix), restrictor, restrictFailText,
                false, null, retryText, false
        );
    }

    public String requireString(String retryText, Predicate<String> restrictor,
                                String restrictFailText)
    {
        return this.requireCore(
                () -> this.readString(), restrictor, restrictFailText, false, null, retryText, false
        );
    }

    public BigDecimal requireBigDecimal(String retryText, Predicate<BigDecimal> restrictor,
                                        String restrictFailText)
    {
        return this.requireCore(
                () -> this.readBigDecimal(), restrictor, restrictFailText, false, null, retryText, false
        );
    }

    public double requireDouble(String retryText, Predicate<Double> restrictor,
                                String restrictFailText)
    {
        return this.requireCore(
                () -> this.readDouble(), restrictor, restrictFailText, false, 0d, retryText, false
        );
    }

    public float requireFloat(String retryText, Predicate<Float> restrictor,
                              String restrictFailText)
    {
        return this.requireCore(
                () -> this.readFloat(), restrictor, restrictFailText, false, 0f, retryText, false
        );
    }

    /*//////////////////////////////////*/

    public boolean requireBooleanDef(boolean defValue, String trueString,
                                     String falseString, boolean ignoreCase)
    {
        return this.requireCore(
            () -> this.readBoolean(trueString, falseString, ignoreCase), null, null,
            true, defValue, null, false
        );
    }


    public boolean requireBooleanDef(boolean defValue) {
        return this.requireCore(
                () -> this.readBoolean(), null, null, true, defValue, null, false
        );
    }

    public int requireIntDef(int defValue, Predicate<Integer> restrictor,
                             String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readInt(radix), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public long requireLongDef(long defValue, Predicate<Long> restrictor,
                               String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readLong(radix), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public byte requireByteDef(byte defValue, Predicate<Byte> restrictor,
                               String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readByte(radix), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public short requireShortDef(short defValue, Predicate<Short> restrictor,
                                 String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readShort(radix), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public BigInteger requireBigIntegerDef(BigInteger defValue, Predicate<BigInteger> restrictor,
                                           String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readBigInteger(radix), restrictor, restrictFailText,
                true, defValue, null, false
        );
    }

    public String requireStringDef(String defValue, Predicate<String> restrictor,
                                   String restrictFailText)
    {
        return this.requireCore(
                () -> this.readString(), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public BigDecimal requireBigDecimalDef(BigDecimal defValue, Predicate<BigDecimal> restrictor,
                                           String restrictFailText)
    {
        return this.requireCore(
                () -> this.readBigDecimal(), restrictor, restrictFailText,
                true, defValue, null, false
        );
    }

    public double requireDoubleDef(double defValue, Predicate<Double> restrictor,
                                   String restrictFailText)
    {
        return this.requireCore(
                () -> this.readDouble(), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    public float requireFloatDef(float defValue, Predicate<Float> restrictor,
                                 String restrictFailText)
    {
        return this.requireCore(
                () -> this.readFloat(), restrictor, restrictFailText, true, defValue, null, false
        );
    }

    /*//////////////////////////////////*/

    public boolean requireBooleanTerm(String terminationText, String trueString,
                                      String falseString, boolean ignoreCase)
    {
        return this.requireCore(
            () -> this.readBoolean(trueString, falseString, ignoreCase), null, null,
            false, false, terminationText, true
        );
    }

    public boolean requireBooleanTerm(String terminationText) {
        return this.requireCore(
                () -> this.readBoolean(), null, null, false, false, terminationText, true
        );
    }

    public int requireIntTerm(String terminationText, Predicate<Integer> restrictor,
                              String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readInt(radix), restrictor, restrictFailText, false, 0, terminationText, true
        );
    }

    public long requireLongTerm(String terminationText, Predicate<Long> restrictor,
                                String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readLong(radix), restrictor, restrictFailText, false, 0L, terminationText, true
        );
    }

    public byte requireByteTerm(String terminationText, Predicate<Byte> restrictor,
                                String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readByte(radix), restrictor, restrictFailText,
                false, (byte)0, terminationText, true
        );
    }

    public short requireShortTerm(String terminationText, Predicate<Short> restrictor,
                                  String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readShort(radix), restrictor, restrictFailText,
                false, (short)0, terminationText, true
        );
    }

    public BigInteger requireBigIntegerTerm(String terminationText, Predicate<BigInteger> restrictor,
                                            String restrictFailText, int radix)
    {
        return this.requireCore(
                () -> this.readBigInteger(radix), restrictor, restrictFailText,
                false, null, terminationText, true
        );
    }

    public String requireStringTerm(String terminationText, Predicate<String> restrictor,
                                    String restrictFailText)
    {
        return this.requireCore(
                () -> this.readString(), restrictor, restrictFailText, false, null, terminationText, true
        );
    }

    public BigDecimal requireBigDecimalTerm(String terminationText, Predicate<BigDecimal> restrictor,
                                            String restrictFailText)
    {
        return this.requireCore(
                () -> this.readBigDecimal(), restrictor, restrictFailText,
                false, null, terminationText, true
        );
    }

    public double requireDoubleTerm(String terminationText, Predicate<Double> restrictor,
                                    String restrictFailText)
    {
        return this.requireCore(
                () -> this.readDouble(), restrictor, restrictFailText, false, 0d, terminationText, true
        );
    }

    public float requireFloatTerm(String terminationText, Predicate<Float> restrictor,
                                  String restrictFailText)
    {
        return this.requireCore(
                () -> this.readFloat(), restrictor, restrictFailText, false, 0f, terminationText, true
        );
    }


    /*//////////////////////// REQUIRE - CONVENIENCE FUNCTIONS ////////////////////////*/


    public int requireInt(String retryText, Predicate<Integer> restrictor, String restrictFailText) {
        return this.requireInt(retryText, restrictor, restrictFailText, 10);
    }

    public int requireInt(String retryText) {
        return this.requireInt(retryText, null, null, 10);
    }

    public long requireLong(String retryText, Predicate<Long> restrictor, String restrictFailText) {
        return this.requireLong(retryText, restrictor, restrictFailText, 10);
    }

    public long requireLong(String retryText) {
        return this.requireLong(retryText, null, null, 10);
    }

    public byte requireByte(String retryText, Predicate<Byte> restrictor, String restrictFailText) {
        return this.requireByte(retryText, restrictor, restrictFailText, 10);
    }

    public byte requireByte(String retryText) {
        return this.requireByte(retryText, null, null, 10);
    }

    public short requireShort(String retryText, Predicate<Short> restrictor, String restrictFailText) {
        return this.requireShort(retryText, restrictor, restrictFailText, 10);
    }

    public short requireShort(String retryText) {
        return this.requireShort(retryText, null, null, 10);
    }

    public BigInteger requireBigInteger(String retryText, Predicate<BigInteger> restrictor,
                                        String restrictFailText)
    {
        return this.requireBigInteger(retryText, restrictor, restrictFailText, 10);
    }

    public BigInteger requireBigInteger(String retryText) {
        return this.requireBigInteger(retryText, null, null, 10);
    }

    public int requireIntDef(int defValue, Predicate<Integer> restrictor, String restrictFailText) {
        return this.requireIntDef(defValue, restrictor, restrictFailText, 10);
    }

    public int requireIntDef(int defValue) {
        return this.requireIntDef(defValue, null, null, 10);
    }

    public long requireLongDef(long defValue, Predicate<Long> restrictor, String restrictFailText) {
        return this.requireLongDef(defValue, restrictor, restrictFailText, 10);
    }

    public long requireLongDef(long defValue) {
        return this.requireLongDef(defValue, null, null, 10);
    }

    public byte requireByteDef(byte defValue, Predicate<Byte> restrictor, String restrictFailText) {
        return this.requireByteDef(defValue, restrictor, restrictFailText, 10);
    }

    public byte requireByteDef(byte defValue) {
        return this.requireByteDef(defValue, null, null, 10);
    }

    public short requireShortDef(short defValue, Predicate<Short> restrictor, String restrictFailText) {
        return this.requireShortDef(defValue, restrictor, restrictFailText, 10);
    }

    public short requireShortDef(short defValue) {
        return this.requireShortDef(defValue, null, null, 10);
    }

    public BigInteger requireBigIntegerDef(BigInteger defValue,
                                           Predicate<BigInteger> restrictor, String restrictFailText)
    {
        return this.requireBigIntegerDef(defValue, restrictor, restrictFailText, 10);
    }

    public BigInteger requireBigIntegerDef(BigInteger defValue) {
        return this.requireBigIntegerDef(defValue, null, null, 10);
    }

    public int requireIntTerm(String terminationText,
                              Predicate<Integer> restrictor, String restrictFailText)
    {
        return this.requireIntTerm(terminationText, restrictor, restrictFailText, 10);
    }

    public long requireLongTerm(String terminationText,
                                Predicate<Long> restrictor, String restrictFailText)
    {
        return this.requireLongTerm(terminationText, restrictor, restrictFailText, 10);
    }

    public byte requireByteTerm(String terminationText,
                                Predicate<Byte> restrictor, String restrictFailText)
    {
        return this.requireByteTerm(terminationText, restrictor, restrictFailText, 10);
    }

    public short requireShortTerm(String terminationText,
                                  Predicate<Short> restrictor, String restrictFailText)
    {
        return this.requireShortTerm(terminationText, restrictor, restrictFailText, 10);
    }

    public BigInteger requireBigIntegerTerm(String terminationText,
                                            Predicate<BigInteger> restrictor, String restrictFailText)
    {
        return this.requireBigIntegerTerm(terminationText, restrictor, restrictFailText, 10);
    }

    public int requireIntTerm(String terminationText) {
        return this.requireIntTerm(terminationText, null, null, 10);
    }

    public long requireLongTerm(String terminationText) {
        return this.requireLongTerm(terminationText, null, null, 10);
    }

    public byte requireByteTerm(String terminationText) {
        return this.requireByteTerm(terminationText, null, null, 10);
    }

    public short requireShortTerm(String terminationText) {
        return this.requireShortTerm(terminationText, null, null, 10);
    }

    public BigInteger requireBigIntegerTerm(String terminationText) {
        return this.requireBigIntegerTerm(terminationText, null, null, 10);
    }

    public String requireString(String retryText) {
        return this.requireString(retryText, null, null);
    }

    public String requireStringDef(String defValue) {
        return this.requireStringDef(defValue, null, null);
    }

    public String requireStringTerm(String terminationText) {
        return this.requireStringTerm(terminationText, null, null);
    }

    public BigDecimal requireBigDecimal(String retryText) {
        return this.requireBigDecimal(retryText, null, null);
    }

    public BigDecimal requireBigDecimalDef(BigDecimal defValue) {
        return this.requireBigDecimalDef(defValue, null, null);
    }

    public BigDecimal requireBigDecimalTerm(String terminationText) {
        return this.requireBigDecimalTerm(terminationText, null, null);
    }

    public double requireDouble(String retryText) {
        return this.requireDouble(retryText, null, null);
    }

    public double requireDoubleDef(double defValue) {
        return this.requireDoubleDef(defValue, null, null);
    }

    public double requireDoubleTerm(String terminationText) {
        return this.requireDoubleTerm(terminationText, null, null);
    }

    public float requireFloat(String retryText) {
        return this.requireFloat(retryText, null, null);
    }

    public float requireFloatDef(float defValue) {
        return this.requireFloatDef(defValue, null, null);
    }

    public float requireFloatTerm(String terminationText) {
        return this.requireFloatTerm(terminationText, null, null);
    }


    /*///////////////////////////////////////////////////////////////////////////*/
    /*//////////////////////  GETTERS & SETTERS & HELPERS  //////////////////////*/
    /*///////////////////////////////////////////////////////////////////////////*/


    public String getGreetingText() { return greetingText; }
    public Runnable getExitFunction() { return exitFunction; }
    public Runnable getTerminateFunction() { return terminateFunction; }

    public void setGreetingText(String greetingText) { this.greetingText = greetingText; }
    public void setExitFunction(Runnable exitFunction) { this.exitFunction = exitFunction; }
    public void setTerminateFunction(Runnable terminateFunction) {
        this.terminateFunction = terminateFunction;
    }

    private void printIfValid(String text) {
        if (text == null) return;
        if (text.isEmpty()) return;
        this.print(text);
    }

    private void printlnIfValid(String text) {
        if (text == null) return;
        if (text.isEmpty()) return;
        this.println(text);
    }


}

