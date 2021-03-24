/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.ui;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
        List<CaConfig> lst = new ArrayList<>();
        String suffix = "/" + redirectFilter.getBucket() + "/" + redirectFilter.getConfigName();
        while (it.hasNext()) {
            String path = it.next().getPath();
            String name = path.replace(suffix, "");
            lst.add(new CaConfig(path, name));
        }
        lst.sort(Comparator.comparing(o -> o.name));
        return lst;
    }
}
