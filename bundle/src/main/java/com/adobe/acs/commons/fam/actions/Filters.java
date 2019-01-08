/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.fam.actions;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.functions.CheckedBiFunction;
import com.adobe.acs.commons.functions.CheckedFunction;
import com.day.cq.dam.commons.util.DamUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Utility filters useful for sifting through search results without over-complicating search queries.
 */
@ProviderType
public class Filters {
    private Filters() {
        // Utility class cannot be instantiated directly.
    }
    
    public static final String ORIGINAL_RENDITION = "original";

    //--- Filters (for using withQueryResults)
    /**
     * Returns opposite of its input, e.g. filterMatching(glob).andThen(not)
     */
    public static final CheckedFunction<Boolean, Boolean> NOT = (Boolean t) -> !t;

    /**
     * Returns true of glob matches provided path
     *
     * @param glob Regex expression
     * @return True for matches
     */
    public static final CheckedBiFunction<ResourceResolver, String, Boolean> filterMatching(final String glob) {
        return (ResourceResolver r, String path) -> path.matches(glob);
    }

    /**
     * Returns false if glob matches provided path Useful for things like
     * filterOutSubassets
     *
     * @param glob Regex expression
     * @return False for matches
     */
    public static final CheckedBiFunction<ResourceResolver, String, Boolean> filterNotMatching(final String glob) {
        return filterMatching(glob).andThen(NOT);
    }

    /**
     * Exclude subassets from processing
     *
     */
    public static final CheckedBiFunction<ResourceResolver, String, Boolean> FILTER_OUT_SUBASSETS = filterNotMatching(".*?/subassets/.*");

    /**
     * Determine if node is a valid asset, skip any non-assets It's better to
     * filter via query if possible to avoid having to use this
     *
     * @return True if asset
     */
    public static final CheckedBiFunction<ResourceResolver, String, Boolean> FILTER_NON_ASSETS = 
        (ResourceResolver r, String path) -> {
            Actions.nameThread("filterNonAssets-" + path);
            Resource res = r.getResource(path);
            return (DamUtil.resolveToAsset(res) != null);
        };

    /**
     * This filter identifies assets where the original rendition is newer than
     * any of the other renditions. This is an especially useful function for
     * updating assets with missing or outdated thumbnails.
     *
     * @return True if asset has no thumbnails or outdated thumbnails
     */
    public static final CheckedBiFunction<ResourceResolver, String, Boolean> FILTER_ASSETS_WITH_OUTDATED_RENDITIONS =
        (ResourceResolver r, String path) -> {
            Actions.nameThread("filterAssetsWithOutdatedRenditions-" + path);
            Resource res = r.getResource(path);
            com.day.cq.dam.api.Asset asset = DamUtil.resolveToAsset(res);
            if (asset == null) {
                return false;
            }
            com.day.cq.dam.api.Rendition original = asset.getRendition(ORIGINAL_RENDITION);
            if (original == null) {
                return false;
            }
            long originalTime = original.getResourceMetadata().getCreationTime();
            int counter = 0;
            for (com.day.cq.dam.api.Rendition rendition : asset.getRenditions()) {
                counter++;
                long time = rendition.getResourceMetadata().getCreationTime();
                if (time < originalTime) {
                    return true;
                }
            }
            return counter <= 1;
        };
    
}
