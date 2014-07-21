/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.cors;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

public class CorsUtilTest {
    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public final void tearDown() throws Exception {

    }

    @Test
    public final void testgetHeadersAsArraySingleValue() throws Exception {
        String inputHeader = "Authorization";
        String[] expected = new String[]{"Authorization"};
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }

    @Test
    public final void testgetHeadersAsArrayMultipleeValue() throws Exception {
        String inputHeader = "Authorization,COOKIE";
        String[] expected = new String[]{"Authorization", "COOKIE"};
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }

    @Test
    public final void testgetHeadersAsArrayNullValue() throws Exception {
        String inputHeader = "";
        String[] expected = new String[0];
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }
}