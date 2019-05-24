/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.version.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class EvolutionConfigTest {

    @Test
    public void testHandleProperties() {
    	final String[] ignoreProps = { "jcr:.*", "cq:.*", "par/cq:.*" };
    	final String[] ignoreRes = {};

    	final EvolutionConfig config = new EvolutionConfig(ignoreProps, ignoreRes);
        assertEquals(false, config.handleProperty("jcr:title"));
        assertEquals(true, config.handleProperty("test/jcr:title"));
        assertEquals(false, config.handleProperty("cq:name"));
        assertEquals(true, config.handleProperty("test/cq:name"));
        assertEquals(false, config.handleProperty("par/cq:name"));
    }

    @Test
    public void testHandleResources() {
    	final String[] ignoreProps = {};
    	final String[] ignoreRes = { "image", "par/image", ".*test.*" };

    	final EvolutionConfig config = new EvolutionConfig(ignoreProps, ignoreRes);
        assertEquals(false, config.handleResource("image"));
        assertEquals(false, config.handleResource("par/image"));
        assertEquals(true, config.handleResource("bert/image"));
        assertEquals(false, config.handleResource("test"));
        assertEquals(false, config.handleResource("tester"));
        assertEquals(false, config.handleResource("par/test"));
        assertEquals(false, config.handleResource("par/testen"));
    }

}
