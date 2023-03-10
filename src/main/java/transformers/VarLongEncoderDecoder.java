package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * A variable length integer encoder/decoder for unsigned or positive integer values
 * The implementation does not currently support the varint encoding of negative/signed numbers
 * The encoding mechanism used is a simple varint encoding
 */
public class VarLongEncoderDecoder implements EncoderDecoder<Long, byte[]> {

    @Override
    public byte[] encode(Long data) {
        if (data < 0) {
            throw new IllegalArgumentException("VarInt encoding only supports non-negative/unsigned integers");
        }
        byte[] buffer = new byte[9];
        int position = 0;
        while (true) {
            if ((data & 0b1111111111111111111111111111111111111111111111111111111110000000L) == 0) {
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
    public Long decode(byte[] encoded) {
        long result = 0;
        int shift = 0;
        for (byte b : encoded) {
            result |= (long) (b & 0b1111111) << shift;
            shift += 7;
            if ((b & 0b10000000) == 0) {
                return result;
            }
        }
        return result;
    }
}
