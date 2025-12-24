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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.twistral.konoblo.Colorizer.*;


public class ColorizerDemoTest {

    private static final HashMap<String, Supplier<String>> effectFuncs = new HashMap<>();
    {
        effectFuncs.put("EF_NONE", Colorizer::EF_NONE);
        effectFuncs.put("EF_CLEAR", Colorizer::EF_CLEAR);
        effectFuncs.put("EF_BOLD", Colorizer::EF_BOLD);
        effectFuncs.put("EF_DIM", Colorizer::EF_DIM);
        effectFuncs.put("EF_ITALIC", Colorizer::EF_ITALIC);
        effectFuncs.put("EF_UNDERLINE", Colorizer::EF_UNDERLINE);
        effectFuncs.put("EF_SLOW_BLINK", Colorizer::EF_SLOW_BLINK);
        effectFuncs.put("EF_RAPID_BLINK", Colorizer::EF_RAPID_BLINK);
        effectFuncs.put("EF_REVERSE", Colorizer::EF_REVERSE);
        effectFuncs.put("EF_HIDDEN", Colorizer::EF_HIDDEN);
        effectFuncs.put("EF_STRIKETHROUGH", Colorizer::EF_STRIKETHROUGH);
        effectFuncs.put("EF_FRAMED", Colorizer::EF_FRAMED);
        effectFuncs.put("EF_ENCIRCLED", Colorizer::EF_ENCIRCLED);
        effectFuncs.put("EF_OVERLINED", Colorizer::EF_OVERLINED);
    }

    private static final HashMap<String, Supplier<String>> foregroundFuncs = new HashMap<>();
    {
        foregroundFuncs.put("FG_BLACK", Colorizer::FG_BLACK);
        foregroundFuncs.put("FG_RED", Colorizer::FG_RED);
        foregroundFuncs.put("FG_GREEN", Colorizer::FG_GREEN);
        foregroundFuncs.put("FG_YELLOW", Colorizer::FG_YELLOW);
        foregroundFuncs.put("FG_BLUE", Colorizer::FG_BLUE);
        foregroundFuncs.put("FG_MAGENTA", Colorizer::FG_MAGENTA);
        foregroundFuncs.put("FG_CYAN", Colorizer::FG_CYAN);
        foregroundFuncs.put("FG_WHITE", Colorizer::FG_WHITE);
        foregroundFuncs.put("FG_BLACK_BRIGHT", Colorizer::FG_BLACK_BRIGHT);
        foregroundFuncs.put("FG_RED_BRIGHT", Colorizer::FG_RED_BRIGHT);
        foregroundFuncs.put("FG_GREEN_BRIGHT", Colorizer::FG_GREEN_BRIGHT);
        foregroundFuncs.put("FG_YELLOW_BRIGHT", Colorizer::FG_YELLOW_BRIGHT);
        foregroundFuncs.put("FG_BLUE_BRIGHT", Colorizer::FG_BLUE_BRIGHT);
        foregroundFuncs.put("FG_MAGENTA_BRIGHT", Colorizer::FG_MAGENTA_BRIGHT);
        foregroundFuncs.put("FG_CYAN_BRIGHT", Colorizer::FG_CYAN_BRIGHT);
        foregroundFuncs.put("FG_WHITE_BRIGHT", Colorizer::FG_WHITE_BRIGHT);
    }

    private static final HashMap<String, Supplier<String>> backgroundFuncs = new HashMap<>();
    {
        backgroundFuncs.put("BG_BLACK", Colorizer::BG_BLACK);
        backgroundFuncs.put("BG_RED", Colorizer::BG_RED);
        backgroundFuncs.put("BG_GREEN", Colorizer::BG_GREEN);
        backgroundFuncs.put("BG_YELLOW", Colorizer::BG_YELLOW);
        backgroundFuncs.put("BG_BLUE", Colorizer::BG_BLUE);
        backgroundFuncs.put("BG_MAGENTA", Colorizer::BG_MAGENTA);
        backgroundFuncs.put("BG_CYAN", Colorizer::BG_CYAN);
        backgroundFuncs.put("BG_WHITE", Colorizer::BG_WHITE);
        backgroundFuncs.put("BG_BLACK_BRIGHT", Colorizer::BG_BLACK_BRIGHT);
        backgroundFuncs.put("BG_RED_BRIGHT", Colorizer::BG_RED_BRIGHT);
        backgroundFuncs.put("BG_GREEN_BRIGHT", Colorizer::BG_GREEN_BRIGHT);
        backgroundFuncs.put("BG_YELLOW_BRIGHT", Colorizer::BG_YELLOW_BRIGHT);
        backgroundFuncs.put("BG_BLUE_BRIGHT", Colorizer::BG_BLUE_BRIGHT);
        backgroundFuncs.put("BG_MAGENTA_BRIGHT", Colorizer::BG_MAGENTA_BRIGHT);
        backgroundFuncs.put("BG_CYAN_BRIGHT", Colorizer::BG_CYAN_BRIGHT);
        backgroundFuncs.put("BG_WHITE_BRIGHT", Colorizer::BG_WHITE_BRIGHT);
    }


    @Test
    @DisplayName("effectTest")
    void effectTest() {
        System.out.println("Different effects:");
        for (Map.Entry<String, Supplier<String>> ef : effectFuncs.entrySet()) {
            String effect = ef.getValue().get();
            System.out.print(ef.getKey() + " => ");
            System.out.println(colorize("Hello World!", effect));
        }
        System.out.println("Effect test is finished.");
    }


    @Test
    @DisplayName("foregroundTest")
    void foregroundTest() {
        System.out.println("Different foreground colors:");
        for (Map.Entry<String, Supplier<String>> entry : foregroundFuncs.entrySet()) {
            String fg = entry.getValue().get();
            System.out.print(entry.getKey() + " => ");
            System.out.println(colorize("Hello World!", fg));
        }
        System.out.println("Foreground test is finished.");
    }


    @Test
    @DisplayName("backgroundTest")
    void backgroundTest() {
        System.out.println("Different background colors:");
        for (Map.Entry<String, Supplier<String>> entry : backgroundFuncs.entrySet()) {
            String bg = entry.getValue().get();
            System.out.print(entry.getKey() + " => ");
            System.out.println(colorize("Hello World!", bg));
        }
        System.out.println("Background test is finished.");
    }


    @Test
    @DisplayName("trueColorTest")
    void trueColorTest() {
        final int COUNT = 10;

        for (int unused = 0; unused < COUNT; unused++) {
            int fi = (int) (Math.random()*256);
            int fj = (int) (Math.random()*256);
            int fk = (int) (Math.random()*256);

            String foreground = FG_CUSTOM(fi, fj, fk);
            System.out.printf(
                "FG=(%d,%d,%d) => %s\n",
                fi, fj, fk,
                colorize("Hello world!", foreground)
            );
        }

        for (int unused = 0; unused < COUNT; unused++) {
            int bi = (int) (Math.random()*256);
            int bj = (int) (Math.random()*256);
            int bk = (int) (Math.random()*256);
            String background = BG_CUSTOM(bi, bj, bk);
            System.out.printf(
                    "BG=(%d,%d,%d) => %s\n",
                    bi, bj, bk,
                    colorize("Hello world!", background)
            );
        }

        for (int unused = 0; unused < COUNT; unused++) {
            int fi = (int) (Math.random()*256);
            int fj = (int) (Math.random()*256);
            int fk = (int) (Math.random()*256);
            int bi = (int) (Math.random()*256);
            int bj = (int) (Math.random()*256);
            int bk = (int) (Math.random()*256);
            String foreground = FG_CUSTOM(fi, fj, fk);
            String background = BG_CUSTOM(bi, bj, bk);
            System.out.printf(
                    "FG=(%d,%d,%d) & BG=(%d,%d,%d) => %s\n",
                    fi, fj, fk, bi, bj, bk,
                    colorize("Hello world!", background, foreground)
            );
        }

    }

    @Test
    @DisplayName("indexedColorTest")
    void indexedColorTest() {
        final int COUNT = 10;

        for (int unused = 0; unused < COUNT; unused++) {
            int fx = (int) (Math.random()*256);
            String fg = FG_INDEXED(fx);
            System.out.printf(
                "FINDEX=%d => %s\n",
                fx, colorize("Hello world!", fg)
            );
        }

        for (int unused = 0; unused < COUNT; unused++) {
            int bx = (int) (Math.random()*256);
            String bg = BG_INDEXED(bx);
            System.out.printf(
                "BINDEX=%d => %s\n",
                bx, colorize("Hello world!", bg)
            );
        }

        for (int unused = 0; unused < COUNT; unused++) {
            int fx = (int) (Math.random()*256);
            String fg = FG_INDEXED(fx);
            int bx = (int) (Math.random()*256);
            String bg = BG_INDEXED(bx);
            System.out.printf(
                "FINDEX=%d & BINDEX=%d => %s\n",
                fx, bx, colorize("Hello world!", bg, fg)
            );
        }



    }

}
