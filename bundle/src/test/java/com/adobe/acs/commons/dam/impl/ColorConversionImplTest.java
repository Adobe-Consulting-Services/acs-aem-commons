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
package com.adobe.acs.commons.dam.impl;

import static org.junit.Assert.assertEquals;

import com.adobe.acs.commons.dam.ColorConversion;

import io.wcm.testing.mock.aem.junit.AemContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class ColorConversionImplTest {

    private ColorConversionImpl impl;
    
    @Rule
    public AemContext context = new AemContext();
    

    @Before
    public void setup() throws Exception {
        impl = new ColorConversionImpl();
        context.registerInjectActivateService(impl, Collections.<String, Object>emptyMap());
    }

    @Test
    public void testCmyk() {
        ColorConversion.CMYK cmyk = new ColorConversion.CMYK(0,0,100,0);

        ColorConversion.RGB rgb = impl.toRGB(cmyk);
        assertEquals(255, rgb.red);
        assertEquals(241, rgb.green);
        assertEquals(0, rgb.blue);
    }


    @Test
    public void testLab() {
        ColorConversion.LAB lab = new ColorConversion.LAB(54, -16, -36);

        ColorConversion.RGB rgb = impl.toRGB(lab);
        assertEquals(0, rgb.red);
        assertEquals(140, rgb.green);
        assertEquals(191, rgb.blue);

    }
}
