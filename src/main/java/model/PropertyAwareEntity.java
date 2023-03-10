package model;

/*
 Copyright 2023 VMware, Inc.
 SPDX-License-Identifier: BSD-2-Clause
*/

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents an graph entity that can contain properties.
 * <br/>
 * Currently the types of {@link GraphEntity} instances that
 * can contain properties are restricted to {@link Vertex} and {@link Edge} instances
 * Objects implementing this interface expose mechanisms to retrieve the mapping between
 * a particular time instance and the complete set of properties associated with the entity
 * at that time instance.
 * The time series based representation for the set of properties allows clients of this
 * interface to retrieve the full history of property associations with the {@link GraphEntity} instance.
 */
public interface PropertyAwareEntity extends GraphEntity {

    /**
     * Gets the full history of the associated properties with the current
     * {@link GraphEntity} instance. The history is chronologically ordered
     * from oldest to the newest.
     * @return A {@link TreeMap} with  time instant as the key and a map
     * of properties indexed by property name existing at that time instant as a value.
     */
    Set<Property> getProperties();

    /**
     * Get the properties associated with the vertex at a specified timestamp.
     * @param timestamp the timestamp at which the properties need to be retrieved.
     * @return the collection of {@link Property} instances at the specified time stamp.
     */
    Collection<Property> getPropertiesAtTime(long timestamp);

    /**
     * Get the property identified by name associated with the vertex at a specified timestamp
     * @param name the name of the property  to be retrieved
     * @param timestamp the timestamp at which the property needs to be retrieved
     * @return the {@link Property} instance as available within the vertex at the specified time.
     */
    Property getPropertyAtTime(String name, long timestamp);

    /**
     * Add a collection of properties to the vertex
     * @param properties The collection of properties to be dded.
     */
    void addProperties(Collection<Property> properties);

    /**
     * Removes a property with the specified name from the vertex
     * @param propertyName
     */
    void removeProperty(String propertyName);

}
