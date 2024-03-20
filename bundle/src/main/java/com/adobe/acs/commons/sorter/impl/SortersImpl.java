/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.sorter.impl;

import com.adobe.acs.commons.sorter.NodeSorter;
import com.adobe.acs.commons.sorter.Sorters;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Model(adaptables = { SlingHttpServletRequest.class },
        adapters = { Sorters.class })
public class SortersImpl implements Sorters {

    @SlingObject
    private SlingScriptHelper slingScriptHelper;

    public Collection<NodeSorter> getAvailableSorters() {
        NodeSorter[] sorters = slingScriptHelper.getServices(NodeSorter.class, null);
        return sorters == null ? Collections.emptyList() : Arrays.asList(sorters);
    }
}
