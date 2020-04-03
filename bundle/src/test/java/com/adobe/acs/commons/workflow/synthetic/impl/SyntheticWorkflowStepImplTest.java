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
package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SyntheticWorkflowStepImplTest {
    @Test
    public void getId() throws Exception {
        assertEquals("test", new SyntheticWorkflowStepImpl("test", null).getId());
    }

    @Test
    public void getMetadataMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        assertEquals(map, new SyntheticWorkflowStepImpl("test", map).getMetadataMap());
    }

    @Test
    public void getIdType_Default() throws Exception {
        assertEquals(SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_NAME,
                new SyntheticWorkflowStepImpl("test", null).getIdType());
    }

    @Test
    public void getIdType_Specified() throws Exception {
        assertEquals(SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_LABEL,
                new SyntheticWorkflowStepImpl("test", SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_LABEL, null).getIdType());
    }
}