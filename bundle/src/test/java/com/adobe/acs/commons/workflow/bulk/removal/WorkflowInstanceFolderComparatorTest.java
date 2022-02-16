/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.workflow.bulk.removal;

import com.adobe.acs.commons.workflow.bulk.removal.impl.WorkflowInstanceFolderComparator;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceFolderComparatorTest {

    @Test
    public void testCompare_Simple() throws Exception {
        Resource one = mock(Resource.class);
        Resource two = mock(Resource.class);

        when(one.getName()).thenReturn("1980-09-16");
        when(two.getName()).thenReturn("1980-09-16_1");

        List<Resource> actual = new ArrayList<Resource>();

        actual.add(two);
        actual.add(one);

        Collections.sort(actual, new WorkflowInstanceFolderComparator());

        assertEquals(actual.get(0).getName(), one.getName());
        assertEquals(actual.get(1).getName(), two.getName());
    }

    @Test
    public void testCompare() throws Exception {

        Resource one = mock(Resource.class);
        Resource two = mock(Resource.class);
        Resource three = mock(Resource.class);
        Resource four = mock(Resource.class);
        Resource five = mock(Resource.class);

        when(one.getName()).thenReturn("2009-09-02_0");
        when(two.getName()).thenReturn("2012-01-12");
        when(three.getName()).thenReturn("2014-01-02_1");
        when(four.getName()).thenReturn("2014-06-06");
        when(five.getName()).thenReturn("2014-06-06_2");

        List<Resource> actual = new ArrayList<Resource>();

        actual.add(four);
        actual.add(five);
        actual.add(one);
        actual.add(three);
        actual.add(two);

        Collections.sort(actual, new WorkflowInstanceFolderComparator());

        assertEquals(actual.get(0).getName(), one.getName());
        assertEquals(actual.get(1).getName(), two.getName());
        assertEquals(actual.get(2).getName(), three.getName());
        assertEquals(actual.get(3).getName(), four.getName());
        assertEquals(actual.get(4).getName(), five.getName());
    }
}