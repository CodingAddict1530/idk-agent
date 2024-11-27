package com.idk.agent;

import java.util.HashSet;

/**
 * A wrapper on {@code HashSet} that stored hash codes from objects.
 * This is to uniquely identify the objects and avoid the set from counting different
 * Objects with the same value as duplicates.
 */
public class ReferenceSet {

    /**
     * Stores the hash codes of objects.
     */
    private final HashSet<Integer> references = new HashSet<>();

    /**
     * Adds an object's hash code to the set.
     *
     * @param item The Object to add.
     * @return whether it was successful or not.
     */
    public boolean add(Object item) {

        return references.add(System.identityHashCode(item));
    }

    /**
     * Checks whether an object's hash code already exists in the set.
     *
     * @param item The Object to look for.
     * @return Whether it already exists in the set or not.
     */
    public boolean contains(Object item) {

        return references.contains(System.identityHashCode(item));
    }

}
