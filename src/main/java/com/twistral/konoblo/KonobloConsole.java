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
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class KonobloConsole {

    private static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    // State Related Objects
    private final HashMap<String, State> states;
    private final Stack<String> stateStack;
    private String entryStateID;

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
        this.stateStack = new Stack<>();
        this.exitFunction = null;
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


    public KonobloConsole define(String ID, Consumer<KonobloConsole> function, Supplier<String> director) {
        if (ID == null)
            throw new KonobloException("State ID can't be null.");
        if (director == null)
            throw new KonobloException("State director can't be null.");
        if (this.states.containsKey(ID))
            throw new KonobloException("Duplicate state ID: %s.", ID);

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
            String currentStateID = this.stateStack.peek();

            if (currentStateID == "EXIT") {
                break;
            }

            if (!this.states.containsKey(currentStateID)) {
                throw new KonobloException("No state with an ID of %s was found.", currentStateID);
            }

            State currentState = this.states.get(currentStateID);

            if (currentState.function != null) {
                currentState.function.accept(this);
            }

            String nextStateID = currentState.director.get();
            this.stateStack.push(nextStateID);
        }

        if (this.exitFunction != null)
            this.exitFunction.run();

        if (ownsScanner)
            this.scanner.close();

        if (ownsStreams) {
            boolean usesSameStream = outStream.equals(errStream);
            this.outStream.close();
            if (!usesSameStream) {
                this.errStream.close();
            }
        }
    }



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

                this.errorf("Input must be in range [%d,%d], your input was: %d\n", min, max, input);
            }
            catch (Exception ignored) {
                this.errorf("Invalid input: %s\n", line);
            }
        }
    }


    public int readInt() {
        return this.readInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
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

    public void setExitFunction(Runnable exitFunction) { this.exitFunction = exitFunction; }
    public Runnable getExitFunction() { return exitFunction; }

    public void setEntryStateID(String entryStateID) { this.entryStateID = entryStateID; }
    public String getEntryStateID() { return entryStateID; }



    /*//////////////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  HELPER FUNCTIONS  ///////////////////////////*/
    /*//////////////////////////////////////////////////////////////////////////*/



}








