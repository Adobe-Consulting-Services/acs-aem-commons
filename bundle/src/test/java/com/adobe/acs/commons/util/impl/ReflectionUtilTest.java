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
package com.adobe.acs.commons.util.impl;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.adobe.acs.commons.util.impl.ReflectionUtil.getClassOrGenericParam;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isArray;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isAssignableFrom;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isCollectionType;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isListType;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isSetType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReflectionUtilTest {

    private ValueMap valueMap;
    public Number numberField;
    public List<Integer> integerList;

    static class TestClazz{
        public List<String> stringList;
        public Set<Integer> integerSet;
        public Collection<Long> longCollection;
        public Float[] floatArray;
        public Number atomicInteger;
        public String string;
        public CharSequence charSequence;
    }

    @Before
    public void setUp() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("myIntegerField", 22);
        map.put("myIntegerArray", new Integer[]{22, 33});
        map.put("myDoubleArray", new Double[]{11.11, 22.22});

        valueMap = new ValueMapDecorator(map);
    }

    @Test
    public void convertValueMapValue() {
        Integer myIntegerField = ReflectionUtil.convertValueMapValue(valueMap, "myIntegerField", Integer.class);
        assertEquals(22, myIntegerField.intValue());

        Integer[] myIntegerArray = ReflectionUtil.convertValueMapValue(valueMap, "myIntegerArray", Integer[].class);
        assertEquals(2, myIntegerArray.length);
        assertEquals(33, myIntegerArray[1].intValue());

        Double[] myDoubleArray = ReflectionUtil.convertValueMapValue(valueMap, "myDoubleArray", Double[].class);
        assertEquals(2, myDoubleArray.length);
        assertEquals(22.22, myDoubleArray[1], 0);

    }

    @Test
    public void test_inheritanceValueMap() {
        InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(valueMap);
        Integer myInheritedIntegerField = ReflectionUtil.convertValueMapValue(inheritanceValueMap, "myIntegerField", Integer.class);
        assertEquals(22, myInheritedIntegerField.intValue());
    }

    @Test
    public void toArray() {

        List<Integer> integerList = Arrays.asList(1, 2);
        Integer[] integers = ReflectionUtil.toArray(integerList);
        assertEquals(2, integers.length);
        assertEquals(2, integers[1].intValue());

    }

    @Test
    public void test_isArray() {
        List<Integer> integerList = Arrays.asList(1, 2);
        Integer[] integers = ReflectionUtil.toArray(integerList);
        assertFalse(isArray(integerList.getClass()));
        assertTrue(isArray(integers.getClass()));
    }

    @Test
    public void testTestClazz(){

        for(Field field :TestClazz.class.getDeclaredFields()){

            Type type = field.getAnnotatedType().getType();
            switch(field.getName()){
                case "stringList":
                    assertTrue(isListType(type));
                    assertSame(String.class, getClassOrGenericParam(type));
                    assertTrue(isAssignableFrom(type, new ArrayList<String>().getClass()));
                    break;
                case "integerSet":
                    assertTrue(isSetType(type));
                    assertSame(Integer.class, getClassOrGenericParam(type));
                    assertTrue(isAssignableFrom(type, new HashSet<String>().getClass()));
                    break;
                case "longCollection":
                    assertTrue(isCollectionType(type));
                    assertSame(Long.class,getClassOrGenericParam(type));
                    assertTrue(isAssignableFrom(type, new ArrayList<Long>().getClass()));
                    break;
                case "floatArray":
                    assertTrue(isArray(type));
                    assertSame(Float.class, getClassOrGenericParam(type));
                    assertTrue(isAssignableFrom(type, Float[].class));
                    break;
                case "atomicInteger":
                    assertFalse(isListType(type));
                    assertFalse(isSetType(type));
                    assertFalse(isCollectionType(type));
                    assertFalse(isArray(type));
                    assertTrue(isAssignableFrom(type, AtomicInteger.class));
                    assertSame(Number.class, getClassOrGenericParam(type));
                    break;
                case "string":
                    assertTrue(isAssignableFrom(type, String.class));
                    assertFalse(isAssignableFrom(type, CharSequence.class));
                    break;
                case "charSequence":
                    assertTrue(isAssignableFrom(type, String.class));
                    assertTrue(isAssignableFrom(type, CharSequence.class));
                    assertFalse(isAssignableFrom(type, Number.class));
                    break;
                default:
                    break;
            }

        }

    }


    @Test
    public void test_isAssignableFrom() {

        Field numberField = FieldUtils.getDeclaredField(ReflectionUtilTest.class, "numberField");
        boolean isAssignableFrom = isAssignableFrom(numberField.getGenericType(), Integer.class);
        assertTrue(isAssignableFrom);
    }


}
