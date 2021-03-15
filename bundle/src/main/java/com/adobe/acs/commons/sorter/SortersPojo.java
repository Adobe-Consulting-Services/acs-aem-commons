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
package com.adobe.acs.commons.sorter;

import com.adobe.cq.sightly.WCMUsePojo;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Produce a list of available sorters to populate the drop-down on the sort-nodes.html page.
 */
@ProviderType
public class SortersPojo extends WCMUsePojo {

    @Override
    public void activate(){
        // no op
    }

    public Collection<NodeSorter> getAvailableSorters(){
        NodeSorter[] sorters = getSlingScriptHelper().getServices(NodeSorter.class, null);
        return sorters == null ? Collections.emptyList() : Arrays.asList(sorters);
    }

}
