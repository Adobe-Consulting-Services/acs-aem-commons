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
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/**
 * Model for rendering the tags for a report item.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class TagsCellValue {

  private static final Logger log = LoggerFactory.getLogger(TagsCellValue.class);

  @Self
  private SlingHttpServletRequest request;

  @ValueMapValue
  private String property;

  public List<Tag> getTags() {

    TagManager tagMgr = request.getResourceResolver().adaptTo(TagManager.class);

    Resource resource = (Resource) request.getAttribute("result");

    log.debug("Loading tags from {}@{}", resource.getPath(), property);
    List<Tag> tags = new ArrayList<Tag>();
    String[] values = resource.getValueMap().get(property, String[].class);
    if (values != null) {
      for (String value : values) {
        tags.add(tagMgr.resolve(value));
      }
    }
    log.debug("Loaded {} tags", tags.size());

    return tags;

  }
}
