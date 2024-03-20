/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.mcp.impl.processes.asset;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NamesFilterTest {

    @Test
    public void testIsNotValidNameReturnsFalseWhenFilterIsEmpty() {
        assertFalse(new NamesFilter(StringUtils.EMPTY).isNotValidName("test1"));
        assertFalse(new NamesFilter(StringUtils.EMPTY).isNotValidName("test2"));
        assertFalse(new NamesFilter(StringUtils.EMPTY).isNotValidName("test3"));
        assertFalse(new NamesFilter(null).isNotValidName("test3"));
    }

    @Test
    public void testIsNotValidNameReturnsTrueWhenOnlyIncludeWasProvided() {
        assertTrue(new NamesFilter("+test1,test2,test3").isNotValidName("smthElse1"));
        assertTrue(new NamesFilter("test1,test2,test3").isNotValidName("smthElse2"));
        assertTrue(new NamesFilter("+test1,+test2,+test3").isNotValidName("smthElse3"));
    }

    @Test
    public void testIsNotValidNameReturnsFalseWhenOnlyIncludesWasProvided() {
        assertFalse(new NamesFilter("+test1,test2,test3").isNotValidName("test1"));
        assertFalse(new NamesFilter("test1,test2,+test3").isNotValidName("test2"));
        assertFalse(new NamesFilter("+test1,+test2,+test3").isNotValidName("test3"));
    }

    @Test
    public void testIsNotValidNameReturnsTrueWhenOnlyExcludeWasProvided() {
        assertTrue(new NamesFilter("-test1,-test2,-test3").isNotValidName("test1"));
        assertTrue(new NamesFilter("-test1,-test2,-test3").isNotValidName("test2"));
        assertTrue(new NamesFilter("-test1,-test2,-test3").isNotValidName("test3"));
    }

    @Test
    public void testIsNotValidNameReturnsFalseWhenOnlyExcludeWasProvided() {
        assertFalse(new NamesFilter("-test1,-test2,-test3").isNotValidName("smthElse1"));
        assertFalse(new NamesFilter("-test1,-test2,-test3").isNotValidName("smthElse2"));
        assertFalse(new NamesFilter("-test1,-test2,-test3").isNotValidName("smthElse3"));
    }

    @Test
    public void testIsNotValidNameReturnsTrueWhenItemWasIncludedAndExcluded() {
        assertTrue(new NamesFilter("-test1,test1").isNotValidName("test1"));
    }

    @Test
    public void testIsNotValidNameWhenBothFiltersAreApplied() {
        assertTrue(new NamesFilter("-test1,+test2,test3,-test4").isNotValidName("test1"));
        assertTrue(new NamesFilter("-test1,test2,+test3,-test4").isNotValidName("test4"));
        assertTrue(new NamesFilter("-test1,+test2,+test3,-test4").isNotValidName("test5"));

        assertFalse(new NamesFilter("-test1,test2,test3,-test4").isNotValidName("test2"));
        assertFalse(new NamesFilter("-test1,test2,test3,-test4").isNotValidName("test3"));
    }
}
