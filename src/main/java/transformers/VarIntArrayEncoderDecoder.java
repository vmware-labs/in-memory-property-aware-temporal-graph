package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VarIntArrayEncoderDecoder implements EncoderDecoder<int[], byte[]> {

    private final VarIntEncoderDecoder intEncoderDecoder;

    public VarIntArrayEncoderDecoder() {
        this(new VarIntEncoderDecoder());
    }

    @VisibleForTesting
    VarIntArrayEncoderDecoder(VarIntEncoderDecoder intEncoderDecoder) {
        this.intEncoderDecoder = intEncoderDecoder;
    }

    @Override
    public byte[] encode(int[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (int datum : data) {
                bos.write(intEncoderDecoder.encode(datum));
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int[] decode(byte[] encoded) {
        List<Integer> buffer = new ArrayList<>();
        int index = 0;
        int range = 0;
        for (byte datum : encoded) {
            if ((datum & 0b10000000) != 0) {
                range++;
                continue;
            }

            byte[] toDecode = Arrays.copyOfRange(encoded, index, range + 1);
            buffer.add(intEncoderDecoder.decode(toDecode));
            index = range + 1;
            range = index;
        }
        int[] decoded = new int[buffer.size()];
        for (int  i = 0; i < buffer.size(); i++) {
            decoded[i] = buffer.get(i);
        }
        return decoded;
    }
}
