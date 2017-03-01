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
package com.adobe.acs.commons.wcm.comparisons.model;

import com.adobe.acs.commons.wcm.comparisons.One2OneData;
import com.adobe.acs.commons.wcm.comparisons.One2OneDataLine;
import com.adobe.acs.commons.wcm.comparisons.One2OneDataLoader;
import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.adobe.acs.commons.wcm.comparisons.lines.Lines;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@Model(adaptables = SlingHttpServletRequest.class)
public class One2OneCompareModel {

    private static final Logger log = LoggerFactory.getLogger(One2OneCompareModel.class);

    private final String pathA;
    private final String versionA;
    private final String pathB;
    private final String versionB;

    @VisibleForTesting
    @Inject
    ResourceResolver resolver;

    @VisibleForTesting
    @Inject
    One2OneDataLoader loader;

    private One2OneData a;
    private One2OneData b;

    public One2OneCompareModel(SlingHttpServletRequest request) {
        this.pathA = request.getParameter("path");
        String versionA = request.getParameter("a");
        this.pathB = request.getParameter("pathB");
        String versionB = request.getParameter("b");

        this.versionA = isNullOrEmpty(versionA) ? "latest" : versionA;
        this.versionB = isNullOrEmpty(versionB) ? "latest" : versionB;
    }

    @PostConstruct
    public void activate() {
        if (pathA == null) {
            return;
        }
        Resource resource = resolver.resolve(pathA);
        this.a = load(resource, getVersionA());

        Resource resourceB = pathB != null ? resolver.resolve(pathB) : resource;
        this.b = load(resourceB, getVersionB());
    }

    public List<Line<One2OneDataLine>> getData() {
        Lines<One2OneDataLine> lines = new Lines<One2OneDataLine>(new Function<One2OneDataLine, Serializable>() {
            @Nullable
            @Override
            public Serializable apply(@Nullable One2OneDataLine input) {
                return input.getUniqueName();
            }
        });
        if (a != null && b != null) {
            return lines.generate(a.getLines(), b.getLines());
        }
        return Lists.newArrayList();
    }

    public One2OneData getA() {
        return a;
    }

    public One2OneData getB() {
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

    private One2OneData load(Resource resource, String version) {
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
