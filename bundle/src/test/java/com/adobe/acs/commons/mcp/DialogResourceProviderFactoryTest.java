/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.mcp;

import com.adobe.acs.commons.mcp.impl.DialogResourceProviderFactoryImpl;
import com.adobe.acs.commons.mcp.model.SimpleModelOne;
import com.adobe.acs.commons.mcp.model.SimpleModelThree;
import com.adobe.acs.commons.mcp.model.SimpleModelTwo;
import java.io.IOException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test various aspects of the Dialog Resource Provider service
 */
public class DialogResourceProviderFactoryTest {

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.JCR_MOCK);

    private DialogResourceProviderFactoryImpl dialogProvider;

    @Before
    public void init() throws IOException {
        dialogProvider = new DialogResourceProviderFactoryImpl();
        slingContext.addModelsForPackage("com.adobe.acs.commons.mcp.model");
        slingContext.registerInjectActivateService(dialogProvider, "enabled", true);
    }

    @Test
    public void testResourceTypeDetection() throws IOException {
        assertTrue("Should detect simpleModelOne", dialogProvider.getActiveProviders().containsKey(SimpleModelOne.class.getName()));
        assertTrue("Should detect simpleModelTwo", dialogProvider.getActiveProviders().containsKey(SimpleModelTwo.class.getName()));
        assertTrue("Should detect simpleModelThree", dialogProvider.getActiveProviders().containsKey(SimpleModelThree.class.getName()));
    }

    @Test
    public void testEnableSwitch() throws IOException {
        assertTrue("Service should report being enabled", dialogProvider.isEnabled());
        assertEquals("Should have 3 services registered", 3, dialogProvider.getActiveProviders().size());
        dialogProvider.setEnabled(false);
        assertFalse("Service should report being disabled", dialogProvider.isEnabled());
        assertEquals("Should have 0 services registered", 0, dialogProvider.getActiveProviders().size());
        assertFalse("Should not resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));
        dialogProvider.setEnabled(true);
        assertTrue("Service should report being enabled", dialogProvider.isEnabled());
        assertEquals("Should have 3 services registered", 3, dialogProvider.getActiveProviders().size());
        assertTrue("Should resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));

    }

    @Test
    public void testResourceResolution() {
        // Quick parity check on test method
        assertFalse("Should not resolve non-existing resource", resourceExists("/this/does/not/exist"));
        // Check basic resource as well as "items" child.
        assertTrue("Should resolve model one dialog", resourceExists("/apps/test/model1/cq:dialog"));
        assertTrue("Should resolve model two dialog", resourceExists("/apps/test/model2/cq:dialog"));
        assertTrue("Should resolve model three dialog", resourceExists("/apps/test/model3/cq:dialog"));
        assertTrue("Should resolve model one dialog items", resourceExists("/apps/test/model1/cq:dialog/content/items"));
        assertTrue("Should resolve model two dialog items", resourceExists("/apps/test/model2/cq:dialog/content/items"));
        assertTrue("Should resolve model three dialog items", resourceExists("/apps/test/model3/cq:dialog/content/items"));
    }

    private boolean resourceExists(String path) {
        return slingContext.resourceResolver().getResource(path) != null;
    }
}