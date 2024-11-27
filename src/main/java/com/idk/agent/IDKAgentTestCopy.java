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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A java agent that will deeply analyze the memory used by an object.
 * It can
 */
public class IDKAgentTestCopy {

    private static final Map<Class<?>, Set<Field>> cache = new ConcurrentHashMap<>();

    private static final Pattern patternOpens = Pattern.compile("\"opens (.+)\"");
    private static final Pattern patternExports = Pattern.compile("\"exports (.+)\"");

    private static Instrumentation inst;

    public static void setInst(Instrumentation inst) {

        IDKAgentTestCopy.inst = inst;
    }

    public static void premain(String args, Instrumentation inst) {

        IDKAgentTestCopy.inst = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {

        IDKAgentTestCopy.inst = inst;
    }

    public static Instrumentation getInstrumentation() {

        return inst;
    }

    public static long getObjectSize(Object o, int depth, boolean openModules) {

        return calculateObjectSize(o, new ReferenceSet(), depth, 0, openModules);
    }

    // Increment starts from 0
    private static long calculateObjectSize(Object o, ReferenceSet visited, int depth, int increment, boolean openModules) {

        if (o == null || visited.contains(o)) {
            return 0;
        }

        visited.add(o);
        long size = inst.getObjectSize(o);
        if (checkIfPrimitiveArray(o)) {
            return size;
        }
        int s = getPrimitiveSize(o);
        if (s != 0) {
            return size;
        }

        if (depth > 0 && increment++ != depth) {
            Class<?> clazz = o.getClass();
            Set<Field> fields = new HashSet<>();

            if (cache.containsKey(clazz)) {
                fields = cache.get(clazz);
            } else {
                Class<?> loopClass = clazz;
                while (loopClass != null) {
                    Field[] declaredFields = loopClass.getDeclaredFields();
                    for (Field f : declaredFields) {
                        try {
                            f.setAccessible(true);
                        } catch (InaccessibleObjectException e) {
                            if (openModules) {
                                openModule(f, e);
                                f.setAccessible(true);
                            } else {
                                continue;
                            }
                        }
                        if (!Modifier.isStatic(f.getModifiers())) {
                            fields.add(f);
                        }
                    }
                    loopClass = loopClass.getSuperclass();
                }
                cache.putIfAbsent(clazz, fields);
            }
            size += checkIsArray(o, visited, depth, increment, openModules);
            for (Field field : fields) {
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        if (!field.getType().isPrimitive()) {
                            size += calculateObjectSize(value, visited, depth, increment, openModules);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return size;

    }

    private static boolean checkIfPrimitiveArray(Object o) {

        if (o.getClass().isArray()) {
            return o.getClass().getComponentType().isPrimitive();
        }
        return false;

    }

    private static void openModule(Field field, Exception e) {

        Matcher matcher = patternOpens.matcher(e.getMessage());
        if (matcher.find()) {

            inst.redefineModule(
                    field.getClass().getModule(),
                    Set.of(),
                    Map.of(),
                    Map.of(matcher.group(1), Set.of(IDKAgentTestCopy.class.getModule())),
                    Set.of(),
                    Map.of()
            );
        } else {
            matcher = patternExports.matcher(e.getMessage());

            if (matcher.find()) {

                inst.redefineModule(
                        field.getClass().getModule(),
                        Set.of(),
                        Map.of(matcher.group(1), Set.of(IDKAgentTestCopy.class.getModule())),
                        Map.of(),
                        Set.of(),
                        Map.of()
                );
            } else {
                System.err.println("No match :(");
            }
        }

    }

    private static long checkIsArray(Object value, ReferenceSet visited,
               int depth, int increment, boolean openModules) {

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            long size = 0;
            for (int i = 0; i < length; i++) {
                size += calculateObjectSize(Array.get(value, i), visited, depth, increment, openModules);
            }
            return size;
        }

        return 0;

    }

    private static int getPrimitiveSize(Object o) {

        if (o instanceof Integer) {
            return Integer.BYTES;
        } else if (o instanceof Long) {
            return Long.BYTES;
        } else if (o instanceof Double) {
            return Double.BYTES;
        } else if (o instanceof Float) {
            return Float.BYTES;
        } else if (o instanceof Short) {
            return Short.BYTES;
        } else if (o instanceof Byte) {
            return Byte.BYTES;
        } else if (o instanceof Character) {
            return Character.BYTES;
        } else if (o instanceof Boolean) {
            return 1;
        }

        return 0;

    }

}
