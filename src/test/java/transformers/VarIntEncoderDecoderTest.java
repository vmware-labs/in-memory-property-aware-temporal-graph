package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarIntEncoderDecoderTest {

    private VarIntEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new VarIntEncoderDecoder();
    }

    @Test
    public void encode() {
        byte[] encoded = underTest.encode(300);
        assertEquals(2, encoded.length);
        assertEquals((byte)-84, encoded[0]);
        assertEquals((byte)2, encoded[1]);

        encoded = underTest.encode(Integer.MAX_VALUE - 1);
        assertEquals(5, encoded.length);

        encoded = underTest.encode(45);
        assertEquals(1, encoded.length);
    }

    @Test
    public void decode() {
        byte[] encoded = underTest.encode(300);
        int decoded = underTest.decode(encoded);
        assertEquals(300, decoded);

        encoded = underTest.encode(Integer.MAX_VALUE - 1);
        decoded = underTest.decode(encoded);
        assertEquals(Integer.MAX_VALUE - 1, decoded);
    }

}