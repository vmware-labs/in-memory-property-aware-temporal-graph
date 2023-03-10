package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntZigZagEncoderDecoderTest {

    private IntZigZagEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new IntZigZagEncoderDecoder();
    }

    @Test
    public void encodeAndDecode() {
        {
            int value = 3;
            byte[] encoded = underTest.encode(value);
            assertEquals(value, underTest.decode(encoded));
        }

        {
            int value = -3;
            byte[] encoded = underTest.encode(value);
            assertEquals(value, underTest.decode(encoded));
        }

        {
            int value = -345623588;
            byte[] encoded = underTest.encode(value);
            assertEquals(value, underTest.decode(encoded));
        }
        {
            int value = (-1 * 10_000_000);
            byte[] encoded = underTest.encode(value);
            assertEquals(value, underTest.decode(encoded));
        }
    }
}