/*
    IDKAgent - A Java Agent for advanced measurement of object memory footprints.
    Copyright (C) 2024  Alexis Mugisha

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
