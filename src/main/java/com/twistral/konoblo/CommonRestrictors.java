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


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;


public class CommonRestrictors {


    /*///////////////// STRING RESTRICTORS /////////////////*/

    public static Predicate<String> mustStartWith(String text) {
        Objects.requireNonNull(text, "text");
        return (x) -> x.startsWith(text);
    }

    public static Predicate<String> mustNotInclude(String... forbiddenStrings) {
        Objects.requireNonNull(forbiddenStrings, "forbiddenStrings");
        return (x) -> {
            for (String forbiddenText : forbiddenStrings) {
                if (x.contains(forbiddenText)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static Predicate<String> mustEndWith(String text) {
        Objects.requireNonNull(text, "text");
        return (x) -> x.endsWith(text);
    }

    public static Predicate<String> mustBeOneOf(String... allowedStrings) {
        Objects.requireNonNull(allowedStrings, "allowedStrings");
        return (x) -> {
            for (String allowedString : allowedStrings) {
                if (allowedString.equals(x)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<String> mustBeOneOfIgnoreCase(String... allowedStrings) {
        Objects.requireNonNull(allowedStrings, "allowedStrings");
        return (x) -> {
            for (String allowedString : allowedStrings) {
                if (allowedString.equalsIgnoreCase(x)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<String> minLength(int minInclusiveLength) {
        return x -> x.length() >= minInclusiveLength;
    }

    public static Predicate<String> maxLength(int maxInclusiveLength) {
        return x -> x.length() <= maxInclusiveLength;
    }

    /*///////////////// NUMERICAL RESTRICTORS /////////////////*/

    public static Predicate<Double> inRange(double minInclusive, double maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<Long> inRange(long minInclusive, long maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<Byte> inRange(byte minInclusive, byte maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<Short> inRange(short minInclusive, short maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<Float> inRange(float minInclusive, float maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<Integer> inRange(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) throw new KonobloException("Min > max");
        return (x) -> minInclusive <= x && x <= maxInclusive;
    }

    public static Predicate<BigInteger> inRange(BigInteger minInclusive, BigInteger maxInclusive) {
        Objects.requireNonNull(minInclusive, "minInclusive");
        Objects.requireNonNull(maxInclusive, "maxInclusive");

        if (minInclusive.compareTo(maxInclusive) > 0)
            throw new KonobloException("Min > max");

        return x -> x.compareTo(minInclusive) >= 0 && x.compareTo(maxInclusive) <= 0;
    }

    public static Predicate<BigDecimal> inRange(BigDecimal minInclusive, BigDecimal maxInclusive) {
        Objects.requireNonNull(minInclusive, "minInclusive");
        Objects.requireNonNull(maxInclusive, "maxInclusive");

        if (minInclusive.compareTo(maxInclusive) > 0)
            throw new KonobloException("Min > max");

        return x -> x.compareTo(minInclusive) >= 0 && x.compareTo(maxInclusive) <= 0;
    }

    public static Predicate<BigDecimal> min(BigDecimal minInclusive) {
        Objects.requireNonNull(minInclusive, "minInclusive");
        return x -> x.compareTo(minInclusive) >= 0;
    }

    public static Predicate<BigDecimal> max(BigDecimal maxInclusive) {
        Objects.requireNonNull(maxInclusive, "maxInclusive");
        return x -> x.compareTo(maxInclusive) <= 0;
    }

    public static Predicate<BigInteger> min(BigInteger minInclusive) {
        Objects.requireNonNull(minInclusive, "minInclusive");
        return x -> x.compareTo(minInclusive) >= 0;
    }

    public static Predicate<BigInteger> max(BigInteger maxInclusive) {
        Objects.requireNonNull(maxInclusive, "maxInclusive");
        return x -> x.compareTo(maxInclusive) <= 0;
    }

    public static Predicate<Double> min(double minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Long> min(long minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Byte> min(byte minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Short> min(short minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Float> min(float minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Integer> min(int minInclusive) {
        return x -> x >= minInclusive;
    }

    public static Predicate<Double> max(double maxInclusive) {
        return x -> x <= maxInclusive;
    }

    public static Predicate<Long> max(long maxInclusive) {
        return x -> x <= maxInclusive;
    }

    public static Predicate<Byte> max(byte maxInclusive) {
        return x -> x <= maxInclusive;
    }

    public static Predicate<Short> max(short maxInclusive) {
        return x -> x <= maxInclusive;
    }

    public static Predicate<Float> max(float maxInclusive) {
        return x -> x <= maxInclusive;
    }

    public static Predicate<Integer> max(int maxInclusive) {
        return x -> x <= maxInclusive;
    }


}
