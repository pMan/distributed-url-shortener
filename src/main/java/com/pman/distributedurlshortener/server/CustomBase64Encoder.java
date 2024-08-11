package com.pman.distributedurlshortener.server;

public class CustomBase64Encoder {

    // shuffled array containing [0-9a-zA-Z-_]+
    private static final char[] CHARS = new char[] { '-', 'c', 'h', 'i', 'j', '5', 'V', 'M', 'n', 'H', 'I', '3', 'u',
            'U', 'G', 'b', 'F', 't', 'v', 'L', 'E', '2', 'm', '8', 'k', 'g', 'a', 'R', '_', 'r', 'P', 'w', 'y', '9',
            'W', 'S', 'z', 's', '6', 'T', 'q', '1', '4', 'K', '0', 'p', 'N', 'l', 'J', 'Z', 'B', '7', 'A', 'X', 'e',
            'd', 'Q', 'O', 'C', 'f', 'o', 'Y', 'x', 'D' };

    private static final int LEN = CHARS.length;

    /**
     * encode a long to a custom base-64 number system. A five char long hash can
     * have over 1 billion unique strings, six chars can have over 68 billion
     * 
     * @param num
     * @return
     */
    public static String longToBase64(long num) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(CHARS[(int) (num % LEN)]);
            num /= LEN;
        } while (num % LEN > 0);
        return sb.reverse().toString();
    }

}
