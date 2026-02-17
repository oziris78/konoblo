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

import java.awt.*;

import static com.twistral.konoblo.CommonRestrictors.inRange;
import static com.twistral.konoblo.CommonRestrictors.mustBeOneOf;

public class MenuTest {

    // MAINMENU
    //   FMENU => F1, F2, ...
    //   GMENU => G1, G2, ...
    //   HMENU => H1, H2, ...

    static void f1(KonobloConsole cns) { cns.println("yoo this is f1 printing!"); }
    static void f2(KonobloConsole cns) { cns.println("yoo this is f2 printing!"); }
    static void f3(KonobloConsole cns) { cns.println("yoo this is f3 printing!"); }
    static void f4(KonobloConsole cns) { cns.println("yoo this is f4 printing!"); }
    static void f5(KonobloConsole cns) { cns.println("yoo this is f5 printing!"); }
    static void f6(KonobloConsole cns) { cns.println("yoo this is f6 printing!"); }

    static void g1(KonobloConsole cns) { cns.println("yoo this is g1 printing!"); }
    static void g2(KonobloConsole cns) { cns.println("yoo this is g2 printing!"); }
    static void g3(KonobloConsole cns) { cns.println("yoo this is g3 printing!"); }
    static void g4(KonobloConsole cns) { cns.println("yoo this is g4 printing!"); }
    static void g5(KonobloConsole cns) { cns.println("yoo this is g5 printing!"); }
    static void g6(KonobloConsole cns) { cns.println("yoo this is g6 printing!"); }

    static void h1(KonobloConsole cns) { cns.println("yoo this is h1 printing!"); }
    static void h2(KonobloConsole cns) { cns.println("yoo this is h2 printing!"); }
    static void h3(KonobloConsole cns) { cns.println("yoo this is h3 printing!"); }
    static void h4(KonobloConsole cns) { cns.println("yoo this is h4 printing!"); }
    static void h5(KonobloConsole cns) { cns.println("yoo this is h5 printing!"); }
    static void h6(KonobloConsole cns) { cns.println("yoo this is h6 printing!"); }

    static void mainMenu(KonobloConsole cns) {
        cns.println("Available menus are: ");
        cns.println("- F Menu");
        cns.println("- G Menu");
        cns.println("- H Menu");
        cns.print("Please choose a menu by name: ");
    }

    static void fMenu(KonobloConsole cns) {
        cns.print("Which f menu option do you want? There are f1-6: ");
    }

    static void gMenu(KonobloConsole cns) {
        cns.print("Which g menu option do you want? There are g1-6: ");
    }

    static void hMenu(KonobloConsole cns) {
        cns.print("Which h menu option do you want? There are h1-6: ");
    }

    public static void main(String[] args) {
        KonobloConsole cns = new KonobloConsole();
        cns.setGreetingText(null);

        cns.define("mainMenu", MenuTest::mainMenu);
        cns.define("fMenu", MenuTest::fMenu);
        cns.define("gMenu", MenuTest::gMenu);
        cns.define("hMenu", MenuTest::hMenu);

        cns.define("#f1", MenuTest::f1); cns.define("#f2", MenuTest::f2);
        cns.define("#f3", MenuTest::f3); cns.define("#f4", MenuTest::f4);
        cns.define("#f5", MenuTest::f5); cns.define("#f6", MenuTest::f6);
        cns.define("#g1", MenuTest::g1); cns.define("#g2", MenuTest::g2);
        cns.define("#g3", MenuTest::g3); cns.define("#g4", MenuTest::g4);
        cns.define("#g5", MenuTest::g5); cns.define("#g6", MenuTest::g6);
        cns.define("#h1", MenuTest::h1); cns.define("#h2", MenuTest::h2);
        cns.define("#h3", MenuTest::h3); cns.define("#h4", MenuTest::h4);
        cns.define("#h5", MenuTest::h5); cns.define("#h6", MenuTest::h6);

        // Clean string matching menu, recommended!
        cns.directStrSelect(
            "mainMenu",
            "Choose a menu by name: ", "Invalid option.",
            cns.option("F Menu", "fMenu"),
            cns.option("G Menu", "gMenu"),
            cns.option("H Menu", "hMenu")
        );

        // Very compact but hard to read tbh
        cns.direct("fMenu", () -> {
            int choice = cns.requireInt("Enter a valid integer bro: ", inRange(1, 6), null);
            return choice == 1 ? "#f1" : choice == 2 ? "#f2" : choice == 3 ?
                    "#f3" : choice == 4 ? "#f4" : choice == 5 ? "#f5" : "#f6";
        });

        // Cleaner and recommended version
        cns.directIntSelect(
                "gMenu",
                "Choose a function with an integer 1-6: ", "Invalid option.",
                cns.option(1, "#g1"),
                cns.option(2, "#g2"),
                cns.option(3, "#g3"),
                cns.option(4, "#g4"),
                cns.option(5, "#g5"),
                cns.option(6, "#g6")
        );

        // This is pretty much what directIntSelect does behind the scenes, kinda...
        cns.direct("hMenu", () -> {
            int choice = cns.requireInt("Enter a valid integer bro: ", inRange(1, 6), null);
            if (choice == 1)        return "#h1";
            else if (choice == 2)   return "#h2";
            else if (choice == 3)   return "#h3";
            else if (choice == 4)   return "#h4";
            else if (choice == 5)   return "#h5";
            else /*(choice == 6)*/  return "#h6";
        });

        cns.run("mainMenu");
    }

}
