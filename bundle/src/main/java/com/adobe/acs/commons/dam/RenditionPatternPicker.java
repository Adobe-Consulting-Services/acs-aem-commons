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
package com.adobe.acs.commons.dam;

import aQute.bnd.annotation.ProviderType;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RenditionPicker that picks Assets based on a Regex pattern that matches against
 * Rendition names.
 */
@ProviderType
public class RenditionPatternPicker implements RenditionPicker {

    private final Pattern pattern;

    /**
     * Create an Asset Rendition Picker that will pick a Rendition by matching
     * the supplied Regex pattern (as String).
     *
     * @param pattern Regex pattern to match against Rendition names.
     */
    public RenditionPatternPicker(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    /**
     * Create an Asset Rendition Picker that will pick a Rendition by matching
     * the supplied Regex pattern.
     *
     * @param pattern Pattern used to find the Asset rendition
     */
    public RenditionPatternPicker(final Pattern pattern) {
        this.pattern = pattern;
    }

    private Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Gets the rendition which matches against the constructor's Regex pattern.
     * <p>
     * If no matches are made and an Original exists, returns the Original.
     * <p>
     * If no matches are made and an Original doesn't exist, return the first Rendition.
     *
     * @param asset Asset whose Renditions will be selected.
     * @return The first rendition whose name matches the supplied pattern (via constructor).
     */
    @Override
    public final Rendition getRendition(final Asset asset) {

        final List<Rendition> renditions = asset.getRenditions();
        final Pattern p = getPattern();

        boolean hasOriginal = asset.getOriginal() != null;
        boolean hasRenditions = renditions.size() > 0;

        for (final Rendition rendition : renditions) {
            final Matcher m = p.matcher(rendition.getName());

            if (m.find()) {
                return rendition;
            }
        }

        if (hasOriginal) {
            return asset.getOriginal();
        } else if (hasRenditions) {
            return renditions.get(0);
        } else {
            return null;
        }
    }
}