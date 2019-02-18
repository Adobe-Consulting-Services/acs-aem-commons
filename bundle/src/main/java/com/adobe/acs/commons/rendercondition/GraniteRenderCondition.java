package com.adobe.acs.commons.rendercondition;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.api.SlingHttpServletRequest;
import com.adobe.granite.ui.components.Config;

/**
 * This interface is to be implemented by an OSGI service that wishes to register as a
 * Granite Render Condition. The actual render condition^ will then get the implemented service by PID
 * and execute the service's evaluate method to determine if the Granite Widget should be rendered.
 *
 * ^Service Render condition: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
 */
public interface GraniteRenderCondition {

  /**
   *
   * @param slingHttpServletRequest The sling Request you know and love.
   * @param httpServletRequest The Servlet Request
   *        notably used for the method: httpServletRequest#renderedgetPathInfo, which will obtain
   *        the path of the page where the Granite Widget being evaluated is rendered.
   * @param componentConfig Contains all the properties on the granite:rendercondition node.
   * @return true to show the granite widget, false otherwise.
   */
  boolean evaluate(
      SlingHttpServletRequest slingHttpServletRequest,
      HttpServletRequest httpServletRequest,
      Config componentConfig,
      boolean defaultValue);
}
