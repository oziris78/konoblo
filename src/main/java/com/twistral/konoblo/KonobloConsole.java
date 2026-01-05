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


    private void signalTermination() {
        throw new KonobloTerminateSignal();
    }


    /*///////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  DIRECTORS  ///////////////////////////*/
    /*///////////////////////////////////////////////////////////////////*/


    public Director exit() {
        return new Director(DirectorType.EXIT, () -> "");
    }

    public Director next(String nextID) {
        return new Director(DirectorType.NEXT, () -> nextID);
    }

    public Director back(int n) {
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

    public Director sepInt(int a, int b, String... options) {
        if (options.length != b - a + 1) {
            throw new KonobloException("You need to have exactly %d options.", b - a + 1);
        }

        return new Director(DirectorType.SEP_INT, () -> {
            int x = requireInt(a, b);
            return options[x-a];
        });
    }

    public Director sepStr(String[] inputs, String[] options) {
        if (inputs.length != options.length) {
            throw new KonobloException(
                "Both arrays must have the same number of items: %d != %d.",
                inputs.length, options.length
            );
        }

        return new Director(DirectorType.SEP_STR, () -> {
            final String selectedID = requireString(inputs);

            int selectedIndex = 0;
            while (selectedIndex < inputs.length) {
                if (inputs[selectedIndex].equals(selectedID)) {
                    break;
                }

                selectedIndex++;
            }

            return options[selectedIndex];
        });
    }


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


    private boolean requireBoolean(boolean useDefaultValue, boolean defaultValue,
                                   String exceptionText, boolean doTerminate)
    {
        while (true) {
            try {
                return readBoolean();
            }
            catch (Exception ignored) {
                if (doTerminate) {
                    this.signalTermination();
                }
                if (useDefaultValue) {
                    return defaultValue;
                }
                if (exceptionText != null) {
                    this.print(exceptionText);
                }
            }
        }
    }


    public boolean requireBooleanDefVal(boolean defaultValue) {
        return this.requireBoolean(true, defaultValue, null, false);
    }

    public boolean requireBooleanTry(String exceptionText) {
        return this.requireBoolean(false, false, exceptionText, false);
    }

    public boolean requireBooleanTerm(String exceptionText) {
        return this.requireBoolean(false, false, exceptionText, true);
    }



    //temp
    private int requireInt(int a, int b) {
        return 0;
    }
    private String requireString(String... allowedInputs) {
        return null;
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


}








