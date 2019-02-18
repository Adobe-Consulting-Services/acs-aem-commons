package com.adobe.acs.commons.rendercondition;

import com.adobe.granite.ui.components.ComponentHelper;
import com.adobe.granite.ui.components.Config;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called here: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
 * Does the heavy lifting for the Render Condition feature. instead of putting all this code in a JSP.
 */
public class GraniteRenderConditionEvaluator {

  private static final Logger log = LoggerFactory.getLogger(GraniteRenderConditionEvaluator.class);

  /**
   * All params passed from the JSP in which this method is called.
   * Tries to get the intended GraniteRenderCondition service and call its evaluate method.
   * see: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
   */
  public static boolean evaluate(
      SlingHttpServletRequest slingHttpServletRequest,
      HttpServletRequest httpServletRequest,
      SlingScriptHelper slingScriptHelper,
      PageContext pageContext) {

    final ComponentHelper cmp = new ComponentHelper(pageContext);
    Config cfg = cmp.getConfig();

    // get properties on the render condition
    String servicePID = cfg.get(GraniteRenderCondition.CONDITION_NAME, null);
    boolean defaultValue = cfg.get("default", false);

    if (servicePID != null) {
      GraniteRenderCondition condition = getRenderConditionService(slingScriptHelper, servicePID);
      if (condition != null) {
        return condition.evaluate(slingHttpServletRequest, httpServletRequest, cfg, defaultValue);
      } else {
        log.error("Could not find service with PID: {}", servicePID);
      }
    } else {
      log.error("service.pid cannot be empty for an ACS Commons Service Render Condition. Returning default value: {}", defaultValue);
    }
    // return default value
    return defaultValue;
  }

  /**
   * Obtains a GraniteRenderCondition service filtered by passed servicePID string.
   */
  private static GraniteRenderCondition getRenderConditionService(
      SlingScriptHelper slingScriptHelper,
      @Nonnull String servicePID) {

    String filter = "(" + GraniteRenderCondition.CONDITION_NAME +"="+ servicePID +")";
    GraniteRenderCondition[] services = slingScriptHelper.getServices(GraniteRenderCondition.class, filter);
    if (ArrayUtils.isNotEmpty(services)) {
      return services[0];
    } else {
      return null;
    }
  }
}
