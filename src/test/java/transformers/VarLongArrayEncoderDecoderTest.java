package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VarLongArrayEncoderDecoderTest {

    private VarLongArrayEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new VarLongArrayEncoderDecoder();
    }

    @Test
    public void encode() {
        byte[] bytes = underTest.encode(new long[]{127L, 126L, 32767L, 32764L});
    }

    @Test
    public void decode() {
        long[] input = new long[]{127L, 126L, 32767L, 32764L};
        byte[] bytes = underTest.encode(input);
        long[] decoded = underTest.decode(bytes);
        assertArrayEquals(decoded, input);
    }

    @Test
    public void largeInput() {
        int limit = 7000000;
        Random r = new Random();
        long[] input = new long[limit];
        for (int i = 0; i < limit; i++) {
            input[i] = Math.abs(r.nextLong());
        }
        long now = System.currentTimeMillis();
        byte[] bytes = underTest.encode(input);
        long later = System.currentTimeMillis();
        System.out.println("Size of array in bytes for 7M integers:" + bytes.length + " in " + (later-now) + " " +
                                   "ms");

        now = System.currentTimeMillis();
        long[] decoded = underTest.decode(bytes);
        later = System.currentTimeMillis();
        System.out.println("Size of array in bytes for 7M integers:" + bytes.length + " in " + (later-now) + " " +
                                   "ms");
    }
}