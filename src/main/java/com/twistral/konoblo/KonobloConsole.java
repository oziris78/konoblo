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
import java.util.Locale;


public class KonobloConsole {

    private static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    private final PrintStream outStream, errStream;
    private Runnable exitFunction;
    private String greetingText;


    //////////////////////////////////////////////////////////////////////////
    /////////////////////////////  CONSTRUCTORS  /////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    public KonobloConsole(PrintStream outStream, PrintStream errStream) {
        this.outStream = outStream;
        this.errStream = errStream;

        this.greetingText = DEF_GREETING_TEXT;
        this.exitFunction = null;
    }

    public KonobloConsole(PrintStream outAndErrStream) {
        this(outAndErrStream, outAndErrStream);
    }

    public KonobloConsole() {
        this(System.out, System.err);
    }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////  METHODS  /////////////////////////////
    /////////////////////////////////////////////////////////////////////


    public void run() {
        this.outStream.println(this.greetingText);

        // some stuff

        if(this.exitFunction != null) {
            this.exitFunction.run();
        }
    }

    /* PRINTING METHODS: BINDINGS FOR OUTSTREAM */
    public void printf(Locale l, String format, Object... args) { outStream.printf(l, format, args); }
    public void printf(String format, Object... args) { outStream.printf(format, args); }
    public void println(int x) { outStream.println(x); }
    public void println(char x) { outStream.println(x); }
    public void println(long x) { outStream.println(x); }
    public void println(float x) { outStream.println(x); }
    public void println(char... x) { outStream.println(x); }
    public void println(double x) { outStream.println(x); }
    public void println(Object x) { outStream.println(x); }
    public void println(String x) { outStream.println(x); }
    public void println(boolean x) { outStream.println(x); }
    public void println() { outStream.println(); }


    /* ERROR METHODS */
    public void errorf(Locale l, String format, Object... args) { errStream.printf(l, format, args); }
    public void errorf(String format, Object... args) { errStream.printf(format, args); }
    public void errorln(int x) { errStream.println(x); }
    public void errorln(char x) { errStream.println(x); }
    public void errorln(long x) { errStream.println(x); }
    public void errorln(float x) { errStream.println(x); }
    public void errorln(char... x) { errStream.println(x); }
    public void errorln(double x) { errStream.println(x); }
    public void errorln(Object x) { errStream.println(x); }
    public void errorln(String x) { errStream.println(x); }
    public void errorln(boolean x) { errStream.println(x); }
    public void errorln() { errStream.println(); }


    ///////////////////////////////////////////////////////////////////////////////
    /////////////////////////////  GETTERS & SETTERS  /////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public void setGreetingText(String greetingText) { this.greetingText = greetingText; }
    public String getGreetingText() { return greetingText; }

    public void setExitFunction(Runnable exitFunction) { this.exitFunction = exitFunction; }
    public Runnable getExitFunction() { return exitFunction; }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////  HELPERS  /////////////////////////////
    /////////////////////////////////////////////////////////////////////



}








