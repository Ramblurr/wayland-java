/*
 * Copyright © 2024 Casey Link
 *
 * Licensed under the Apache License, Version 2.0 (the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.freedesktop.wayland.util;

import org.junit.jupiter.api.Assertions;

import java.util.EnumSet;

class EnumUtilTest {

    public enum TestEnum {
        A(1),
        B(2),
        C(4);

        public final int value;

        TestEnum(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    @org.junit.jupiter.api.Test
    void decode() {
        // Test case 1: Decode bitmask 0b001
        EnumSet<TestEnum> result1 = EnumUtil.decode(TestEnum.class, 0b001);
        Assertions.assertEquals(EnumSet.of(TestEnum.A), result1);

        // Test case 2: Decode bitmask 0b010
        EnumSet<TestEnum> result2 = EnumUtil.decode(TestEnum.class, 0b010);
        Assertions.assertEquals(EnumSet.of(TestEnum.B), result2);

        // Test case 3: Decode bitmask 0b101
        EnumSet<TestEnum> result3 = EnumUtil.decode(TestEnum.class, 0b101);
        Assertions.assertEquals(EnumSet.of(TestEnum.A, TestEnum.C), result3);

        // Test case 4: Decode bitmask 0b000
        EnumSet<TestEnum> result4 = EnumUtil.decode(TestEnum.class, 0b000);
        Assertions.assertEquals(EnumSet.noneOf(TestEnum.class), result4);

    }

    @org.junit.jupiter.api.Test
    void encode() {
        // Test case 1: Encode EnumSet with TestEnum.A
        EnumSet<TestEnum> set1 = EnumSet.of(TestEnum.A);
        int bitmask1 = EnumUtil.encode(set1);
        Assertions.assertEquals(0b001, bitmask1);

        // Test case 2: Encode EnumSet with TestEnum.B
        EnumSet<TestEnum> set2 = EnumSet.of(TestEnum.B);
        int bitmask2 = EnumUtil.encode(set2);
        Assertions.assertEquals(0b010, bitmask2);

        // Test case 3: Encode EnumSet with TestEnum.A and TestEnum.C
        EnumSet<TestEnum> set3 = EnumSet.of(TestEnum.A, TestEnum.C);
        int bitmask3 = EnumUtil.encode(set3);
        Assertions.assertEquals(0b101, bitmask3);

        // Test case 4: Encode empty EnumSet
        EnumSet<TestEnum> set4 = EnumSet.noneOf(TestEnum.class);
        int bitmask4 = EnumUtil.encode(set4);
        Assertions.assertEquals(0b000, bitmask4);
    }
}