/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WorkflowHelperImplTest {

    private WorkflowHelperImpl workflowHelper = new WorkflowHelperImpl();

    @Test
    public void testBuildArgumentsFromNullArguments() throws Exception {
        MetaDataMap map = mock(MetaDataMap.class);
        when(map.get(WorkflowHelper.PROCESS_ARGS, String.class)).thenReturn(null);
        String[] result = workflowHelper.buildArguments(map);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testBuildArgumentsFromBlankArguments() throws Exception {
        MetaDataMap map = mock(MetaDataMap.class);
        when(map.get(WorkflowHelper.PROCESS_ARGS, String.class)).thenReturn("");
        String[] result = workflowHelper.buildArguments(map);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testBuildArguments() throws Exception {
        MetaDataMap map = mock(MetaDataMap.class);
        when(map.get(WorkflowHelper.PROCESS_ARGS, String.class)).thenReturn("foo:bar,goo:baz");
        String[] result = workflowHelper.buildArguments(map);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("foo:bar", result[0]);
        assertEquals("goo:baz", result[1]);
    }

    @Test
    public void getValuesFromArgsSingleArgument() throws Exception {
        List<String> values = workflowHelper.getValuesFromArgs("foo", new String[] { "foo:bar", "goo:baz "});
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("bar", values.get(0));
    }

    @Test
    public void getValuesFromArgsMultipleArgument() throws Exception {
        List<String> values = workflowHelper.getValuesFromArgs("foo", new String[] { "foo:bar", "foo:bar2", "goo:baz "});
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals("bar", values.get(0));
        assertEquals("bar2", values.get(1));
    }

}