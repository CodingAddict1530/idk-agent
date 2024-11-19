package com.idk.agent;

import java.util.HashSet;

public class ReferenceSet<T> {

    private final HashSet<Integer> references = new HashSet<>();

    public boolean add(T item) {

        return references.add(System.identityHashCode(item));
    }

    public boolean contains(T item) {

        return references.contains(System.identityHashCode(item));
    }

}
