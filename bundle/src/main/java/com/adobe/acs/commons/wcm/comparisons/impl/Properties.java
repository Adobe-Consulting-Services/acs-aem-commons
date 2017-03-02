/*
 *
 *  * #%L
 *  * ACS AEM Commons Bundle
 *  * %%
 *  * Copyright (C) 2016 Adobe
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 *
 */
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.day.cq.wcm.api.NameConstants;
import org.apache.sling.api.resource.Resource;

import java.util.Date;

public class Properties {

    public static Date lastModified(Resource resource) {
        if (resource != null) {
            return resource.getValueMap().get(NameConstants.PN_LAST_MOD, new Date());
        }
        return new Date();
    }
}
