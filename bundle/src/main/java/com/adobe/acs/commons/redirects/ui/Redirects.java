package com.adobe.acs.commons.redirects.ui;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Model(adaptables = SlingHttpServletRequest.class)
public class Redirects {
    @SlingObject
    private SlingHttpServletRequest request;
    @OSGiService
    private RedirectFilterMBean redirectFilter;

    private static final String REDIRECTS_RESOURCE_TYPE = "acs-commons/components/utilities/manage-redirects/redirects";

    public Collection<CaConfig> getConfigurations() {
        String sql = "SELECT * FROM [nt:unstructured] AS s WHERE ISDESCENDANTNODE([/conf]) "
                + "AND s.[sling:resourceType]='" + REDIRECTS_RESOURCE_TYPE + "'";
        Iterator<Resource> it = request.getResourceResolver().findResources(sql, Query.JCR_SQL2);
        Collection<CaConfig> lst = new ArrayList<>();
        String suffix = "/" + redirectFilter.getBucket() + "/" + redirectFilter.getConfigName();
        while (it.hasNext()) {
            String path = it.next().getPath();
            String name = path.replace(suffix, "");
            lst.add(new CaConfig(path, name));
        }
        return lst;
    }
}
