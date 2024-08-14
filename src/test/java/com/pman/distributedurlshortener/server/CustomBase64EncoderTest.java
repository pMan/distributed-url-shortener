package com.pman.distributedurlshortener.server;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;

/**
 * @author pman
 *
 */
public class CustomBase64EncoderTest {

    @Test
    public void testBasic() {
        try {
            Field f = CustomBase64Encoder.class.getDeclaredField("CHARS");
            f.setAccessible(true);
            char[] chars = (char[]) f.get(new CustomBase64Encoder());
            for (int i = 0; i < chars.length; i++)
                assertEquals("" + chars[i], CustomBase64Encoder.longToBase64(i));
        } catch (Exception e) {
            e.printStackTrace();
        }

        long test = 64L;
        assertEquals("c-", CustomBase64Encoder.longToBase64(test));
    }

}
