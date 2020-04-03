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
package com.adobe.acs.commons.redirectmaps.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class MapEntryTest {

    private Resource mockResource = null;

    private static final Logger log = LoggerFactory.getLogger(MapEntryTest.class);

    @Before
    public void init() {
        log.info("init");
        MockResourceResolver mockResourceResolver = new MockResourceResolver();
        mockResource = new MockResource(mockResourceResolver,
                "/etc/acs-commons/redirect-maps/redirectmap1/jcr:content/redirects/2_123",
                "acs-commons/components/utilities/redirects");
    }

    @Test
    public void testInvalidMapEntry() {

        log.info("testInvalidMapEntry");
        String source = "/vanity 2";
        MapEntry invalid = new MapEntry(0,source, mockResource.getPath(), "File");

        invalid.setValid(false);
        invalid.setStatus("Invalid!");

        log.info("Asserting that entry is invalid");
        assertFalse(invalid.isValid());
        assertNotNull(invalid.getStatus());

        log.info("Asserting that matches expected values");
        assertEquals(invalid.getOrigin(), "File");
        assertEquals(invalid.getSource(), source);
        assertEquals(invalid.getTarget(), mockResource.getPath());
        log.debug(invalid.toString());

        log.info("Test successful!");
    }

    @Test
    public void testValidMapEntry() {

        log.info("testValidMapEntry");
        String source = "/vanity-2";
        MapEntry valid = new MapEntry(0,source, mockResource.getPath(), mockResource.getPath());

        log.info("Asserting that entry is valid");
        assertTrue(valid.isValid());

        log.info("Asserting that matches expected values");
        assertEquals(valid.getOrigin(), mockResource.getPath());
        assertEquals(valid.getSource(), source);
        assertEquals(valid.getTarget(), mockResource.getPath());
        log.debug(valid.toString());

        log.info("Test successful!");
    }
    
}
