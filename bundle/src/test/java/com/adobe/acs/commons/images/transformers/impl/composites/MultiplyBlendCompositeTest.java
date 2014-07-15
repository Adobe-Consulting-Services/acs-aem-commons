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

import static org.mockito.Mockito.*;

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
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest( { ColorModel.class, DirectColorModel.class } )
public class MultiplyBlendCompositeTest {

	private static final float ALPHA = 0.75f;

	@Mock
	ColorModel notDirect;
	
	@Mock
	DirectColorModel srcColorModel;
	
	@Mock
	DirectColorModel destColorModel;
	
	@Mock
	RenderingHints hints;
	
	MultiplyBlendComposite composite = new MultiplyBlendComposite(ALPHA);
	
    @Before
    public void setUp() throws Exception {

    	when(srcColorModel.getAlphaMask()).thenReturn(MultiplyBlendComposite.ALPHA_MASK);
    	when(srcColorModel.getRedMask()).thenReturn(MultiplyBlendComposite.RED_MASK);
    	when(srcColorModel.getGreenMask()).thenReturn(MultiplyBlendComposite.GREEN_MASK);
    	when(srcColorModel.getBlueMask()).thenReturn(MultiplyBlendComposite.BLUE_MASK);
    
    	when(destColorModel.getAlphaMask()).thenReturn(MultiplyBlendComposite.ALPHA_MASK);
    	when(destColorModel.getRedMask()).thenReturn(MultiplyBlendComposite.RED_MASK);
    	when(destColorModel.getGreenMask()).thenReturn(MultiplyBlendComposite.GREEN_MASK);
    	when(destColorModel.getBlueMask()).thenReturn(MultiplyBlendComposite.BLUE_MASK);
    	
    }

    @After
    public void tearDown() throws Exception {
    	reset(notDirect, srcColorModel, destColorModel, hints);
    }
    
    @Test
    public void testCreateContext() throws Exception {

    	composite.createContext(srcColorModel, destColorModel, hints);

    	verifyZeroInteractions(srcColorModel, destColorModel, hints);
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidSrc() throws Exception {
    	composite.createContext(notDirect, destColorModel, hints);
        verifyZeroInteractions(notDirect, destColorModel, hints);
    	
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidDest() throws Exception {
    	composite.createContext(srcColorModel, notDirect, hints);
        verifyZeroInteractions(notDirect, srcColorModel, hints);
    }
    
    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidAlphaMask() throws Exception {
    	
//    	when(srcColorModel.getAlphaMask()).thenReturn((int) 0);
    	composite.createContext(srcColorModel, destColorModel, hints);
    	
    }
    
    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidRedMask() throws Exception {
    	
//    	when(srcColorModel.getRedMask()).thenReturn((int) 0);
    	composite.createContext(srcColorModel, destColorModel, hints);
    	
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidGreenMask() throws Exception {
    	
//    	when(srcColorModel.getGreenMask()).thenReturn((int) 0);
    	composite.createContext(srcColorModel, destColorModel, hints);
    	
    }

    @Test(expected = RasterFormatException.class)
    public void testCreateContextInvalidBlueMask() throws Exception {
    	
//    	when(srcColorModel.getGreenMask()).thenReturn((int) 0);
    	composite.createContext(srcColorModel, destColorModel, hints);
    	
    }
}
