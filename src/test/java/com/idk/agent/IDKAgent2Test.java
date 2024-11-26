package com.idk.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IDKAgent2Test {

    @Test
    void test() {

        IDKAgent2.setInst(ByteBuddyAgent.install());
        Map<String, List<Integer>> map = new HashMap<>();
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
        System.out.println(IDKAgent2.getObjectSize(sb.toString(), 5));

    }

}