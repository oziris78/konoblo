


# Konoblo

<b>Konoblo is a lightweight state driven framework for building CLI apps in Java.</b>

Konoblo simplifies the creation of interactive command line programs by combining:
- A state defining and directing mechanism
- Built-in input validation
- ANSI color support

Instead of writing scattered loops, switch statements and manual validation logic, you simply 
define states and describe how they transition/direct to one and other. Konoblo handles the 
entire control flow for you!


# Simple Example

```java
public class BasicCalculator {
    // The functions below are omitted for simplicity
    public static void mainMenu(KonobloConsole cns) {}
    public static void add(KonobloConsole cns) {}
    public static void sub(KonobloConsole cns) {}
    public static void mul(KonobloConsole cns) {}
    public static void fiboMenu(KonobloConsole cns) {}
    public static void fiboLast(KonobloConsole cns) {}
    public static void fiboAll(KonobloConsole cns) {}
    
    public static void main(String[] args) {
        KonobloConsole cns = new KonobloConsole();
        cns.setGreetingText("Welcome to my program!");

        // This function will be run when console terminates gracefully
        cns.setExitFunction(() -> {
            cns.println("Thanks for using this program!");
        });

        cns.define("#A", BasicCalculator::mainMenu);
        cns.define("#A1", BasicCalculator::add);
        cns.define("#A2", BasicCalculator::sub);
        cns.define("#A3", BasicCalculator::mul);
        cns.define("#A4", BasicCalculator::fiboMenu);
        cns.define("#A4a", BasicCalculator::fiboLast);
        cns.define("#A4b", BasicCalculator::fiboAll);

        cns.direct("#A", () -> {
            int choice = cns.requireInt(
                "Please enter an int in range [1,4]:",
                inRange(1,4), "Your input is not in the range [1,4]."
            );
            return String.format("#A%d", choice); // #A1, #A2, #A3 or #A4
        });
        cns.directBack("#A1", 1); // go back the state stack once, back to "#A"
        cns.directBack("#A2", 1); // also back to "#A"
        cns.direct("#A4", () -> {
            boolean allSteps = cns.requireBoolean(
                "Enter no/yes only: ", 
                "yes", "no", false
            );
            return allSteps ? "#A4b" : "#A4a";
        });

        cns.run("#A");
    }
}
```


# Defining & Directing States

Konoblo is built around a state machine model. You define states using 
the `define(String, Consumer<KonobloConsole>)` method and define the routes between
those states using the following directing methods:
- `direct(...)`: dynamic routing
- `directStrSelect(...)`: routing based on string input
- `directIntSelect(...)`: routing based on int input
- `directBack(...)`: return to a previous state



# Input

Konoblo separates input into two categories: reading and requiring.

Reading methods do simple input reading and they perform parsing but do not enforce validation:

```java
String name = cns.readString();
int age = cns.readInt();
```

Requiring methods on the other hand, do advanced and validated input reading. They can return
a default value, retry reading or simply terminate the console when something goes wrong:

```java
int a = cns.requireInt(
    "Please try again: ", x -> x < 20, "Input cant be greater or equal to 20."
);
int b = cns.requireIntDef(
    10, x -> x < 20, "Input cant be greater or equal to 20."
);
int c = cns.requireIntTerm(
    "Terminating the program!", x -> x < 20, "Input cant be greater or equal to 20."
);
```



# Output

Output streams are configurable, allowing redirection or integration into other 
systems. The console manages stream ownership and lifecycle safely.

Typical usage:

```java
cns.print(new Integer(10));
cns.print("hello!");
cns.println("hello!");
cns.printf("My name is %s %s and I am 40 years old.", "John", "Doe");
```

You can also use error(...), errorln(...) and errorf(...) functions to print errors, if
you have a separate error printing stream.



# ANSI Colors

Konoblo also includes the `Colorizer`, which is a utility class for generating ANSI colored text output.
Look <a href="https://github.com/oziris78/konoblo/blob/main/src/test/java/com/twistral/konoblo/ColorizerDemoTest.java">here</a> for examples.



# Downloading

Add these to your build.gradle file:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.oziris78:konoblo:v1.0.0'
}
```



# Licensing

Konoblo is licensed under the terms of the Apache-2.0 license.


