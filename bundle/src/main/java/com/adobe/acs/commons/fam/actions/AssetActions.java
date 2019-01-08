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

import static com.adobe.acs.commons.fam.actions.Actions.nameThread;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedBiFunction;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.Rendition;
import java.util.Iterator;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Assets utility functions.
 */
@ProviderType
public class AssetActions {
    private AssetActions() {
        // Utility class cannot be instantiated directly.
    }

    @SuppressWarnings("squid:S3776")
    public static final CheckedBiConsumer<ResourceResolver, String> withAllRenditions(
            final CheckedBiConsumer<ResourceResolver, String> action,
            final CheckedBiFunction<ResourceResolver, String, Boolean>... filters) {
        return (ResourceResolver r, String path) -> {
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                boolean skip = false;
                if (filters != null) {
                    for (CheckedBiFunction<ResourceResolver, String, Boolean> filter : filters) {
                        if (!filter.apply(r, rendition.getPath())) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (!skip) {
                    action.accept(r, path);
                }
            }
        };
    }

    /**
     * Remove all renditions except for the original rendition for assets
     *
     */
    public static final CheckedBiConsumer<ResourceResolver, String> REMOVE_ALL_RENDITIONS =
        (ResourceResolver r, String path) -> {
            nameThread("removeRenditions-" + path);
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                if (!rendition.getName().equalsIgnoreCase("original")) {
                    asset.removeRendition(rendition.getName());
                }
            }
        };

    /**
     * Remove all renditions with a given name
     *
     */
    public static final CheckedBiConsumer<ResourceResolver, String> removeAllRenditionsNamed(final String name) {
        return (ResourceResolver r, String path) -> {
            nameThread("removeRenditions-" + path);
            AssetManager assetManager = r.adaptTo(AssetManager.class);
            Asset asset = assetManager.getAsset(path);
            for (Iterator<? extends Rendition> renditions = asset.listRenditions(); renditions.hasNext();) {
                Rendition rendition = renditions.next();
                if (rendition.getName().equalsIgnoreCase(name)) {
                    asset.removeRendition(rendition.getName());
                }
            }
        };
    }

    /**
     * Remove all non-original renditions from an asset.
     *
     * @param path
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> removeRenditions(String path) {
        return res -> REMOVE_ALL_RENDITIONS.accept(res, path);
    }

    /**
     * Remove all renditions with a given name
     *
     * @param path
     * @param name
     * @return
     */
    public static final CheckedConsumer<ResourceResolver> removeRenditionsNamed(String path, String name) {
        return res -> removeAllRenditionsNamed(name).accept(res, path);
    }    
}
