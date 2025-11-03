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


public class KonobloConsole {

    private static final String DEF_GREETING_TEXT = "Welcome to Konoblo! You can " +
            "customize or disable this message with setGreetingText(String) method.";

    private final PrintStream outStream, errStream;
    private String greetingText;


    //////////////////////////////////////////////////////////////////////////
    /////////////////////////////  CONSTRUCTORS  /////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    public KonobloConsole(PrintStream outStream, PrintStream errStream) {
        this.outStream = outStream;
        this.errStream = errStream;

        this.greetingText = DEF_GREETING_TEXT;
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
    }


    ///////////////////////////////////////////////////////////////////////////////
    /////////////////////////////  GETTERS & SETTERS  /////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    public void setGreetingText(String greetingText) {
        this.greetingText = greetingText;
    }

    public String getGreetingText() {
        return greetingText;
    }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////  HELPERS  /////////////////////////////
    /////////////////////////////////////////////////////////////////////



}








