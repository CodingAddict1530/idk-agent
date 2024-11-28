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
import java.util.*;

public class IDKAgent2 {

    private static Instrumentation inst;

    public static void setInst(Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static void premain(String args, Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static Instrumentation getInstrumentation() {

        return inst;
    }

    public static long getObjectSize(Object o, int depth) {

        return calculateObjectSize(o, Collections.newSetFromMap(new IdentityHashMap<>()), depth, 0);
    }

    // Increment starts from 0
    private static long calculateObjectSize(Object o, Set<Object> visited, int depth, int increment) {

        if (o == null || visited.contains(o)) {
            return 0;
        }

        visited.add(o);
        long size = inst.getObjectSize(o);
        int s = getPrimitiveSize(o);
        if (s != 0) {
            return size;
        }

        if (depth > 0 && increment++ != depth) {
            Set<Field> fieldsSuper = new HashSet<>();
            Class<?> clazz = o.getClass();
            Set<Field> fields = new HashSet<>();
            boolean first = true;

            while (clazz != null) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field f : declaredFields) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        if (first) {
                            fields.add(f);
                        } else {
                            fieldsSuper.add(f);
                        }
                    }
                }
                first = false;
                clazz = clazz.getSuperclass();
            }
            size += checkContainer(o, visited, depth, increment);
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    continue;
                }
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        if (!field.getType().isPrimitive()) {
                            size += calculateObjectSize(value, visited, depth, increment);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            for (Field field : fieldsSuper) {
                try {
                    field.setAccessible(true);
                } catch (InaccessibleObjectException e) {
                    continue;
                }
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        if (!field.getType().isPrimitive()) {
                            size += calculateObjectSize(value, visited, depth, increment);
                        } else {
                            size += inst.getObjectSize(value);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return size;

    }

    private static long checkContainer(Object value,
                                       Set<Object> visited, int depth, int increment) {

        long size = 0;

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    size += calculateObjectSize(item, visited, depth, increment);
                }
            }
            return size;
        }

        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    size += calculateObjectSize(entry.getKey(), visited, depth, increment);
                }
                if (entry.getValue() != null) {
                    size += calculateObjectSize(entry.getValue(), visited, depth, increment);
                }
            }
            return size;
        }

        if (value instanceof Enumeration<?> enumeration) {
            while (enumeration.hasMoreElements()) {
                Object item = enumeration.nextElement();
                if (item != null) {
                    size += calculateObjectSize(item, visited, depth, increment);
                }
            }
            return size;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            boolean isPrimitive = value.getClass().getComponentType().isPrimitive();
            for (int i = 0; i < length; i++) {
                if (isPrimitive) {
                    size += inst.getObjectSize(Array.get(value, i));
                } else {
                    size += calculateObjectSize(Array.get(value, i), visited, depth, increment);
                }
            }
            return size;
        }

        return size;

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
