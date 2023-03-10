package transformers;

import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * An encoder/decoder that decodes an arbitrary collection of positive and negative integers
 * using the ZigZag encoding/decoding scheme.
 * <br>
 * Reference : https://golb.hplar.ch/2019/06/variable-length-int-java.html
 */
public class IntZigZagEncoderDecoder implements EncoderDecoder<Integer, byte[]> {

    private final VarIntEncoderDecoder intEncoderDecoder;

    public IntZigZagEncoderDecoder() {
        this(new VarIntEncoderDecoder());
    }

    @VisibleForTesting
    IntZigZagEncoderDecoder(VarIntEncoderDecoder encoderDecoder) {
        this.intEncoderDecoder = encoderDecoder;
    }


    @Override
    public byte[] encode(Integer data) {
        int temp = (data << 1) ^ (data >> 31);
        return intEncoderDecoder.encode(temp);
    }

    @Override
    public Integer decode(byte[] encoded) {
        int temp = intEncoderDecoder.decode(encoded);
        return (temp >>> 1) ^ -(temp & 1);
    }
}
