/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.*;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * Tests a few cases for folder relocator
 */
public class FolderRelocatorTest {

    @Test
    public void testInit() throws LoginException, DeserializeException, RepositoryException {
        FolderRelocator tool = new FolderRelocator();
        ProcessInstance instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test");
        assertEquals("Folder relocator: relocator test", instance.getName());
        try {
            instance.init(getMockResolver(), Collections.EMPTY_MAP);
            fail("That should have thrown an error");
        } catch (DeserializeException ex) {
            // Expected
        }
        Map<String, Object> values = new HashMap<>();
        values.put("sourcePaths", "/content/folderA");
        values.put("destinationPath", "/content/folderB");
        instance.init(getMockResolver(), values);
    }

    private ControlledProcessManager getControlledProcessManager() throws LoginException {
        ActionManager am = getActionManager();

        ActionManagerFactory amf = mock(ActionManagerFactoryImpl.class);
        when(amf.createTaskManager(any(), any(), anyInt())).thenReturn(am);

        ControlledProcessManager cpm = mock(ControlledProcessManager.class);
        when(cpm.getActionManagerFactory()).thenReturn(amf);
        return cpm;
    }

}
