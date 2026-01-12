// Copyright 2026 Oğuzhan Topaloğlu
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

public class ReadVsRequireTest {

    static void f1(KonobloConsole cns) {
        cns.println("testReadBoolean");
        cns.println(cns.readBoolean());
    }
    static void f2(KonobloConsole cns) {
        cns.println("testReqBooleanDefVal");
        cns.println(cns.requireBooleanDefVal(false));
    }
    static void f3(KonobloConsole cns) {
        cns.println("testReqBooleanTerm");
        cns.println(cns.requireBooleanTerm("Invalid input terminating now lol"));
    }
    static void f4(KonobloConsole cns) {
        cns.println("testReqBooleanTry");
        cns.println(cns.requireBooleanTry("Invalid input so try again bro"));
    }


    public static void main(String[] args) {
        KonobloConsole cns = new KonobloConsole();
        cns.setTerminateFunction(() -> cns.println("Terminated!!"));
        cns.setExitFunction(() -> cns.println("Exited!!"));

        cns.define("#0", ReadVsRequireTest::f1, cns.dirNext("#1"));
        cns.define("#1", ReadVsRequireTest::f2, cns.dirNext("#2"));
        cns.define("#2", ReadVsRequireTest::f3, cns.dirNext("#3"));
        cns.define("#3", ReadVsRequireTest::f4, cns.dirExit());

        cns.run();
    }

}
