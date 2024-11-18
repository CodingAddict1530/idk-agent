package com.idk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    private static long calculateObjectSize(Object o, Set<Object> visited) {

        if (o == null || visited.contains(o)) {
            return 0;
        }

        visited.add(o);
        long size = inst.getObjectSize(o);
        Set<Field> fields = new HashSet<>();

        Class<?> clazz = o.getClass();
        boolean firstClass = true;
        while (clazz != null) {
            if (firstClass) {
                fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            } else {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field f : declaredFields) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        fields.add(f);
                    }
                }
            }
            clazz = clazz.getSuperclass();
            firstClass = false;
        }
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(o);
                if (value != null && !field.getType().isPrimitive()) {
                    size += calculateObjectSize(value, visited);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return size;

    }

}
