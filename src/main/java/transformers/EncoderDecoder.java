package transformers;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * Represents a contract for encoding and decoding a value to
 * be stored in the graph.
 * Encoders and decoders are generally defined in pairs to ensure
 * data integrity and consistency during the encoding and decoding process
 *
 * @param <T> The source data type before the encoding process
 * @param <U> The destination data type of the encoding process
 */
public interface EncoderDecoder<T, U> {

    U encode(T data);

    T decode(U encoded);
}
