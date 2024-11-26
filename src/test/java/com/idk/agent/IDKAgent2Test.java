package com.idk.agent;

import com.idk.agent.object_tests.Child;
import com.idk.agent.object_tests.Outer;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class IDKAgent2Test {

    static Instrumentation inst;

    @BeforeAll
    static void setUp() {
        inst = ByteBuddyAgent.install();
    }

    @Test
    void test1() {

        IDKAgent2.setInst(inst);
        Map<String, List<Integer>> map = new HashMap<>();
        StringBuilder sb;
        for (int i = 0; i < 1000000; i++) {
            sb = new StringBuilder();
            sb.append("DDDDDD").append(i);
            map.put(sb.toString(), List.of(i, 999));
        }
        long start = System.currentTimeMillis();
        assertTimeout(Duration.ofMillis(3200), () -> IDKAgent2.getObjectSize(map, 5, true));

    }

    @Test
    void test2() {

        IDKAgent2.setInst(inst);
        Map<String, Integer> map = new HashMap<>();
        map.put("1", 2);
        map.put("2", 4);
        map.put("3", 6);
        map.put("4", 8);
        map.put("5", 10);

        assertEquals(608, IDKAgent2.getObjectSize(map, 5, true));

    }

    @Test
    void test3() {

        IDKAgent2.setInst(inst);
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");

        assertEquals(264, IDKAgent2.getObjectSize(list, 5, true));

        CopyOnWriteArrayList<String> list2 = new CopyOnWriteArrayList<>();
        list2.add("5");
        list2.add("5");
        list2.add("5");
        list2.add("5");

        assertEquals(120, IDKAgent2.getObjectSize(list2, 5, true));

    }

    @Test
    void test4() {

        IDKAgent2.setInst(inst);
        HashSet<String> emptySet = new HashSet<>();
        assertEquals(64, IDKAgent2.getObjectSize(emptySet, 5, true));

        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        System.out.println(ClassLayout.parseInstance(listWithNull).toPrintable());
        assertEquals(80, IDKAgent2.getObjectSize(listWithNull, 5, true));

    }

    @Test
    void test5() {

        IDKAgent2.setInst(inst);
        int[] emptyIntArray = new int[0];
        assertEquals(16, IDKAgent2.getObjectSize(emptyIntArray, 5, true));

        String[] emptyStringArray = new String[0];
        assertEquals(16, IDKAgent2.getObjectSize(emptyStringArray, 5, true));

        int[] partiallyFilledArray = new int[10];
        partiallyFilledArray[0] = 1;
        partiallyFilledArray[1] = 2;
        partiallyFilledArray[2] = 3;
        assertEquals(56, IDKAgent2.getObjectSize(partiallyFilledArray, 5, true));

        String[] stringArray = new String[10];
        for (int i = 0; i < 10; i++) {
            stringArray[i] = "example";
        }
        assertEquals(104, IDKAgent2.getObjectSize(stringArray, 5, true));

        int[][] multiArray = new int[5][5];
        assertEquals(240, IDKAgent2.getObjectSize(multiArray, 5, true));

    }

    @Test
    void test6() {

        IDKAgent2.setInst(inst);
        Child child = new Child();
        assertEquals(24, IDKAgent2.getObjectSize(child, 5, true));

        Outer outer = new Outer();
        assertEquals(16, IDKAgent2.getObjectSize(outer, 5, true));

        Outer.Inner inner = outer.new Inner();
        assertEquals(16, IDKAgent2.getObjectSize(inner, 5, true));

    }

    @Test
    void test7() throws IOException {

        IDKAgent2.setInst(inst);
        File file = new File("hi.java");
        file.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        System.out.println(IDKAgent2.getObjectSize(writer, 50, true));
        writer.close();
        file.delete();

    }

}
