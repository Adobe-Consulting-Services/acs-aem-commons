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
package com.adobe.acs.commons.dam;

import aQute.bnd.annotation.ProviderType;

/**
 * Service interface for performing color space conversion operations.
 */
@ProviderType
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public interface ColorConversion {

    /**
     * Convert a color in the CYMK color space to RGB.
     *
     * @param cymk a CYMK color
     * @return an RGB color
     */
    RGB toRGB(CMYK cymk);

    /**
     * Convert a color in the LAB color space to RGB.
     *
     * @param lab a LAB color
     * @return an RGB color
     */
    RGB toRGB(LAB lab);

    @ProviderType
    final class RGB {
        public final int red;
        public final int green;
        public final int blue;

        public RGB(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public String toString() {
            return "RGB{"
                    + "red=" + red
                    + ", green=" + green
                    + ", blue=" + blue
                    + '}';
        }
    }

    @ProviderType
    @SuppressWarnings({"checkstyle:membername", "checkstyle:parametername"})
    final class LAB {
        public final float lightness;
        public final int a;
        public final int b;

        public LAB(float lightness, int a, int b) {
            this.lightness = lightness;
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "LAB{"
                    + "lightness=" + lightness
                    + ", a=" + a
                    + ", b=" + b
                    + '}';
        }
    }

    @ProviderType
    final class CMYK {
        public final int cyan;
        public final int magenta;
        public final int yellow;
        public final int black;

        public CMYK(int cyan, int magenta, int yellow, int black) {
            this.cyan = cyan;
            this.magenta = magenta;
            this.yellow = yellow;
            this.black = black;
        }

        @Override
        public String toString() {
            return "CYMK{"
                    + "cyan=" + cyan
                    + ", magenta=" + magenta
                    + ", yellow=" + yellow
                    + ", black=" + black
                    + '}';
        }
    }
}
