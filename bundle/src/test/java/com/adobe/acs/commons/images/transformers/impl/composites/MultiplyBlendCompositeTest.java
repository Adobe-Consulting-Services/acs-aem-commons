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
package com.adobe.acs.commons.images.transformers.impl.composites;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.RasterFormatException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiplyBlendCompositeTest {

    private static final float ALPHA = 0.75f;

    @Mock
    ColorModel notDirect;

    
    DirectColorModel srcColorModel = (DirectColorModel) DirectColorModel.getRGBdefault();

    
    DirectColorModel destColorModel = (DirectColorModel) DirectColorModel.getRGBdefault();

    @Mock
    RenderingHints hints;

    MultiplyBlendComposite composite = new MultiplyBlendComposite(ALPHA);

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        reset(notDirect, hints);
    }

    @Test
    public void testCreateContext() throws Exception {
        CompositeContext ctx = composite.createContext(srcColorModel, destColorModel, hints);
        assertNotNull(ctx);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrc() throws Exception {
        composite.createContext(notDirect, destColorModel, hints);

    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDest() throws Exception {
        composite.createContext(srcColorModel, notDirect, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrcAlphaMask() throws Exception {

        srcColorModel = new DirectColorModel(Integer.SIZE, srcColorModel.getRedMask(), srcColorModel.getGreenMask(),
                srcColorModel.getBlueMask(), 0);
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrcRedMask() throws Exception {

        srcColorModel = new DirectColorModel(Integer.SIZE, 0, srcColorModel.getGreenMask(),
                srcColorModel.getBlueMask(), srcColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrcGreenMask() throws Exception {

        srcColorModel = new DirectColorModel(Integer.SIZE, srcColorModel.getRedMask(), 0, srcColorModel.getBlueMask(),
                srcColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrcBlueMask() throws Exception {

        srcColorModel = new DirectColorModel(Integer.SIZE, srcColorModel.getRedMask(), srcColorModel.getGreenMask(), 0,
                srcColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDestAlphaMask() throws Exception {

        destColorModel = new DirectColorModel(Integer.SIZE, destColorModel.getRedMask(), destColorModel.getGreenMask(),
                destColorModel.getBlueMask(), 0);
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDestRedMask() throws Exception {

        destColorModel = new DirectColorModel(Integer.SIZE, 0, destColorModel.getGreenMask(),
                destColorModel.getBlueMask(), destColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDestGreenMask() throws Exception {

        destColorModel = new DirectColorModel(Integer.SIZE, destColorModel.getRedMask(), 0,
                destColorModel.getBlueMask(), destColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDestBlueMask() throws Exception {

        destColorModel = new DirectColorModel(Integer.SIZE, destColorModel.getRedMask(), destColorModel.getGreenMask(),
                0, destColorModel.getAlphaMask());
        composite.createContext(srcColorModel, destColorModel, hints);
    }
}
