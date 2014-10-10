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

import org.apache.commons.lang.StringUtils;

import tldgen.Function;
import aQute.bnd.annotation.ProviderType;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

/**
 * DAM JSP functions.
 */
@ProviderType
public final class DAMFunctions {

    private DAMFunctions() {
    }

    /**
     * Return the title or name of the asset, if the title is not defined.
     * 
     * @param asset the asset
     * @return the asset title or name
     */
    @Function
    public static String getTitleOrName(Asset asset) {
        String title = asset.getMetadataValue(DamConstants.DC_TITLE);
        return StringUtils.isNotBlank(title) ? title : asset.getName();
    }

}
