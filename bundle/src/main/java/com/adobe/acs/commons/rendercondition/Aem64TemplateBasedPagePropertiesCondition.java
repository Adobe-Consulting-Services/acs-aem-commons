package com.adobe.acs.commons.rendercondition;

import com.adobe.granite.ui.components.Config;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders page properties Granite Widget based on the current page template.
 * Assumes that page properties console is under path: /wcm/core/content/sites/properties
 * and that page path is passed via a request param called "item".
 * For example, if you open page properties for page: /content/my-company/my-page, the URL would be:
 * http://localhost:4502/mnt/overlay/wcm/core/content/sites/properties.html?item=/content/my-company/my-page
 *
 * Sample render:condition node:
 *
 * granite:rendercondition
 *   - jcr:primaryType: "nt:unstructured",
 *   - templatePath: "/conf/my-com/settings/wcm/templates/community-page",
 *   - condition.name: "acs-template-based-page-properties-condition",
 *   - sling:resourceType: "/apps/acs-commons/granite/ui/components/renderconditions/service"
 *
 */

@Component(
    immediate = true,
    property = {
        "label=AEM 6.4 Template Based Page Properties Render Condition",
        "description=Renders Granite Widgets in Page Properties if page template matches tha passed template path",
        GraniteRenderConditionConstants.CONDITION_NAME + "=acs-template-based-page-properties-condition"
    },
    service = GraniteRenderCondition.class)
public class Aem64TemplateBasedPagePropertiesCondition implements GraniteRenderCondition {

  private static final Logger log = LoggerFactory.getLogger(Aem64TemplateBasedPagePropertiesCondition.class);
  private static final String PAGE_PROPERTIES_CONSOLE = "wcm/core/content/sites/properties";

  @Override
  public boolean evaluate(
      SlingHttpServletRequest slingHttpServletRequest,
      HttpServletRequest httpServletRequest,
      Config cfg,
      boolean defaultValue) {

    String expectedTemplatePath = cfg.get("templatePath", "");


    if (slingHttpServletRequest == null
        || httpServletRequest == null
        || StringUtils.isBlank(expectedTemplatePath)) {
      throw new IllegalArgumentException("One of the passed parameters is null.");
    }

    // not in page properties console, exit.
    if (!StringUtils.contains(httpServletRequest.getPathInfo(), PAGE_PROPERTIES_CONSOLE)) {
      return defaultValue;
    }

    // make sure page path exists
    String pagePath = httpServletRequest.getParameter("item");
    if (StringUtils.isBlank(pagePath)) {
      log.warn("item parameter is blank, returning default value, {}", defaultValue);
      return defaultValue;
    }

    return Optional.ofNullable(slingHttpServletRequest)
        .map(SlingHttpServletRequest::getResourceResolver)
        .map(resolver -> resolver.getResource(pagePath))
        .map(pageResource -> pageResource.adaptTo(Page.class))
        .map(Page::getTemplate)
        .map(Template::getPath)
        .map(templatePath -> StringUtils.contains(templatePath, expectedTemplatePath))
        .orElse(defaultValue);
  }
}
