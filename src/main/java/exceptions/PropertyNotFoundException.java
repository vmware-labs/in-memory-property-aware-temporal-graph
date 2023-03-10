package exceptions;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

public class PropertyNotFoundException extends Exception {
    public PropertyNotFoundException(final String message) {
        super(message);
    }
}
