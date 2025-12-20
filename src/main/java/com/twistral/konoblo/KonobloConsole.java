// Copyright 2025 Oğuzhan Topaloğlu
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
import java.util.*;
import java.util.function.*;
import com.twistral.konoblo.Director.DirectorType;


public class KonobloConsole {

    private static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    // State Related Objects
    private final HashMap<String, State> states;
    private final Stack<String> stateStack;
    private String entryStateID;

    // Data (Object Instance) Storage
    private final HashMap<String, Object> storage;

    // IO Objects
    private final PrintStream outStream, errStream;
    private final boolean ownsScanner, ownsStreams;
    private final Scanner scanner;

    // Misc Objects
    private String greetingText;
    private Runnable exitFunction;


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
        this.states = new HashMap<>(64);
        this.storage = new HashMap<>(64);
        this.stateStack = new Stack<>();
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


    /*/////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  METHODS  ///////////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/


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
        this.println(this.greetingText);

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

        // Run the special and final exit function
        this.exitFunction.run();

        // Clean-up / close scanner and streams if needed
        if (ownsStreams) {
            boolean usesSameStream = outStream.equals(errStream);
            this.outStream.close();
            if (!usesSameStream) {
                this.errStream.close();
            }
        }

        if (ownsScanner) {
            this.scanner.close();
        }
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
            int x = readInt(a, b);
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
            final String selectedID = readString(inputs);

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


    /*///////////////////////////////////////////////////////////////////*/
    /*/////////////////////  READING INPUT METHODS  /////////////////////*/
    /*///////////////////////////////////////////////////////////////////*/


    /**
     * Reads an integer in range [min, max]. This range includes both parameters.
     * @param min accepted minimum integer input
     * @param max accepted maximum integer input
     * @return an integer input in range [min, max]
     */
    public int readInt(int min, int max) {
        int input = 0;
        String line = "";
        while (true) {
            try {
                line = scanner.nextLine();
                input = Integer.parseInt(line);

                if(min <= input && input <= max) {
                    return input;
                }

                this.errorf(
                    "Input must be in range [%d,%d], your input was: %d, try again: ",
                    min, max, input
                );
            }
            catch (Exception ignored) {
                this.errorf("Invalid input: '%s', try again: ", line);
            }
        }
    }


    /**
     * @return reads any integer input
     */
    public int readInt() {
        return this.readInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }


    public int readInt(Object text) {
        this.print(text);
        return this.readInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }


    public int readInt(Object text, int min, int max) {
        this.print(text);
        return this.readInt(min, max);
    }


    private String readString(String... allowedInputs) {
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
        for (String objectID : this.storage.keySet()) {
            removeObject(objectID);
        }
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

    public void setGreetingText(String greetingText) { this.greetingText = greetingText; }
    public String getGreetingText() { return greetingText; }

    public void setEntryStateID(String entryStateID) { this.entryStateID = entryStateID; }
    public String getEntryStateID() { return entryStateID; }

    public void setExitFunction(Runnable exitFunction) {
        this.exitFunction = (exitFunction != null) ? exitFunction : (() -> {});
    }
    public Runnable getExitFunction() { return exitFunction; }



}








