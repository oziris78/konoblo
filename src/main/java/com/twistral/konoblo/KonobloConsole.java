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
import com.twistral.konoblo.Director.DirectorType;
import static com.twistral.konoblo.CommonRestrictors.*;


public class KonobloConsole {

    private static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    private static final Runnable EMPTY_RUNNABLE = () -> {};

    // State Related Objects
    private String greetingText;
    private final HashMap<String, State> states;
    private final Stack<String> stateStack;
    private String entryStateID;
    private Runnable exitFunction; // Always run at the end of each program
    private Runnable terminateFunction; // Only run when the program is intentionally terminated

    // Data (Object Instance) Storage
    private final HashMap<String, Object> storage;

    // IO Objects
    private final PrintStream outStream, errStream;
    private final boolean ownsScanner, ownsStreams;
    private final Scanner scanner;


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
        this.states = new HashMap<>(64);
        this.stateStack = new Stack<>();
        this.terminateFunction = () -> {};
        this.exitFunction = () -> {};
        this.entryStateID = null;
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


    private static final class State {
        public final Consumer<KonobloConsole> function;
        public final Director director;
        public final String ID;

        State(String ID, Consumer<KonobloConsole> function, Director director) {
            this.function = function;
            this.director = director;
            this.ID = ID;
        }
    }


    public KonobloConsole define(String ID, Consumer<KonobloConsole> function, Director director) {
        if (ID == null)
            throw new KonobloException("State ID can't be null.");
        if (director == null)
            throw new KonobloException("State director can't be null.");
        if (this.states.containsKey(ID))
            throw new KonobloException("Duplicate state ID: %s.", ID);

        if (function == null) // allow null functions but correct them to do nothing
            function = cns -> {};

        this.states.put(ID, new State(ID, function, director));

        // First time defining a state
        if (entryStateID == null) {
            this.entryStateID = ID;
        }

        return this;
    }


    public void run() {
        if (this.greetingText != null && !this.greetingText.isEmpty()) {
            this.println(this.greetingText);
        }

        try {
            this.stateStack.push(entryStateID);

            while (true) {
                // Get the current state ID
                String currentStateID = this.stateStack.peek();
                if (!this.states.containsKey(currentStateID)) {
                    throw new KonobloException("No state with an ID of %s was found.", currentStateID);
                }

                // Get the current state using that ID and run its function
                State curState = this.states.get(currentStateID);
                curState.function.accept(this);

                // Terminate if you come across an exit director
                if (curState.director.type == DirectorType.EXIT) {
                    break;
                }

                // If not terminated, queue the next state ID to be looped over
                String nextStateID = curState.director.nextIDSupplier.get();
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


    // This Exception subclass is intentionally used to signal termination
    private static final class KonobloTerminateSignal extends RuntimeException {
        KonobloTerminateSignal() {
            // no message, no throwable chaining, no stacktrace => NO COST
            super(null, null, false, false);
        }
    }



    /*///////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  DIRECTORS  ///////////////////////////*/
    /*///////////////////////////////////////////////////////////////////*/


    public Director dirExit() {
        return new Director(DirectorType.EXIT, () -> "");
    }

    public Director dirNext(String nextID) {
        return new Director(DirectorType.NEXT, () -> nextID);
    }

    public Director dirBack(int n) {
        return new Director(DirectorType.BACK, () -> {
            final int m = stateStack.size();
            final int nextIndex = m - n - 1;

            if (nextIndex < 0) {
                throw new KonobloException(
                    "State stack cant go back %d times when it has %d elements.", n, m);
            }

            return stateStack.get(nextIndex);
        });
    }

    public Director dirSepInt(String restrictFailText, String retryText, int a, int b, String... options) {
        if (options.length != b - a + 1) {
            throw new KonobloException("You need to have exactly %d options.", b - a + 1);
        }

        return new Director(DirectorType.SEP_INT, () -> {
            int x = this.requireInt(retryText, inRange(a, b), restrictFailText);
            return options[x-a];
        });
    }

    public Director dirSepStr(String restrictFailText, String retryText,
                              String[] allowedInputs, String[] mappedIDs)
    {
        if (allowedInputs.length != mappedIDs.length) {
            throw new KonobloException(
                "Both arrays must have the same number of items: %d != %d.",
                allowedInputs.length, mappedIDs.length
            );
        }

        return new Director(DirectorType.SEP_STR, () -> {
            String selectedID = this.requireString(
                retryText, mustBeOneOf(allowedInputs), restrictFailText
            );

            int selectedIndex = 0;
            while (selectedIndex < allowedInputs.length) {
                if (allowedInputs[selectedIndex].equals(selectedID)) {
                    break;
                }

                selectedIndex++;
            }

            return mappedIDs[selectedIndex];
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

    public String readString() { return scanner.nextLine(); }
    public double readDouble() { return scanner.nextDouble(); }
    public float readFloat() { return scanner.nextFloat(); }
    public boolean readBoolean() { return scanner.nextBoolean(); }
    public BigDecimal readBigDecimal() { return scanner.nextBigDecimal(); }

    public byte readByte() { return scanner.nextByte(); }
    public int readInt() { return scanner.nextInt(); }
    public BigInteger readBigInteger() { return scanner.nextBigInteger(); }
    public long readLong() { return scanner.nextLong(); }
    public short readShort() { return scanner.nextShort(); }

    public byte readByte(int radix) { return scanner.nextByte(radix); }
    public int readInt(int radix) { return scanner.nextInt(radix); }
    public BigInteger readBigInteger(int radix) { return scanner.nextBigInteger(radix); }
    public long readLong(int radix) { return scanner.nextLong(radix); }
    public short readShort(int radix) { return scanner.nextShort(radix); }


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
    /*///////////////////////////  GETTERS & SETTERS  ///////////////////////////*/
    /*///////////////////////////////////////////////////////////////////////////*/


    public Runnable getTerminateFunction() { return terminateFunction; }
    public Runnable getExitFunction() { return exitFunction; }
    public String getGreetingText() { return greetingText; }
    public String getEntryStateID() { return entryStateID; }

    public void setGreetingText(String greetingText) { this.greetingText = greetingText; }
    public void setEntryStateID(String entryStateID) { this.entryStateID = entryStateID; }

    public void setTerminateFunction(Runnable terminateFunction) {
        this.terminateFunction = (terminateFunction != null) ? terminateFunction : EMPTY_RUNNABLE;
    }

    public void setExitFunction(Runnable exitFunction) {
        this.exitFunction = (exitFunction != null) ? exitFunction : EMPTY_RUNNABLE;
    }


    /*//////////////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  HELPER FUNCTIONS  ///////////////////////////*/
    /*//////////////////////////////////////////////////////////////////////////*/

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

