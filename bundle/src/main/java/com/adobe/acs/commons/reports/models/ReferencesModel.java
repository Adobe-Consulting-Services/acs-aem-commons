/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.granite.references.Reference;
import com.adobe.granite.references.ReferenceAggregator;
import com.adobe.granite.references.ReferenceList;

@Model(adaptables = Resource.class)
public class ReferencesModel implements ReportCellCSVExporter {

  @Inject
  @OSGiService
  private ReferenceAggregator aggregator;

  private ReferenceList referenceList;

  private Resource resource;

  public ReferencesModel(Resource resource) {
    this.resource = resource;
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
    return StringUtils.join(refStrings, ";");
  }

  @PostConstruct
  public void init() {
    referenceList = aggregator.createReferenceList(resource);
    Iterator<Reference> references = referenceList.iterator();
    while (references.hasNext()) {
      if (references.next().getTarget().getPath().equals(resource.getPath())) {
        references.remove();
      }
    }
  }
}
