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

import com.adobe.acs.commons.reports.internal.PredictedTagsUtil;
import com.day.cq.dam.api.Asset;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

public class PredictedTagReportCellCSVExporterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictedTagReportCellCSVExporterTest.class);

    private static final String ASSET_PATH = "/content/dam/sample.jpg";

    public static final String PREDICTED_TAGS_PROPERTY_PATH = "jcr:content/metadata/predictedTags";
    public static final String TAG1_NAME = "tag1Name";
    public static final double TAG1_CONFIDENCE = 0.63;
    public static final boolean TAG1_IS_CUSTOM = false;
    public static final String TAG2_NAME = "tag2Name";
    public static final double TAG2_CONFIDENCE = 0.75;
    public static final boolean TAG2_IS_CUSTOM = true;

    @Mock
    private Resource mockResource;

    @Mock
    private ResourceResolver mockResolver;

    @Mock
    private Asset mockAsset;

    @Mock
    private Resource predictedTagsParentResource;

    @Mock
    private Resource predictedTag1Resource;

    @Mock
    private PredictedTag predictedTag1;

    @Mock
    private Resource predictedTag2Resource;

    @Mock
    private PredictedTag predictedTag2;


    @Spy
    PredictedTagsUtil predictedTagsUtil;

    @InjectMocks
    PredictedTagReportCellCSVExporter systemUnderTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        assertNotNull(systemUnderTest);

        doReturn(mockResolver).when(mockResource).getResourceResolver();
        doReturn(mockAsset).when(predictedTagsUtil).resolveToAsset(Mockito.any(Resource.class));

        doReturn(ASSET_PATH).when(mockResource).getPath();
        doReturn(ASSET_PATH).when(mockAsset).getPath();
    }

    @Test
    public void testPropertyDoesNotExist() throws IllegalAccessException {
        LOGGER.info("testEmpty");
        FieldUtils.writeField(systemUnderTest, "property", "nonExistingProperty", true);
        assertEquals("", systemUnderTest.getValue(mockResource));
        LOGGER.info("Test successful!");
    }

    @Test
    public void testPropertyHasTags() throws IllegalAccessException {
        LOGGER.info("testPropertyHasTags");

        String predictedPath = ASSET_PATH + "/" + PREDICTED_TAGS_PROPERTY_PATH;
        doReturn(predictedTagsParentResource).when(mockResolver).getResource(predictedPath);
        doReturn(ImmutableList.of(predictedTag1Resource, predictedTag2Resource)).when(predictedTagsParentResource).getChildren();
        doReturn(predictedTag1).when(predictedTag1Resource).adaptTo(PredictedTag.class);
        doReturn(predictedTag2).when(predictedTag2Resource).adaptTo(PredictedTag.class);

        doReturn(TAG1_NAME).when(predictedTag1).getName();
        doReturn(TAG1_CONFIDENCE).when(predictedTag1).getConfidence();
        doReturn(TAG1_IS_CUSTOM).when(predictedTag1).isCustom();

        doReturn(TAG2_NAME).when(predictedTag2).getName();
        doReturn(TAG2_CONFIDENCE).when(predictedTag2).getConfidence();
        doReturn(TAG2_IS_CUSTOM).when(predictedTag2).isCustom();

        FieldUtils.writeField(systemUnderTest, "property", PREDICTED_TAGS_PROPERTY_PATH, true);

        // Expect tag2 to be first, as it has a higher confidence
        String expectedValue = TAG2_NAME + ";" + TAG1_NAME;
        assertEquals(expectedValue, systemUnderTest.getValue(mockResource));
        LOGGER.info("Test successful!");
    }

    @Test
    public void testShowConfidence() throws IllegalAccessException {
        LOGGER.info("testShowConfidence");

        String predictedPath = ASSET_PATH + "/" + PREDICTED_TAGS_PROPERTY_PATH;
        doReturn(predictedTagsParentResource).when(mockResolver).getResource(predictedPath);
        doReturn(ImmutableList.of(predictedTag1Resource, predictedTag2Resource)).when(predictedTagsParentResource).getChildren();
        doReturn(predictedTag1).when(predictedTag1Resource).adaptTo(PredictedTag.class);
        doReturn(predictedTag2).when(predictedTag2Resource).adaptTo(PredictedTag.class);

        doReturn(TAG1_NAME).when(predictedTag1).getName();
        doReturn(TAG1_CONFIDENCE).when(predictedTag1).getConfidence();
        doReturn(TAG1_IS_CUSTOM).when(predictedTag1).isCustom();

        doReturn(TAG2_NAME).when(predictedTag2).getName();
        doReturn(TAG2_CONFIDENCE).when(predictedTag2).getConfidence();
        doReturn(TAG2_IS_CUSTOM).when(predictedTag2).isCustom();

        FieldUtils.writeField(systemUnderTest, "property", PREDICTED_TAGS_PROPERTY_PATH, true);
        FieldUtils.writeField(systemUnderTest, "confidenceShown", true, true);

        // Expect tag2 to be first, as it has a higher confidence
        String expectedValue = TAG2_NAME + " [" + confidenceAsString(TAG2_CONFIDENCE) + "];" + TAG1_NAME + " [" + confidenceAsString(TAG1_CONFIDENCE) + "]";
        assertEquals(expectedValue, systemUnderTest.getValue(mockResource));
        LOGGER.info("Test successful!");
    }

    @Test
    public void testFilterTags() throws IllegalAccessException {
        LOGGER.info("testFilterTags");

        String predictedPath = ASSET_PATH + "/" + PREDICTED_TAGS_PROPERTY_PATH;
        doReturn(predictedTagsParentResource).when(mockResolver).getResource(predictedPath);
        doReturn(ImmutableList.of(predictedTag1Resource, predictedTag2Resource)).when(predictedTagsParentResource).getChildren();
        doReturn(predictedTag1).when(predictedTag1Resource).adaptTo(PredictedTag.class);
        doReturn(predictedTag2).when(predictedTag2Resource).adaptTo(PredictedTag.class);

        doReturn(TAG1_NAME).when(predictedTag1).getName();
        doReturn(TAG1_CONFIDENCE).when(predictedTag1).getConfidence();
        doReturn(TAG1_IS_CUSTOM).when(predictedTag1).isCustom();

        doReturn(TAG2_NAME).when(predictedTag2).getName();
        doReturn(TAG2_CONFIDENCE).when(predictedTag2).getConfidence();
        doReturn(TAG2_IS_CUSTOM).when(predictedTag2).isCustom();

        FieldUtils.writeField(systemUnderTest, "property", PREDICTED_TAGS_PROPERTY_PATH, true);
        FieldUtils.writeField(systemUnderTest, "confidenceShown", true, true);
        FieldUtils.writeField(systemUnderTest, "lowerConfidenceThreshold", 0.65, true);

        // Expect only tag2, as tag1 is filtered
        String expectedValue = TAG2_NAME + " [" + confidenceAsString(TAG2_CONFIDENCE) + "]";
        assertEquals(expectedValue, systemUnderTest.getValue(mockResource));
        LOGGER.info("Test successful!");
    }

    protected String confidenceAsString(double confidenceValue) {
        return String.format("%.4f", confidenceValue);
    }
}
