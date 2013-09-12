package com.adobe.acs.commons.wcm;

import javax.annotation.CheckForNull;

import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

/**
 * Utility functions for working with CQ Templates.
 */
public class TemplateUtils {

    private TemplateUtils() {
    }

    /**
     * Determine if the page is of a particular template type. This method
     * is null safe and works properly in the publish and author environments.
     * 
     * @param page the page to check
     * @param templatePath the path of the template to check
     * @return true if the page is of the defined template
     */
    public static boolean hasTemplate(@CheckForNull final Page page, @CheckForNull final String templatePath) {
        if (page == null) {
            return false;
        }
        return hasTemplate(page.getProperties(), templatePath);
    }

    private static boolean hasTemplate(@CheckForNull final ValueMap valueMap, @CheckForNull final String templatePath) {
        if (valueMap != null && templatePath != null) {
            String path = valueMap.get(NameConstants.NN_TEMPLATE, String.class);
            if (templatePath.equals(path)) {
                return true;
            }
        }
        return false;
    }

}
