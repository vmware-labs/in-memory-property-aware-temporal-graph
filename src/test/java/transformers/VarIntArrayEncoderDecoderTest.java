package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VarIntArrayEncoderDecoderTest {

    private VarIntArrayEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new VarIntArrayEncoderDecoder();
    }

    @Test
    public void encode() {
        byte[] bytes = underTest.encode(new int[]{127, 126, 32767, 32764});
    }

    @Test
    public void decode() {
        int[] input = new int[]{127, 126, 32767, 32764};
        byte[] bytes = underTest.encode(input);
        int[] decoded = underTest.decode(bytes);
        assertArrayEquals(decoded, input);
    }

    @Test
    public void largeInput() {
        int limit = 70_000_000;
        Random r = new Random();
        int[] input = new int[limit];
        for (int i = 0; i < limit; i++) {
            input[i] = Math.abs(r.nextInt());
        }
        byte[] bytes = underTest.encode(input);
        System.out.println("Size of array in bytes for 70M integers:" + bytes.length);
    }

    @Test
    public void largeInput1() throws InterruptedException {
        int limit = 70_000_000;
        Random r = new Random();
        int[] input = new int[limit];
        for (int i = 0; i < limit; i++) {
            input[i] = Math.abs(r.nextInt() % 100_000);
        }
        byte[] bytes = underTest.encode(input);
        System.out.println("TAKE heap dump");
        Thread.sleep(240000);
        //System.out.println("Size of array in bytes for 70M integers:" + bytes.length);
    }
}