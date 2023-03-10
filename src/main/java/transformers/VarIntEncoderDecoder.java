package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * A variable length integer encoder/decoder for unsigned or positive integer values
 * The implementation does not currently support the varint encoding of negative/signed numbers
 * The encoding mechanism used is a simple varint encoding.
 * <br>
 * Reference: https://golb.hplar.ch/2019/06/variable-length-int-java.html
 */
public class VarIntEncoderDecoder implements EncoderDecoder<Integer, byte[]> {

    @Override
    public byte[] encode(Integer data) {
        if (data < 0) {
            throw new IllegalArgumentException("VarInt encoding only supports non-negative/unsigned integers");
        }
        byte[] buffer = new byte[5];
        int position = 0;
        while (true) {
            if ((data & 0b11111111111111111111111110000000) == 0) {
                buffer[position++] = (byte)(data.intValue());
                break;
            }
            buffer[position++] = (byte)((data & 0b1111111) | 0b10000000);
            data >>>= 7;
        }
        byte[] encoded = new byte[position];
        System.arraycopy(buffer, 0, encoded, 0, position);
        return encoded;
    }

    @Override
    public Integer decode(byte[] encoded) {
        int result = 0;
        int shift = 0;
        for (byte b : encoded) {
            result |= (b & 0b1111111) << shift;
            shift += 7;
            if ((b & 0b10000000) == 0) {
                return result;
            }
        }
        return result;
    }
}
