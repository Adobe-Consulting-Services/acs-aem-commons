/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2023 Adobe
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
package com.adobe.acs.commons.reports.models;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PredictedTagsCellValueTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictedTagsCellValueTest.class);

    private final AemContext context = new AemContext();

    private static final String ASSET_PATH = "/content/dam/sample.jpg";
    private static final String PROPERTY_PATH = "jcr:content/metadata/predictedTags";

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource mockResource;
    @Mock
    private Resource predictedTagsResourceMock;
    @Mock
    private Resource predictedTagsChildResourceMock;
    @Mock
    private Asset assetMock;

    @Mock
    private ResourceResolver resolver;

    @InjectMocks
    private PredictedTagsCellValue underTest;

    @Before
    public void setUp() throws IllegalAccessException {
        LOGGER.info("setup invoked.");

        when(request.getAttribute("result")).thenReturn(mockResource);
        when(mockResource.getResourceResolver()).thenReturn(resolver);
        when(mockResource.getResourceType()).thenReturn(DamConstants.NT_DAM_ASSET);
        when(mockResource.adaptTo(Asset.class)).thenReturn(assetMock);
    }

    @Test
    public void getEmptyPredictedTags() throws IllegalAccessException {
        LOGGER.info("getEmptyPredictedTags invoked.");
        final MockSlingHttpServletRequest request = context.request();
        request.setAttribute("result", mockResource);
        FieldUtils.writeField(underTest, "property", PROPERTY_PATH, true);

        final List<PredictedTag> predictedTags = underTest.getPredictedTags();
        assertNotNull(predictedTags);
        assertTrue(CollectionUtils.isEmpty(predictedTags));
        LOGGER.info("getEmptyPredictedTags execution is successful.");
    }

    @Test
    public void getExistingPredictedTags() throws IllegalAccessException {
        LOGGER.info("getExistingPredictedTags invoked.");
        when(mockResource.getPath()).thenReturn(ASSET_PATH);
        when(resolver.getResource(PathUtil.append(ASSET_PATH, PROPERTY_PATH))).thenReturn(predictedTagsResourceMock);

        when(predictedTagsResourceMock.getChildren()).thenReturn(Collections.singletonList(predictedTagsChildResourceMock));
        final PredictedTag predictedTag = new PredictedTag();
        FieldUtils.writeField(predictedTag, "name", "prediction", true);
        FieldUtils.writeField(predictedTag, "confidence", 0.9d, true);
        when(predictedTagsChildResourceMock.adaptTo(PredictedTag.class)).thenReturn(predictedTag);

        final MockSlingHttpServletRequest request = context.request();
        request.setAttribute("result", mockResource);
        FieldUtils.writeField(underTest, "property", PROPERTY_PATH, true);

        final List<PredictedTag> predictedTags = underTest.getPredictedTags();
        assertNotNull(predictedTags);
        assertEquals(1, predictedTags.size());
        assertEquals(predictedTag, predictedTags.get(0));
        LOGGER.info("getExistingPredictedTags execution is successful.");
    }

}
