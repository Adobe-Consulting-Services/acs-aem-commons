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
package com.adobe.acs.commons.redirects.models;

import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model for paginated output on http://localhost:4502/apps/acs-commons/content/redirect-manager.html
 * 
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class Redirects {
    @SlingObject
    private SlingHttpServletRequest request;

    int pageNumber = 1;
    int pageSize = 1000;
    List<List<Resource>> pages;


    @PostConstruct
    protected void init() {
        String pg = request.getParameter("page");
        if (pg != null) {
            pageNumber = Integer.parseInt(pg);
        }
        Resource configResource = request.getRequestPathInfo().getSuffixResource();
        List<Resource> all = new ArrayList<>();
        configResource.listChildren().forEachRemaining(all::add);
        pages = Lists.partition(all, pageSize);
    }

    public List<Resource> getItems() {
        return pages.isEmpty() ? Collections.emptyList() : pages.get(pageNumber - 1);
    }

    public boolean isPaginated() {
        return pages.size() > 1;
    }

    public int getPages() {
        return pages.size();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public boolean hasNext() {
        return pageNumber < pages.size();
    }

    public int getNextPage() {
        return pageNumber + 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    public int getPreviousPage() {
        return pageNumber - 1;
    }
}
