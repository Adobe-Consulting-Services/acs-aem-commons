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

import com.adobe.acs.commons.dam.ColorConversion;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.InputStream;
import java.util.Map;

@Component(label = "ACS AEM Commons - Color Conversion", description = "ACS AEM Commons - Color Conversion", metatype = true)
@Service
public final class ColorConversionImpl implements ColorConversion {

    private static final String DEFAULT_CMYK_PROFILE = "JapanColor2001Coated";

    @Property(label = "CMYK ICC Profile", description = "ICC Profile for CMYK to RGB Conversion",
            value = DEFAULT_CMYK_PROFILE,
            options = {
            @PropertyOption(name = "CoatedFOGRA27", value = "CoatedFOGRA27"),
            @PropertyOption(name = "CoatedFOGRA39", value = "CoatedFOGRA39"),
            @PropertyOption(name = "JapanColor2001Coated", value = "JapanColor2001Coated"),
            @PropertyOption(name = "JapanColor2001Uncoated", value = "JapanColor2001Uncoated"),
            @PropertyOption(name = "JapanColor2002Newspaper", value = "JapanColor2002Newspaper"),
            @PropertyOption(name = "JapanWebCoated", value = "JapanWebCoated"),
            @PropertyOption(name = "UncoatedFOGRA29", value = "UncoatedFOGRA29"),
            @PropertyOption(name = "USSheetfedCoated", value = "USSheetfedCoated"),
            @PropertyOption(name = "USSheetfedUncoated", value = "USSheetfedUncoated"),
            @PropertyOption(name = "USWebCoatedSWOP", value = "USWebCoatedSWOP"),
            @PropertyOption(name = "USWebUncoated", value = "USWebUncoated"),
            @PropertyOption(name = "WebCoatedFOGRA28", value = "WebCoatedFOGRA28")
    })
    private static final String PROP_CMKY_PROFILE = "cmyk.icc.profile";

    /**
     * XYZ to sRGB conversion matrix
     */
    private static final double[][] xyzTosRgbMatrix = {{ 3.2406, -1.5372, -0.4986},
            {-0.9689,  1.8758,  0.0415},
            { 0.0557, -0.2040,  1.0570}};

    @Activate
    protected void activate(Map<String, Object> properties) throws Exception {
        String profileName = PropertiesUtil.toString(properties.get(PROP_CMKY_PROFILE), DEFAULT_CMYK_PROFILE);

        InputStream iccData = getClass().getClassLoader().getResourceAsStream("icc/cmyk/" + profileName + ".icc");
        ICC_Profile profile = ICC_Profile.getInstance(iccData);
        cmykColorSpace = new ICC_ColorSpace(profile);
    }

    private ColorSpace cmykColorSpace;

    @Override
    public RGB toRGB(CMYK cymk) {
        float[] input = new float[] {
                ((float) cymk.cyan) / 100,
                ((float) cymk.magenta) / 100,
                ((float) cymk.yellow) / 100,
                ((float) cymk.black) / 100
        };
        float[] output = cmykColorSpace.toRGB(input);
        RGB rgb = new RGB(
                Math.round(output[0] * 255),
                Math.round(output[1] * 255),
                Math.round(output[2] * 255)
        );
        return rgb;
    }

    @Override
    public RGB toRGB(LAB lab) {
        XYZ xyz = toXYZ(lab);

        double x = xyz.x / 100.0;
        double y = xyz.y / 100.0;
        double z = xyz.z / 100.0;

        // [r g b] = [X Y Z][xyzTosRgbMatrix]
        double r = (x * xyzTosRgbMatrix[0][0]) + (y * xyzTosRgbMatrix[0][1]) + (z * xyzTosRgbMatrix[0][2]);
        double g = (x * xyzTosRgbMatrix[1][0]) + (y * xyzTosRgbMatrix[1][1]) + (z * xyzTosRgbMatrix[1][2]);
        double b = (x * xyzTosRgbMatrix[2][0]) + (y * xyzTosRgbMatrix[2][1]) + (z * xyzTosRgbMatrix[2][2]);

        // assume sRGB
        if (r > 0.0031308) {
            r = ((1.055 * Math.pow(r, 1.0 / 2.4)) - 0.055);
        } else {
            r = (r * 12.92);
        }
        if (g > 0.0031308) {
            g = ((1.055 * Math.pow(g, 1.0 / 2.4)) - 0.055);
        } else {
            g = (g * 12.92);
        }
        if (b > 0.0031308) {
            b = ((1.055 * Math.pow(b, 1.0 / 2.4)) - 0.055);
        } else {
            b = (b * 12.92);
        }

        r = (r < 0) ? 0 : r;
        g = (g < 0) ? 0 : g;
        b = (b < 0) ? 0 : b;

        // convert 0..1 into 0..255
        RGB rgb = new RGB(
                (int) Math.round(r * 255),
                (int) Math.round(g * 255),
                (int) Math.round(b * 255)
        );
        return rgb;
    }

    private XYZ toXYZ(LAB lab) {
        double y = (lab.lightness + 16.0) / 116.0;
        double y3 = Math.pow(y, 3.0);
        double x = (lab.a / 500.0) + y;
        double x3 = Math.pow(x, 3.0);
        double z = y - (lab.b / 200.0);
        double z3 = Math.pow(z, 3.0);

        if (y3 > 0.008856) {
            y = y3;
        }
        else {
            y = (y - (16.0 / 116.0)) / 7.787;
        }
        if (x3 > 0.008856) {
            x = x3;
        }
        else {
            x = (x - (16.0 / 116.0)) / 7.787;
        }
        if (z3 > 0.008856) {
            z = z3;
        }
        else {
            z = (z - (16.0 / 116.0)) / 7.787;
        }

        XYZ result = new XYZ();
        result.x = x * 95.0429;
        result.y = y * 100;
        result.z = z * 108.8900;

        return result;

    }

    private class XYZ {
        private double x;
        private double y;
        private double z;
    }
}
