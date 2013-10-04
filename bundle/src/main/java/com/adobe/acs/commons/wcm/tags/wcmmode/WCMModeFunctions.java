/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.wcm.tags.wcmmode;

import javax.servlet.ServletRequest;

import tldgen.Function;

import com.day.cq.wcm.api.WCMMode;

/**
 * JSP function classes from the wcmmode tag namespace.
 */
public final class WCMModeFunctions {

    private WCMModeFunctions() {
    }

    /**
     * Determine if the current WCMMode is design mode.
     * 
     * @param request the current request
     * @return true if the WCMMode is design
     */
    @Function
    public static boolean isDesign(ServletRequest request) {
        return WCMMode.fromRequest(request) == WCMMode.DESIGN;
    }

    /**
     * Determine if the current WCMMode is disabled mode.
     * 
     * @param request the current request
     * @return true if the WCMMode is disabled
     */
    @Function
    public static boolean isDisabled(ServletRequest request) {
        return WCMMode.fromRequest(request) == WCMMode.DISABLED;
    }

    /**
     * Determine if the current WCMMode is edit mode.
     * 
     * @param request the current request
     * @return true if the WCMMode is edit
     */
    @Function
    public static boolean isEdit(ServletRequest request) {
        return WCMMode.fromRequest(request) == WCMMode.EDIT;
    }

    /**
     * Determine if the current WCMMode is preview mode.
     * 
     * @param request the current request
     * @return true if the WCMMode is preview
     */
    @Function
    public static boolean isPreview(ServletRequest request) {
        return WCMMode.fromRequest(request) == WCMMode.PREVIEW;
    }

}
