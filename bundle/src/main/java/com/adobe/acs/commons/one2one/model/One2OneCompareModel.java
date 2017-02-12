/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.one2one.model;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class)
public class One2OneCompareModel {

    private static final Logger log = LoggerFactory.getLogger(One2OneCompareModel.class);

    @VisibleForTesting
    @Inject
    ResourceResolver resolver;

    @VisibleForTesting
    @Inject
    EvolutionAnalyser analyser;

    private final String pathA;
    private final String versionA;
    private final String pathB;
    private final String versionB;

    @VisibleForTesting
    FluentIterable<Evolution> evolutionsA;

    @VisibleForTesting
    FluentIterable<Evolution> evolutionsB;

    public One2OneCompareModel(SlingHttpServletRequest request) {
        this.pathA = request.getParameter("path");
        this.pathB = request.getParameter("pathB");
        this.versionA = request.getParameter("a");
        this.versionB = request.getParameter("b");
    }

    @PostConstruct
    protected void activate() {
        this.evolutionsA = FluentIterable.from(evolutions(pathA));
        this.evolutionsB = pathB != null ? FluentIterable.from(evolutions(pathB)) : this.evolutionsA;
    }

    public String getResourcePathA() {
        return pathA;
    }

    public String getResourcePathB() {
        return Optional.fromNullable(pathB).or("");
    }

    public List<VersionSelection> getVersionSelectionsA() {
        return evolutionsA.transform(TO_VERSION_SELECTION).toList();
    }

    public List<VersionSelection> getVersionSelectionsB() {
        return evolutionsB.transform(TO_VERSION_SELECTION).toList();
    }

    public Evolution getEvolutionA() {
        return version(evolutionsA, getVersionA()).orNull();
    }

    public Evolution getEvolutionB() {
        return version(evolutionsB, getVersionB()).orNull();
    }

    public String getVersionA() {
        if (versionA == null && evolutionsA.size() >= 2) {
            return evolutionsA.get(evolutionsA.size() - 2).getVersionName();
        }
        if (versionA == null) {
            return "";
        }
        return versionA;
    }

    public String getVersionB() {
        if (versionB == null && !evolutionsB.isEmpty()) {
            return evolutionsB.last().get().getVersionName();
        }
        if (versionB == null) {
            return "";
        }
        return versionB;
    }


    private Optional<Evolution> version(FluentIterable<Evolution> evolutions, final String name) {
        return evolutions.firstMatch(new Predicate<Evolution>() {
            @Override
            public boolean apply(Evolution evolution) {
                return evolution.getVersionName().equalsIgnoreCase(name);
            }
        });
    }

    private List<Evolution> evolutions(String path) {
        if (StringUtils.isNotEmpty(path)) {
            Resource resource = resolver.resolve(path);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                return analyser.getEvolutionContext(resource).getEvolutionItems();
            }
            log.warn("Could not resolve resource at path={}", path);
        }
        log.warn("No path provided");
        return Collections.emptyList();
    }

    private static final Function<Evolution, String> TO_NAME = new Function<Evolution, String>() {
        @Override
        public String apply(Evolution evolution) {
            if (evolution == null) {
                return null;
            }
            return evolution.getVersionName();
        }
    };

    private static final Function<Evolution, VersionSelection> TO_VERSION_SELECTION = new Function<Evolution, VersionSelection>() {
        @Override
        public VersionSelection apply(Evolution evolution) {
            if (evolution == null) {
                return null;
            }
            return new VersionSelection(evolution.getVersionName(), evolution.getVersionDate());
        }
    };

}
