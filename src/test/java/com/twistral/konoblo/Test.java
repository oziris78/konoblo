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


import java.util.Scanner;

import static com.twistral.konoblo.KonobloConsole.*;

public class Test {

    public static void add(KonobloConsole cns) {
        int x = cns.readInt("Enter number #1: ");
        int y = cns.readInt("Enter number #2: ");
        cns.printf("%d + %d = %d\n", x, y, x+y);
    }

    public static void sub(KonobloConsole cns) {
        int x = cns.readInt("Enter number #1: ");
        int y = cns.readInt("Enter number #2: ");
        cns.printf("%d - %d = %d\n", x, y, x-y);
    }

    public static void mul(KonobloConsole cns) {
        int x = cns.readInt("Enter number #1: ");
        int y = cns.readInt("Enter number #2: ");
        cns.printf("%d * %d = %d\n", x, y, x*y);
    }

    public static void mainMenu(KonobloConsole cns) {
        cns.println("Hello please choose an option:");
        cns.println("1. Addition");
        cns.println("2. Subtraction");
        cns.println("3. Multiplication");
        cns.println("4. Fibonacci");
        cns.print("Your choice: ");
    }

    public static void fiboMenu(KonobloConsole cns) {
        cns.printf("Do you want to see all steps (0 for no, 1 for yes): ");
    }

    public static void fiboLast(KonobloConsole cns) {
        int x = cns.readInt("Enter number: ", 2, Integer.MAX_VALUE);
        int a = 0, b = 1;
        for (int i = 2; i <= x; i++) {
            int next = a + b;
            a = b;
            b = next;
        }
        cns.printf("Fibonacci(%d) = %d\n", x, b);
    }

    public static void fiboAll(KonobloConsole cns) {
        int x = cns.readInt("Enter number: ", 2, Integer.MAX_VALUE);
        int a = 0, b = 1;
        cns.printf("Fibonacci(0) = 0\n");
        cns.printf("Fibonacci(1) = 1\n");
        for (int i = 2; i <= x; i++) {
            int next = a + b;
            a = b;
            b = next;
            cns.printf("Fibonacci(%d) = %d\n", i, b);
        }
    }

    public static void main(String[] args) {
        final KonobloConsole cns = new KonobloConsole();

        cns.setExitFunction(() -> {
            cns.println("Thanks for using this program!");
        });

        cns.define("#A", Test::mainMenu, cns.sepInt(1, 4, "#A1", "#A2", "#A3", "#A4"))
               .define("#A1", Test::add, cns.back(1))
               .define("#A2", Test::sub, cns.next("#A"))
               .define("#A3", Test::mul, cns.exit())
               .define("#A4", Test::fiboMenu, cns.sepInt(0, 1, "#A4.1", "#A4.2"))
                   .define("#A4.1", Test::fiboLast, cns.exit())
                   .define("#A4.2", Test::fiboAll, cns.exit());

        cns.run();
    }

    public static void versionWithoutKonoblo() {
        Scanner scanner = new Scanner(System.in);
        int x, y;
        boolean keepRunning = true;

        while (keepRunning) {
            System.out.println("Hello please choose an option:");
            System.out.println("1. Addition");
            System.out.println("2. Subtraction");
            System.out.println("3. Multiplication");
            System.out.println("4. Fibonacci");
            System.out.printf("Your choice: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    System.out.printf("Enter number #1: ");
                    x = scanner.nextInt();
                    System.out.printf("Enter number #2: ");
                    y = scanner.nextInt();
                    System.out.printf("%d + %d = %d\n", x, y, x+y);
                    break;
                case 2:
                    System.out.printf("Enter number #1: ");
                    x = scanner.nextInt();
                    System.out.printf("Enter number #2: ");
                    y = scanner.nextInt();
                    System.out.printf("%d - %d = %d\n", x, y, x-y);
                    break;
                case 3:
                    System.out.printf("Enter number #1: ");
                    x = scanner.nextInt();
                    System.out.printf("Enter number #2: ");
                    y = scanner.nextInt();
                    System.out.printf("%d * %d = %d\n", x, y, x*y);
                    keepRunning = false;
                    break;
                case 4:
                    do {
                        System.out.printf("Enter an index for fibonacci (x>=2): ");
                        x = scanner.nextInt();
                    } while (x < 2);

                    do {
                        System.out.printf("Do you want to see all steps (0 for no, 1 for yes): ");
                        y = scanner.nextInt();
                    } while(y != 0 && y != 1);

                    int a = 0, b = 1, next;
                    if (y == 0) {
                        // compute only fib(x)
                        for (int i = 2; i <= x; i++) {
                            next = a + b;
                            a = b;
                            b = next;
                        }
                        System.out.printf("Fibonacci(%d) = %d\n", x, b);
                    } else {
                        // print fib(0), fib(1), ..., fib(x)
                        System.out.printf("Fibonacci(0) = 0\n");
                        System.out.printf("Fibonacci(1) = 1\n");
                        for (int i = 2; i <= x; i++) {
                            next = a + b;
                            a = b;
                            b = next;
                            System.out.printf("Fibonacci(%d) = %d\n", i, b);
                        }
                    }

                    keepRunning = false;
                    break;
                default:
                    System.out.println("Invalid option, please try again.");
                    break;
            }
        }

        System.out.println("Thanks for using this program!");
        scanner.close();
    }

}
