// Copyright 2025-2026 Oğuzhan Topaloğlu
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



/**
 * Utility class for applying ANSI SGR escape codes to text. Please note that
 * support for certain effects depends on terminal and font capabilities.
 */
public final class Colorizer {

    // Used resources:
    // https://stackoverflow.com/questions/4842424/list-of-ansi-color-escape-sequences/33206814
    // https://en.wikipedia.org/wiki/ANSI_escape_code

    private static final String LINE_SEP = System.lineSeparator();
    private static final String RESET = "\u001B[0m"; // for resetting terminal's format
    private static final String ESC = "\u001B[";


    private Colorizer() {} // No constructor


    /*/////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  METHODS  ///////////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/


    /**
     * Applies ANSI SGR attributes to the given text. <br><br>
     *
     * You can provide multiple attributes into this function such as effects,
     * foreground colors and background colors. All of these attributes will be
     * combined into a single ANSI escape sequence for you to print easily. <br><br>
     *
     * The terminal state will be automatically reset at the end of each returned
     * string. Also line separators are handled safely to prevent background color
     * spilling also known as "bleeding".
     *
     * @param text the text to be colorized with the given attributes
     * @param attributes ANSI SGR attribute codes (effects, foreground and background colors)
     * @return the ANSI-formatted string ready to be printed
     */
    public static String colorize(String text, String... attributes) {
        if (text == null) return "null";
        if (attributes == null) return text;
        if (attributes.length == 0) return text;

        final StringBuilder sb = new StringBuilder(256);

        sb.append(ESC); // Every Ansi escape code begins with this

        boolean firstAttr = true;
        for (String code : attributes) {
            if (!code.isEmpty()) {
                if (!firstAttr) {
                    sb.append(";"); // Every two attribute must be separated by ";"
                }
                sb.append(code);
                firstAttr = false;
            }
        }

        sb.append("m"); // Every Ansi escape code must end with this

        final String ansiCode = sb.toString();
        sb.setLength(0);
        sb.append(ansiCode);

        // Fix the background color spilling by resetting the terminal's format
        sb.append(text.replace(LINE_SEP, RESET + LINE_SEP + ansiCode));
        sb.append(RESET);
        return sb.toString();
    }


    /*/////////////////////////////////////////////////////////////////////////*/
    /*//////////////////////////////   EFFECTS   //////////////////////////////*/
    /*/////////////////////////////////////////////////////////////////////////*/


    /*///////////////////// SPECIAL EFFECTS /////////////////////*/

    /**
     * Placeholder effect that produces no ANSI code. Useful with conditional assignments.
     */
    public static String EF_NONE() { return ""; }

    /**
     * Resets all SGR attributes (equivalent to ESC[0m)
     */
    public static String EF_CLEAR() { return "0"; }

    /*//////////////// WIDELY SUPPORTED EFFECTS ////////////////*/

    public static String EF_BOLD() { return "1"; }
    public static String EF_UNDERLINE() { return "4"; }

    /**
     * Swaps FG and BG colors.
     */
    public static String EF_REVERSE() { return "7"; }

    /*//////// COMMON (TERMINAL/FONT DEPENDENT) EFFECTS ////////*/

    public static String EF_DIM() { return "2"; }
    public static String EF_ITALIC() { return "3"; }
    public static String EF_STRIKETHROUGH() { return "9"; }

    /*/////////// EXTREMELY RARELY SUPPORTED EFFECTS ///////////*/

    public static String EF_SLOW_BLINK() { return "5"; }
    public static String EF_RAPID_BLINK() { return "6"; }
    public static String EF_HIDDEN() { return "8"; }
    public static String EF_FRAMED() { return "51"; }
    public static String EF_ENCIRCLED() { return "52"; }
    public static String EF_OVERLINED() { return "53"; }


    /*/////////////////////////////////////////////////////////////////*/
    /*//////////////////////////  FG COLORS  //////////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/

    public static String FG_BLACK() { return "30"; }
    public static String FG_RED() { return "31"; }
    public static String FG_GREEN() { return "32"; }
    public static String FG_YELLOW() { return "33"; }
    public static String FG_BLUE() { return "34"; }
    public static String FG_MAGENTA() { return "35"; }
    public static String FG_CYAN() { return "36"; }
    public static String FG_WHITE() { return "37"; }

    public static String FG_BLACK_BRIGHT() { return "90"; }
    public static String FG_RED_BRIGHT() { return "91"; }
    public static String FG_GREEN_BRIGHT() { return "92"; }
    public static String FG_YELLOW_BRIGHT() { return "93"; }
    public static String FG_BLUE_BRIGHT() { return "94"; }
    public static String FG_MAGENTA_BRIGHT() { return "95"; }
    public static String FG_CYAN_BRIGHT() { return "96"; }
    public static String FG_WHITE_BRIGHT() { return "97"; }

    /**
     * Creates a 24-bit (true color) foreground color ANSI attribute.
     *
     * @param r red component in range [0, 255]
     * @param g green component in range [0, 255]
     * @param b blue component in range [0, 255]
     * @return ANSI SGR foreground color code
     */
    public static String FG_CUSTOM(int r, int g, int b) {
        return "38;2;" + clamped(r) + ";" + clamped(g) + ";" + clamped(b);
    }

    /**
     * Creates an indexed foreground color ANSI attribute.
     *
     * @param index color index in range [0, 255]
     * @return ANSI SGR foreground color code
     */
    public static String FG_INDEXED(int index) {
        return "38;5;" + clamped(index);
    }

    /*/////////////////////////////////////////////////////////////////*/
    /*//////////////////////////  BG COLORS  //////////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/

    public static String BG_BLACK() { return "40"; }
    public static String BG_RED() { return "41"; }
    public static String BG_GREEN() { return "42"; }
    public static String BG_YELLOW() { return "43"; }
    public static String BG_BLUE() { return "44"; }
    public static String BG_MAGENTA() { return "45"; }
    public static String BG_CYAN() { return "46"; }
    public static String BG_WHITE() { return "47"; }

    public static String BG_BLACK_BRIGHT() { return "100"; }
    public static String BG_RED_BRIGHT() { return "101"; }
    public static String BG_GREEN_BRIGHT() { return "102"; }
    public static String BG_YELLOW_BRIGHT() { return "103"; }
    public static String BG_BLUE_BRIGHT() { return "104"; }
    public static String BG_MAGENTA_BRIGHT() { return "105"; }
    public static String BG_CYAN_BRIGHT() { return "106"; }
    public static String BG_WHITE_BRIGHT() { return "107"; }

    /**
     * Creates a 24-bit (true color) background color ANSI attribute.
     *
     * @param r red component in range [0, 255]
     * @param g green component in range [0, 255]
     * @param b blue component in range [0, 255]
     * @return ANSI SGR background color code
     */
    public static String BG_CUSTOM(int r, int g, int b) {
        return "48;2;" + clamped(r) + ";" + clamped(g) + ";" + clamped(b);
    }

    /**
     * Creates an indexed background color ANSI attribute.
     *
     * @param index color index in range [0, 255]
     * @return ANSI SGR background color code
     */
    public static String BG_INDEXED(int index) {
        return "48;5;" + clamped(index);
    }

    /*/////////////////////////////////////////////////////////////////*/
    /*///////////////////////////  HELPERS  ///////////////////////////*/
    /*/////////////////////////////////////////////////////////////////*/

    private static int clamped(int x) {
        return Math.min(Math.max(x, 0), 255);
    }


}

