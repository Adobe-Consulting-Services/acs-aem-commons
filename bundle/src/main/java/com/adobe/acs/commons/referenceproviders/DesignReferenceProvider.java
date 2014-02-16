package com.adobe.acs.commons.referenceproviders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

@Component
@Service(ReferenceProvider.class)
public class DesignReferenceProvider implements ReferenceProvider {

    private static final String TYPE_DESIGN_PAGE = "designpage";
    private static final String PROP_DESIGN_PATH = "cq:designPath";

    @Override
    public List<Reference> findReferences(Resource resource) {
        String designPath = getDesignPath(resource);
        if (null == designPath)
            return Collections.emptyList();
        Resource designResource = resource.getResourceResolver().getResource(designPath);
        Page designPage = designResource.adaptTo(Page.class);
        List<Reference> references = new ArrayList<Reference>(1);
        references.add(new Reference(TYPE_DESIGN_PAGE, designResource.getName(), designResource,
                getLastModifiedOfResource(designPage)));
        return references;
    }

    private long getLastModifiedOfResource(Page page) {
        final Calendar mod = page.getLastModified();
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        return lastModified;
    }
    private String getDesignPath(Resource resource){
        HierarchyNodeInheritanceValueMap hnvm = new HierarchyNodeInheritanceValueMap(
                resource);
        return hnvm.getInherited(PROP_DESIGN_PATH, null);
    }
}
