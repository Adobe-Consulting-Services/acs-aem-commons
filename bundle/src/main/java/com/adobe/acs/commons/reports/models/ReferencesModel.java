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
package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.internal.DelimiterConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.granite.references.Reference;
import com.adobe.granite.references.ReferenceAggregator;
import com.adobe.granite.references.ReferenceList;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = Resource.class)
public class ReferencesModel implements ReportCellCSVExporter {
    private static final Logger log = LoggerFactory.getLogger(ReferencesModel.class);

    @OSGiService
    private ReferenceAggregator aggregator;

    @OSGiService
    private DelimiterConfiguration delimiterConfiguration;

    private ReferenceList referenceList;

    private Resource resource;

    public ReferencesModel(Resource resource) {
        this.resource = resource;
    }

    /**
     * Used only for testing.
     *
     * @param delimiterConfiguration the delimiter configuration to use for this exporter
     */
    ReferencesModel(Resource resource, DelimiterConfiguration delimiterConfiguration) {
        this.resource = resource;
        this.delimiterConfiguration = delimiterConfiguration;
    }

    public List<Reference> getReferences() {
        return Optional.ofNullable(referenceList)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
    }

    @Override
    public String getValue(Object result) {
        resource = (Resource) result;
        init();
        List<String> refStrings = new ArrayList<>();
        for (Reference reference : referenceList) {
            refStrings.add(reference.getType() + " - " + reference.getTarget().getPath());
        }
        return StringUtils.join(refStrings, delimiterConfiguration.getMultiValueDelimiter());
    }

    @PostConstruct
    public void init() {
        if (resource == null) {
            throw new IllegalStateException("Resource is null, and must must be set before calling init()");
        }

        referenceList = aggregator.createReferenceList(resource);
        Iterator<Reference> references = referenceList.iterator();

        while (references.hasNext()) {
            Reference reference = references.next();

            if (reference == null) {
                log.warn("Reference is null for resource: {}", resource.getPath());
                continue;
            }

            Resource target = reference.getTarget();
            if (target == null) {
                log.warn("Reference target is null for resource: {}", resource.getPath());
                continue;
            }

            String targetPath = target.getPath();
            if (StringUtils.isBlank(targetPath)) {
                log.warn("Reference target path is blank for resource: {}", resource.getPath());
                continue;
            }

            if (StringUtils.equals(targetPath, resource.getPath())) {
                references.remove();
            }
        }
    }
}
