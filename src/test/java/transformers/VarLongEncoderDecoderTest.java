package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VarLongEncoderDecoderTest {

    private VarLongEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new VarLongEncoderDecoder();
    }

    @Test
    public void encode() {
        byte[] encoded = underTest.encode(300L);
        assertEquals(2, encoded.length);
        assertEquals((byte)-84, encoded[0]);
        assertEquals((byte)2, encoded[1]);

        encoded = underTest.encode(Integer.MAX_VALUE + 1L);
        assertEquals(5, encoded.length);

        encoded = underTest.encode(45L);
        assertEquals(1, encoded.length);
    }

    @Test
    public void decode() {
        byte[] encoded = underTest.encode(300L);
        long decoded = underTest.decode(encoded);
        assertEquals(300, decoded);

        encoded = underTest.encode(Integer.MAX_VALUE + 1L);
        decoded = underTest.decode(encoded);
        assertEquals(Integer.MAX_VALUE + 1L, decoded);

        long time = System.currentTimeMillis();
        encoded = underTest.encode(time);
        decoded = underTest.decode(encoded);
        assertEquals(decoded, time);
    }

}