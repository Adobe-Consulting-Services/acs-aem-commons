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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.query.Query;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.tika.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public class RedirectMapModelTest {

    private static final String[] vanities = new String[] { "/vanity1", "/vanity 2" };
    private Resource searchResultsResource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource redirectMapResource;

    private List<RedirectConfigModel> redirectConfigs = new ArrayList<RedirectConfigModel>() {
        private static final long serialVersionUID = 1L;

        {
            add(new RedirectConfigModel() {

                @Override
                public String getDomain() {
                    return "www.adobe.com";
                }

                @Override
                public String getPath() {
                    return "/content/adobe";
                }

                @Override
                public String getProperty() {
                    return "vanity";
                }

                @Override
                public String getProtocol() {
                    return "https";
                }

                @Override
                public Resource getResource() {
                    return mock(Resource.class);
                }
            });
        }
    };

    @InjectMocks
    private RedirectMapModel model;

    private static final Logger log = LoggerFactory.getLogger(RedirectMapModelTest.class);

    @Before
    public void init() throws Exception {
        log.info("init");

        PrivateAccessor.setField(model, "redirects", redirectConfigs);

        searchResultsResource = mock(Resource.class);

        Resource childResource = mock(Resource.class);
        doReturn(childResource).when(searchResultsResource).getChild(JcrConstants.JCR_CONTENT);

        ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("vanity", vanities);
            }
        });
        doReturn(properties).when(childResource).getValueMap();
        doReturn("/content/adobe/en").when(searchResultsResource).getPath();

        doReturn(IOUtils.toInputStream("/vanity3\thttp://www.adobe.com")).when(redirectMapResource)
                .adaptTo(InputStream.class);

        doReturn(new ArrayList<Resource>() {
            private static final long serialVersionUID = 1L;
            {
                add(searchResultsResource);
            }
        }.iterator()).when(resolver).findResources(
                "SELECT * FROM [cq:Page] WHERE [jcr:content/vanity] IS NOT NULL AND (ISDESCENDANTNODE([/content/adobe]) OR [jcr:path]='/content/adobe')",
                Query.JCR_SQL2);

        doReturn(new ArrayList<Resource>().iterator()).when(resolver).findResources(
                "SELECT * FROM [dam:Asset] WHERE [jcr:content/vanity] IS NOT NULL AND (ISDESCENDANTNODE([/content/adobe]) OR [jcr:path]='/content/adobe')",
                Query.JCR_SQL2);

    }

    @Test
    public void testGetInvalidEntries() throws IOException {

        log.info("testGetInvalidEntries");
        List<MapEntry> mapEntries = model.getInvalidEntries();

        log.info("Asserting that the invalid results are found");
        assertNotNull(mapEntries);
        assertEquals(1, mapEntries.size());
        assertEquals("/vanity 2", mapEntries.get(0).getSource());

        log.info("Test successful!");
    }

    @Test
    public void testGetRedirectMap() throws IOException {

        log.info("testGetInvalidEntries");
        String redirectMap = model.getRedirectMap();

        log.info("Asserting the expected redirect map found");
        assertNotNull(redirectMap);
        assertFalse(redirectMap.contains("/vanity 2"));
        assertTrue(redirectMap.contains("/vanity1"));
        assertTrue(redirectMap.contains("/vanity3"));
        log.info("Test successful!");
    }
}
