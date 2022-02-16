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
