package com.adobe.acs.commons.dam.impl;

import static org.junit.Assert.assertEquals;

import com.adobe.acs.commons.dam.ColorConversion;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;


/**
 * Created by jedelson on 6/11/16.
 */
public class ColorConversionImplTest {

    private ColorConversionImpl impl;

    @Before
    public void setup() throws Exception {
        impl = new ColorConversionImpl();
        impl.activate(Collections.<String, Object>emptyMap());
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
