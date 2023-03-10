package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeltaEncoderDecoderTest {

    private DeltaEncoderDecoder underTest;

    @BeforeEach
    public void init() {
        underTest = new DeltaEncoderDecoder();
    }

    @Test
    void encode() {
        List<Number> numberList = new ArrayList<>();
        for (int i = 100; i > 0; i--) {
            numberList.add(i);
        }
        DeltaEncoderDecoder.Result<Number> encoded = underTest.encode(numberList);
        assertTrue(encoded.getBaseValue() instanceof  Integer);
        assertSame(encoded.getBaseValue(), (Integer) 100);
        long[] encodedData = encoded.getEncodedData();
        assertEquals(encodedData[0], (int) (Integer) 100);
        for (int i = 1; i < encodedData.length; i++) {
            assertEquals(-1, encodedData[i]);
        }

    }

    @Test
    void decode() {
        List<Number> numberList = new ArrayList<>();
        for (int i = 100; i > 0; i--) {
            numberList.add(i);
        }
        DeltaEncoderDecoder.Result<Number> encoded = underTest.encode(numberList);
        assertTrue(encoded.getBaseValue() instanceof  Integer);
        assertSame(encoded.getBaseValue(), (Integer) 100);
        long[] encodedData = encoded.getEncodedData();
        assertEquals(encodedData[0], (int) (Integer) 100);
        for (int i = 1; i < encodedData.length; i++) {
            assertEquals(-1, encodedData[i]);
        }
        List<Number> decoded = underTest.decode(encoded);
        assertEquals(numberList, decoded);

    }

    @Test
    void decodeLong() {
        List<Number> numberList = new ArrayList<>();
        for (long i = 100L; i > 0L; i--) {
            numberList.add(i);
        }
        DeltaEncoderDecoder.Result<Number> encoded = underTest.encode(numberList);
        assertTrue(encoded.getBaseValue() instanceof  Long);
        assertSame(encoded.getBaseValue(), (Long) 100L);
        long[] encodedData = encoded.getEncodedData();
        assertEquals(encodedData[0], (long) (Long) 100L);
        for (int i = 1; i < encodedData.length; i++) {
            assertEquals(-1, encodedData[i]);
        }
        List<Number> decoded = underTest.decode(encoded);
        assertEquals(numberList, decoded);

    }
}