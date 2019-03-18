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
}