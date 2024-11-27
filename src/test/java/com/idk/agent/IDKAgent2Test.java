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