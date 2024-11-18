package com.idk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class IDKAgent {

    private static Instrumentation inst;

    public static void premain(String[] args, Instrumentation inst) {

        IDKAgent.inst = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {

        IDKAgent.inst = inst;
    }

    public static Instrumentation getInstrumentation() {

        return inst;
    }

    public static long getObjectSize(Object o) {

        return inst.getObjectSize(o);
    }

    // Increment starts from 0
    private static long calculateObjectSize(Object o, Set<Object> visited, int depth, int increment) {

        if (o == null || visited.contains(o)) {
            return 0;
        }

        visited.add(o);
        long size = inst.getObjectSize(o);
        increment++;

        if (depth > 0 && increment != depth) {
            Set<Field> fieldsSuper = new HashSet<>();
            Class<?> clazz = o.getClass();
            Set<Field> fields = new HashSet<>(Arrays.asList(clazz.getDeclaredFields()));

            clazz = clazz.getSuperclass();
            while (clazz != null) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field f : declaredFields) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        fieldsSuper.add(f);
                    }
                }
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        if (!field.getType().isPrimitive()) {
                            int s = getPrimitiveSize(value);
                            if (s == 0) {
                                size += calculateObjectSize(value, visited, depth, increment);
                                size += checkContainer(field, value, visited, depth, increment);
                            } else {
                                size += s;
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            for (Field field : fieldsSuper) {
                field.setAccessible(true);
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        int s = getPrimitiveSize(value);
                        if (s == 0) {
                            size += calculateObjectSize(value, visited, depth, increment);
                            size += checkContainer(field, value, visited, depth, increment);
                        } else {
                            size += s;
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return size;

    }

    private static long checkContainer(Field field, Object value,
                                       Set<Object> visited, int depth, int increment) {

        long size = 0;

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    int s = getPrimitiveSize(item);
                    if (s == 0) {
                        size += calculateObjectSize(item, visited, depth, increment);
                    } else {
                        size += s;
                    }
                }
            }
            return size;
        }

        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    int s = getPrimitiveSize(entry.getKey());
                    if (s == 0) {
                        size += calculateObjectSize(entry.getKey(), visited, depth, increment);
                    } else {
                        size += s;
                    }
                }
                if (entry.getValue() != null) {
                    int s = getPrimitiveSize(entry.getValue());
                    if (s == 0) {
                        size += calculateObjectSize(entry.getValue(), visited, depth, increment);
                    } else {
                        size += s;
                    }
                }
            }
            return size;
        }

        if (value instanceof Enumeration<?> enumeration) {
            while (enumeration.hasMoreElements()) {
                Object item = enumeration.nextElement();
                if (item != null) {
                    int s = getPrimitiveSize(item);
                    if (s == 0) {
                        size += calculateObjectSize(item, visited, depth, increment);
                    } else {
                        size += s;
                    }
                }
            }
            return size;
        }

        if (field.getType().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                int s = getPrimitiveSize(Array.get(value, i));
                if (s == 0) {
                    size += calculateObjectSize(Array.get(value, i), visited, depth, increment);
                } else {
                    size += s;
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
