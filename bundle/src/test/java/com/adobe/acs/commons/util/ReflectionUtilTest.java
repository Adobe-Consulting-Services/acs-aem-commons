/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.util;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReflectionUtilTest {

    private ValueMap valueMap;
    public Number numberField;
    public List<Integer> integerList;

    @Before
    public void setUp() {
        HashMap<String, Object> map = new HashMap();
        map.put("myIntegerField", 22);
        map.put("myIntegerArray", new Integer[]{22, 33});
        map.put("myDoubleArray", new Double[]{11.11, 22.22});

        valueMap = new ValueMapDecorator(map);
    }

    @Test
    public void convertValueMapValue() {
        Integer myIntegerField = (Integer) ReflectionUtil.convertValueMapValue(valueMap, "myIntegerField", Integer.class);
        assertEquals(22, myIntegerField.intValue());

        Integer[] myIntegerArray = (Integer[]) ReflectionUtil.convertValueMapValue(valueMap, "myIntegerArray", Integer[].class);
        assertEquals(2, myIntegerArray.length);
        assertEquals(33, myIntegerArray[1].intValue());

        Double[] myDoubleArray = (Double[]) ReflectionUtil.convertValueMapValue(valueMap, "myDoubleArray", Double[].class);
        assertEquals(2, myDoubleArray.length);
        assertEquals(22.22, myDoubleArray[1], 0);

    }

    @Test
    public void test_inheritanceValueMap() {
        InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(valueMap);
        Integer myInheritedIntegerField = (Integer) ReflectionUtil.convertValueMapValue(inheritanceValueMap, "myIntegerField", Integer.class);
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
    public void isArray() {
        List<Integer> integerList = Arrays.asList(1, 2);
        Integer[] integers = ReflectionUtil.toArray(integerList);
        assertFalse(ReflectionUtil.isArray(integerList.getClass()));
        assertTrue(ReflectionUtil.isArray(integers.getClass()));
    }

    @Test
    public void isAssignableFrom() {

        Field numberField = FieldUtils.getDeclaredField(ReflectionUtilTest.class, "numberField");
        boolean isAssignableFrom = ReflectionUtil.isAssignableFrom(numberField.getGenericType(), Integer.class);
        assertTrue(isAssignableFrom);
    }


}
