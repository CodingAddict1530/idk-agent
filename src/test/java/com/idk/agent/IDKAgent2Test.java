package com.idk.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IDKAgent2Test {

    @Test
    void test() {

        IDKAgent2.setInst(ByteBuddyAgent.install());
        Map<String, List<Integer>> map = new HashMap<>();
        //System.out.println(IDKAgent.getInstrumentation().getObjectSize(Integer.valueOf(1)));
        //map.put("Hey", List.of(1, 2, 3));
        //map.put("Bye", List.of(21, 21, 23));
        //map.put("LLLLL", List.of(1444, 2, 3));
        //map.put("1", List.of(1, 2, 444444443));
        StringBuilder sb = null;
        for (int i = 0; i < 1000000; i++) {
            sb = new StringBuilder();
            sb.append("DDDDDD").append(i);
            map.put(sb.toString(), List.of(i, 999));
        }
        //sb = new StringBuilder();
        ////sb.append("h".repeat(1000000));
        long start = System.currentTimeMillis();
        System.out.println(IDKAgent2.getObjectSize(map, 5, true));
        long time = System.currentTimeMillis() - start;
        System.out.println(time);
        //System.out.println(IDKAgent2.getObjectSize(List.of(1,2,3,4), 5, true));
        //System.out.println(IDKAgent2.getObjectSize(new Integer[] {1,2,3,4}, 5, true));
        //System.out.println(IDKAgent2.getObjectSize(new IDKAgent2(), 5, true));

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("1", 1);
        map2.put("1", 1);
        //System.out.println(IDKAgent2.getObjectSize(map2, 5, true));
        //System.out.println(ClassLayout.parseInstance(map2).toPrintable());
        System.out.println(ClassLayout.parseInstance("DDDDDD12").toPrintable());
        System.out.println(ClassLayout.parseInstance(List.of(1)).toPrintable());
        //System.out.println(ClassLayout.parseInstance(map.get(sb.toString()).get(0)).toPrintable());
        //System.out.println(ClassLayout.parseInstance(map.get(sb.toString())).toPrintable());
        //System.out.println(ClassLayout.parseInstance(sb.toString()).toPrintable());
        //System.out.println(ClassLayout.parseInstance(map).toPrintable());

    }

}