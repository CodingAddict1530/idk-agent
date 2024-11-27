package com.idk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.InaccessibleObjectException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A java agent that will deeply analyze the memory used by an object.
 */
public class IDKAgent {

    /**
     * Stores the fields of objects that have already been analyzed, ensuring the agent doesn't have to re-analyze it.
     */
    private static final Map<Class<?>, Set<Field>> cache = new ConcurrentHashMap<>();

    /**
     * A pattern for finding which package in a module to open to the agent.
     */
    private static final Pattern patternOpens = Pattern.compile("\"opens (.+)\"");

    /**
     * A pattern for finding which package in a module to export to the agent.
     */
    private static final Pattern patternExports = Pattern.compile("\"exports (.+)\"");

    /**
     * Used to get the shallow size of an object.
     */
    private static Instrumentation inst;

    /**
     * Hidden and unusable constructor to avoid creation of Objects of this class.
     */
    private IDKAgent() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Entry point for the Agent.
     * Executes before the main method in the application and sets up the Instrumentation.
     *
     * @param args Arguments passed to the agent.
     * @param inst The instrumentation for calculating shallow sizes.
     */
    public static void premain(String[] args, Instrumentation inst) {

        IDKAgent.inst = inst;
    }

    /**
     * Calculates the deep size of the object.
     * It will follow all objects that the object holds a reference of in its fields.
     * It will open all encapsulated modules to itself.
     *
     * @param o The Object.
     * @return The deep size of the object.
     */
    public static long getObjectSize(Object o) {

        return calculateObjectSize(o, new ReferenceSet(), Integer.MAX_VALUE, 0, true);
    }

    /**
     * Calculates the deep size of the object.
     * It will follow all objects that the object holds a reference of in its fields to a certain level
     * specified by {@code depth}.
     * It will open all encapsulated modules to itself.
     *
     * @param o The Object.
     * @param depth The levels in the hierarchy it should traverse to.
     * @return The deep size of the object.
     */
    public static long getObjectSize(Object o, int depth) {

        return calculateObjectSize(o, new ReferenceSet(), depth, 0, true);
    }

    /**
     * Calculates the deep size of the object.
     * It will follow all objects that the object holds a reference of in its fields.
     * Note that if {@code openModules} is false, it will not attempt to open encapsulated modules
     * and this could lead to very inaccurate sizes. (LIKE WAY LESS THAN IT IS).
     *
     * @param o The Object.
     * @param openModules Whether to open encapsulated modules or not.
     * @return The deep size of the object.
     */
    public static long getObjectSize(Object o, boolean openModules) {

        return calculateObjectSize(o, new ReferenceSet(), Integer.MAX_VALUE, 0, openModules);
    }

    /**
     * Calculates the deep size of the object.
     * It will follow all objects that the object holds a reference of in its fields to a certain level
     * specified by {@code depth}.
     * Note that if {@code openModules} is false, it will not attempt to open encapsulated modules
     * and this could lead to very inaccurate sizes. (LIKE WAY LESS THAN IT IS).
     *
     * @param o The Object.
     * @param depth The levels in the hierarchy it should traverse to.
     * @param openModules Whether to open encapsulated modules or not.
     * @return The deep size of the object.
     */
    public static long getObjectSize(Object o, int depth, boolean openModules) {

        return calculateObjectSize(o, new ReferenceSet(), depth, 0, openModules);
    }

    /**
     * Finds the deep size of an object.
     *
     * @param o The Object.
     * @param visited A set to hold all visited objects.
     * @param depth The levels in the hierarchy it should traverse to.
     * @param increment To keep count of which level the method is at.
     * @param openModules Whether to open encapsulated modules or not.
     * @return The deep size of the object.
     */
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

    /**
     * Checks whether the object is an array containing data of primitive types.
     *
     * @param o The Object.
     * @return Whether it is an array containing data of primitive types or not.
     */
    private static boolean checkIfPrimitiveArray(Object o) {

        if (o.getClass().isArray()) {
            return o.getClass().getComponentType().isPrimitive();
        }
        return false;

    }

    /**
     * Attempts to open a module that is encapsulated.
     *
     * @param field The field inside an encapsulated module.
     * @param e The Exception thrown when trying to access the field.
     */
    private static void openModule(Field field, Exception e) {

        if (!inst.isModifiableModule(field.getClass().getModule())) {
            return;
        }
        Matcher matcher = patternOpens.matcher(e.getMessage());
        if (matcher.find()) {

            inst.redefineModule(
                    field.getClass().getModule(),
                    Set.of(),
                    Map.of(),
                    Map.of(matcher.group(1), Set.of(IDKAgent.class.getModule())),
                    Set.of(),
                    Map.of()
            );
        } else {
            matcher = patternExports.matcher(e.getMessage());

            if (matcher.find()) {

                inst.redefineModule(
                        field.getClass().getModule(),
                        Set.of(),
                        Map.of(matcher.group(1), Set.of(IDKAgent.class.getModule())),
                        Map.of(),
                        Set.of(),
                        Map.of()
                );
            } else {
                System.err.println("No match :(");
            }
        }

    }

    /**
     * Checks if an object is an array and if so it returns it's deep size.
     *
     * @param value The object to be checked.
     * @param visited The set containing all already analyzed objects.
     * @param depth The levels in the hierarchy it should traverse to.
     * @param increment To keep count of which level the method is at.
     * @param openModules Whether to open encapsulated modules or not.
     * @return The deep size of the array or 0 if it is not an array.
     */
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

    /**
     * Gets the size of primitive values.
     * Since instrumentation will cause wrapping of the primitives, we use the know values.
     *
     * @param o The Wrapped primitive value.
     * @return The size of the value.
     */
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
