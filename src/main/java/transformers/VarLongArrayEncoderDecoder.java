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

public class VarLongArrayEncoderDecoder implements EncoderDecoder<long[], byte[]> {

    private final VarLongEncoderDecoder longEncoderDecoder;

    public VarLongArrayEncoderDecoder() {
        this(new VarLongEncoderDecoder());
    }

    @VisibleForTesting
    VarLongArrayEncoderDecoder(VarLongEncoderDecoder longEncoderDecoder) {
        this.longEncoderDecoder = longEncoderDecoder;
    }

    @Override
    public byte[] encode(long[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (long datum : data) {
                bos.write(longEncoderDecoder.encode(datum));
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long[] decode(byte[] encoded) {
        List<Long> buffer = new ArrayList<>();
        int index = 0;
        int range = 0;
        for (byte datum : encoded) {
            if ((datum & 0b10000000) != 0) {
                range++;
                continue;
            }

            byte[] toDecode = Arrays.copyOfRange(encoded, index, range + 1);
            buffer.add(longEncoderDecoder.decode(toDecode));
            index = range + 1;
            range = index;
        }
        long[] decoded = new long[buffer.size()];
        for (int  i = 0; i < buffer.size(); i++) {
            decoded[i] = buffer.get(i);
        }
        return decoded;
    }
}
