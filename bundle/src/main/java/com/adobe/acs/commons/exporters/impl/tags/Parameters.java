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
package com.adobe.acs.commons.exporters.impl.tags;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class Parameters {

  private String path;
  private Boolean localized;
  private String defaultLocalization;

  public Parameters(SlingHttpServletRequest request) {
    this.path = Optional.ofNullable(request.getParameter("path"))
        .orElse(StringUtils.EMPTY);
    this.localized = Optional.ofNullable(request.getParameter("localized"))
        .map(Boolean::new)
        .orElse(Boolean.FALSE);
    this.defaultLocalization = Optional.ofNullable(request.getParameter("defaultLocalization"))
        .filter(StringUtils::isNotBlank)
        .orElse("en");
  }

  public String getPath() {
    return path;
  }

  public Boolean isLocalized() {
    return localized;
  }

  public String getDefaultLocalization() {
    return defaultLocalization;
  }

  public boolean containsPath() {
    return StringUtils.isNotBlank(path);
  }
}
