package core.utils;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

public class CommonUtils {
    public static long pack2IntsInLong(final int int1, final int int2) {
        return (((long) int1) << 32) | (int2 & 0xffffffffL);
    }
}
