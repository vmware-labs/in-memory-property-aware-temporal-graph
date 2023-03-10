/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

public class Driver {
    public static void main(String[] args) {

    }

    private static long packTwoInts(int integer1, int integer2) {
        final long lsbMask = 0x0000_0000_ffff_ffff;

        return ((integer1 & lsbMask) << 32) | (integer2 & lsbMask);

    }

    private static int[] unpackTwoInts(long value) {
        final long lsbMask = 0x0000_0000_ffff_ffff;
        int[] values = new int[2];
        values[1] = (int)(value & lsbMask);
        values[0] = (int)(value >>> 32);
        return values;
    }
}
