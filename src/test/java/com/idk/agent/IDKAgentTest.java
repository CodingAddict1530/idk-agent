package com.idk.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IDKAgentTest {

    @Test
    void test() {

        IDKAgent.setInst(ByteBuddyAgent.install());
        Map<String, List<Integer>> map = new HashMap<>();
        //System.out.println(IDKAgent.getInstrumentation().getObjectSize(Integer.valueOf(1)));
        //map.put("Hey", List.of(1, 2, 3));
        //map.put("Bye", List.of(21, 21, 23));
        //map.put("LLLLL", List.of(1444, 2, 3));
        //map.put("1", List.of(1, 2, 444444443));
        StringBuilder sb = null;
        for (int i = 0; i < 1000; i++) {
            sb = new StringBuilder();
            for (int j = 0; j < 10000; j++) {
                sb.append("DDDDDD").append(i).append(j);
            }
            map.put(sb.toString(), List.of(i));
        }
        sb = new StringBuilder();
        sb.append("h".repeat(1000000));
        System.out.println(IDKAgent.getObjectSize(sb.toString(), 5));

        //System.out.println(ClassLayout.parseInstance(map.get(sb.toString()).get(0)).toPrintable());
        //System.out.println(ClassLayout.parseInstance(map.get(sb.toString())).toPrintable());
        //System.out.println(ClassLayout.parseInstance(sb.toString()).toPrintable());
        //System.out.println(ClassLayout.parseInstance(map).toPrintable());

    }

}