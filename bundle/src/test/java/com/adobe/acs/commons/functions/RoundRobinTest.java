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
package com.adobe.acs.commons.functions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class RoundRobinTest {
    private Iterator<String> iterator;
    private List<String> list;

    @Before
    public void setUp() throws Exception {
        list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        RoundRobin<String> roundRobin = new RoundRobin<>(list);
        iterator = roundRobin.iterator();
    }

    @Test
    public void iterator() {
        List<String> content = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String item = iterator.next();
            content.add(item);
        }
        Assert.assertEquals(list, content);
    }

    @Test(expected = IllegalArgumentException.class)
    public void remove() {
        iterator.remove();
    }

    @Test
    public void hasNext() {
        Assert.assertTrue(iterator.hasNext());
    }

    @Test
    public void rewindToZero() throws IllegalAccessException {
        Field[] fields = iterator.getClass().getDeclaredFields();
        Field indexField = null;
        for (Field field : fields) {
            if (field.getName().equals("index")) {
                indexField = field;
                break;
            }
        }

        Assert.assertNotNull(indexField);
        indexField.setAccessible(true);

        indexField.set(iterator, new AtomicInteger(0));
        Assert.assertEquals("a", iterator.next());
        Assert.assertEquals("b", iterator.next());
        Assert.assertEquals("c", iterator.next());
        Assert.assertEquals("a", iterator.next());
    }
    
    @Test
    public void overflowTest() throws IllegalAccessException {
        Field[] fields = iterator.getClass().getDeclaredFields();
        Field indexField = null;
        for (Field field : fields) {
            if (field.getName().equals("index")) {
                indexField = field;
                break;
            }
        }
        Assert.assertNotNull(indexField);
        indexField.setAccessible(true);
        
        // set the counter near to Integer.MAX_VALUE to test the overflow
        final int indexCounter = Integer.MAX_VALUE -1 ;
        Assert.assertEquals(indexCounter % list.size(), 0);
        AtomicInteger beforeOverflow = new AtomicInteger(indexCounter);
        indexField.set(iterator, beforeOverflow);
          
        Assert.assertEquals("a", iterator.next());
        Assert.assertEquals("b", iterator.next());
        Assert.assertEquals("c", iterator.next());
        Assert.assertEquals("a", iterator.next());
    }
}