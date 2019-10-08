package com.adobe.acs.commons.exporters.impl.tags;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class Parameters {

  private String path;
  private Boolean localized;
  private String defaultLocalization;

  public Parameters (SlingHttpServletRequest request) {
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
    return !path.isEmpty();
  }
}
