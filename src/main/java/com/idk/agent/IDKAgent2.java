package com.idk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDKAgent2 {

    private static final Map<Class<?>, List<Set<Field>>> cache = new ConcurrentHashMap<>();

    private static final Pattern pattern = Pattern.compile("\"opens (.+)\"");

    private static Instrumentation inst;

    public static void setInst(Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static void premain(String[] args, Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {

        IDKAgent2.inst = inst;
    }

    public static Instrumentation getInstrumentation() {

        return inst;
    }

    public static long getObjectSize(Object o, int depth, boolean openModules) {

        return calculateObjectSize(o, new ReferenceSet<>(), depth, 0, openModules);
    }

    // Increment starts from 0
    private static long calculateObjectSize(Object o, ReferenceSet<Object> visited, int depth, int increment, boolean openModules) {

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
            Set<Field> fieldsSuper = new HashSet<>();
            Class<?> clazz = o.getClass();
            Set<Field> fields = new HashSet<>();
            boolean first = true;

            if (cache.containsKey(clazz)) {
                fields = cache.get(clazz).get(0);
                fieldsSuper = cache.get(clazz).get(1);
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
                            if (first) {
                                fields.add(f);
                            } else {
                                fieldsSuper.add(f);
                            }
                        }
                    }
                    first = false;
                    loopClass = loopClass.getSuperclass();
                }
                cache.putIfAbsent(clazz, List.of(fields, fieldsSuper));
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
            for (Field field : fieldsSuper) {
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        if (!field.getType().isPrimitive()) {
                            size += calculateObjectSize(value, visited, depth, increment, openModules);
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

    private static boolean checkIfPrimitiveArray(Object o) {

        if (o.getClass().isArray()) {
            return o.getClass().getComponentType().isPrimitive();
        }
        return false;

    }

    private static void openModule(Field field, Exception e) {

        Matcher matcher = pattern.matcher(e.getMessage());
        if (matcher.find()) {

            inst.redefineModule(
                    field.getClass().getModule(),
                    Set.of(),
                    Map.of(),
                    Map.of(matcher.group(1), Set.of(IDKAgent2.class.getModule())),
                    Set.of(),
                    Map.of()
            );
        } else {
            System.err.println("No match :(");
        }

    }

    private static long checkIsArray(Object value, ReferenceSet<Object> visited,
               int depth, int increment, boolean openModules) {

        long size = 0;

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            boolean isPrimitive = value.getClass().getComponentType().isPrimitive();
            for (int i = 0; i < length; i++) {
                if (isPrimitive) {
                    size += inst.getObjectSize(Array.get(value, i));
                } else {
                    size += calculateObjectSize(Array.get(value, i), visited, depth, increment, openModules);
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
