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

package com.adobe.acs.commons.images.transformers.impl.composites.contexts;

/**
 * Represents a Color bitmask for making calculations.
 * 
 */
public enum ColorMask {

    /**
     * Red Color Mask.
     */
    RED(16),

    /**
     * Green Color Mask.
     */
    GREEN(8),

    /**
     * Blue Color Mask.
     */
    BLUE(0);

    /**
     * Maximum value a single RGB value is allowed.
     */
    public static final int MAX_DEPTH = 0xFF;

    private int mask;

    ColorMask(int mask) {
        this.mask = mask;
    }

    int getMask() {
        return mask;
    }

}
