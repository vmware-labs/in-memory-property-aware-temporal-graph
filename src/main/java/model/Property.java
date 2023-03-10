package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

/**
 * An object that represents a property for a {@link GraphEntity}
 * Properties themselves are graph entities and are stored as first class
 * entities within the storage layer as opposed to attributes or tags on higher level
 * entities like {@link Vertex} and {@link Edge}<br/>
 * Implementations of this interface expose mechanisms to get or set the value of the property
 * at a specific timestamp.
 * <br/>
 * For both <code>get</code> and <code>set</code> methods, the implementations are responsible
 * for interpreting the timestamp correctly and retrieving/storing the property value against the timestamp.
 * For e.g. an implementation of this interface can define the semantics of the {@code get} method to return the
 * value of property at the largest time instant less than or equal to the specified time instance.
 * Alternatively, another implementation of {@code get} may choose to return the value of the property at
 * <b>exactly</b> the specified time instance.
 */
public interface Property extends GraphEntity {

    /**
     * Gets the name of the property
     * @return the name of the property
     */
    String getName();

    /**
     * Returns the value of the property at the specific timestamp.
     * @param timestamp the timestamp at which the value needs to be retrieved.
     * @return the value of the property at the timeinstant.
     */
    Object getValueAtTime(long timestamp);

    /**
     * Set the value of the property at a specified time.
     * @param timestamp the time instant at which to set the value
     * @param value the value to be set.
     */
    void setValueAtTime(long timestamp, Object value);

    /**
     * Purge the values in the property timeseries till timestamp.
     * @param timestamp the time till which values needs to be removed
     * @return true of the timeseries is purged else false
     */
    boolean purgeTimeSeriesUntilTime(final long timestamp);
}
