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

package com.adobe.acs.commons.images.transformers.impl.adhoc;

import junit.framework.Assert;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class ImageQualityTransformerTest {
    private static String MIME_TYPE_PNG = "image/png";

    private static String MIME_TYPE_GIF = "image/gif";

    ValueMap params = null;

    @Before
    public void setUp() throws Exception {
        params = new ValueMapDecorator(new HashMap<String, Object>());
    }

    @Test
    public void testGetQuality_NoParam() throws Exception {
        params.put("quality", "");

        final double expected = .82D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality() throws Exception {
        params.put("quality", "75");

        final double expected = .75D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality_Min() throws Exception {
        params.put("quality", "0");

        final double expected = 0D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality_LessThanMin() throws Exception {
        params.put("quality", ".2");

        final double expected = .82D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality_Max() throws Exception {
        params.put("quality", "100");

        final double expected = 1D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality_GreaterThanMax() throws Exception {
        params.put("quality", "101");

        final double expected = .82D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_PNG, params));
    }

    @Test
    public void testGetQuality_Gif() throws Exception {
        params.put("quality", "50");

        final double expected = 255 * .5D;
        Assert.assertEquals(expected, ImageQualityTransformer.getQuality(MIME_TYPE_GIF, params));
    }
}