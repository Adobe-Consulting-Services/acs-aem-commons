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
package com.adobe.acs.commons.wcm.comparisons.model;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLines;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLoader;
import com.adobe.acs.commons.wcm.comparisons.VersionService;
import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@Model(adaptables = SlingHttpServletRequest.class)
public class PageCompareModel {

    private static final Logger log = LoggerFactory.getLogger(PageCompareModel.class);
    public static final String LATEST = "latest";

    private final String pathA;
    private String versionA;
    private final String pathB;
    private String versionB;

    @VisibleForTesting
    @Inject
    ResourceResolver resolver;

    @VisibleForTesting
    @Inject
    PageCompareDataLoader loader;

    @VisibleForTesting
    @Inject
    PageCompareDataLines lines;

    @VisibleForTesting
    @Inject
    VersionService versionService;

    private PageCompareData a;
    private PageCompareData b;

    public PageCompareModel(SlingHttpServletRequest request) {
        this.pathA = request.getParameter("path");
        String paramVersionA = request.getParameter("a");
        this.pathB = request.getParameter("pathB");
        String paramVersionB = request.getParameter("b");

        this.versionA = isNullOrEmpty(paramVersionA) ? LATEST : paramVersionA;
        this.versionB = isNullOrEmpty(paramVersionB) ? LATEST : paramVersionB;
    }

    @PostConstruct
    public void activate() {
        if (pathA == null) {
            return;
        }
        Resource resource = resolver.resolve(pathA);

        improveDefaultVersionCompare(resource);

        this.a = load(resource, versionA);

        Resource resourceB = pathB != null ? resolver.resolve(pathB) : resource;
        this.b = load(resourceB, versionB);
    }

    private void improveDefaultVersionCompare(Resource resource) {
        if (!versionA.equals(LATEST) || !versionA.equals(versionB) || (pathB != null && !pathA.equals(pathB))) {
            return;
        }
        Version version = versionService.lastVersion(resource);
        if (version != null) {
            try {
                versionA = version.getName();
            } catch (RepositoryException e) {
                log.error("error getting version name", e);
            }
        }
    }

    public List<Line<PageCompareDataLine>> getData() {
        if (a != null && b != null) {
            return lines.generate(a.getLines(), b.getLines());
        }
        return Lists.newArrayList();
    }

    public PageCompareData getA() {
        return a;
    }

    public PageCompareData getB() {
        return b;
    }

    public String getPathA() {
        return pathA;
    }

    public String getVersionA() {
        return versionA;
    }

    public String getPathB() {
        return pathB;
    }

    public String getVersionB() {
        return versionB;
    }

    private PageCompareData load(Resource resource, String version) {
        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            try {
                return loader.load(resource, version);
            } catch (RepositoryException e) {
                log.error("Error loading data", e);
            }
        }
        return null;
    }
}
