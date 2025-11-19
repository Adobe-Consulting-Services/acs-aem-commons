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
package com.adobe.acs.commons.wcm.impl;

import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import com.adobe.acs.commons.wcm.AuthorUIHelper;
import com.day.cq.commons.Externalizer;

@Component(description = "Helper service to maintain a central configuration related to which authoring environment is being used (touch vs classic).", metatype = true)
public class AuthorUIHelperImpl implements AuthorUIHelper {

    @Reference
    private Externalizer externalizer;

    /**
     * Default page editor for Touch UI
     */
    private static final String WCM_EDITOR_URL_TOUCH_DEFAULT = "/editor.html";

    /**
     * Default page editor for Classic UI
     */
    private static final String WCM_EDITOR_URL_CLASSIC_DEFAULT = "/cf#";

    /**
     * Default asset editor for Touch UI
     */
    private static final String DAM_EDITOR_URL_TOUCH_DEFAULT = "/assetdetails.html";

    /**
     * Default asset editor for Classic UI
     */
    private static final String DAM_EDITOR_URL_CLASSIC_DEFAULT = "/damadmin#";

    private static final boolean DEFAULT_TOUCH_UI = true;

        private static final String TOUCH_UI = "isTouch";
    private boolean isTouch = DEFAULT_TOUCH_UI;

        public static final String WCM_EDITOR_URL_TOUCH = "wcmEditorTouchURL";
    private String wcmEditorTouchUrl = WCM_EDITOR_URL_TOUCH_DEFAULT;

        public static final String WCM_EDITOR_URL_CLASSIC = "wcmEditorClassicURL";
    private String wcmEditorClassicUrl = WCM_EDITOR_URL_CLASSIC_DEFAULT;

        public static final String DAM_EDITOR_URL_TOUCH = "damEditorTouchURL";
    private String damEditorTouchUrl = DAM_EDITOR_URL_TOUCH_DEFAULT;

        public static final String DAM_EDITOR_URL_CLASSIC = "damEditorClassicURL";
    private String damEditorClassicUrl = DAM_EDITOR_URL_CLASSIC_DEFAULT;

    @Override
    public boolean isTouchUI() {
        return isTouch;
    }

    @Override
    public String generateEditPageLink(String pagePath, boolean absoluteUrl, ResourceResolver resolver) {

        String pageUrl;
        if (isTouch) {
            pageUrl = wcmEditorTouchUrl + pagePath + ".html";
        } else {
            pageUrl = wcmEditorClassicUrl + pagePath + ".html";
        }

        if (absoluteUrl) {
            pageUrl = externalizer.authorLink(resolver, pageUrl);
        }

        return pageUrl;
    }

    @Override
    public String generateEditAssetLink(String assetPath, boolean absoluteUrl, ResourceResolver resolver) {

        String assetUrl;
        if (isTouch) {
            assetUrl = damEditorTouchUrl + assetPath;
        } else {
            assetUrl = damEditorClassicUrl + assetPath;
        }

        if (absoluteUrl) {
            assetUrl = externalizer.authorLink(resolver, assetUrl);
        }

        return assetUrl;
    }

    private void configure(Map<String, String> config) {
        // touch vs classic
        this.isTouch = PropertiesUtil.toBoolean(config.get(TOUCH_UI), DEFAULT_TOUCH_UI);

        // wcm editor configurations
        this.wcmEditorTouchUrl = PropertiesUtil
                .toString(config.get(WCM_EDITOR_URL_TOUCH), WCM_EDITOR_URL_TOUCH_DEFAULT);
        this.wcmEditorClassicUrl = PropertiesUtil.toString(config.get(WCM_EDITOR_URL_CLASSIC),
                WCM_EDITOR_URL_CLASSIC_DEFAULT);

        // dam editor configurations
        this.damEditorTouchUrl = PropertiesUtil
                .toString(config.get(DAM_EDITOR_URL_TOUCH), DAM_EDITOR_URL_TOUCH_DEFAULT);
        this.damEditorClassicUrl = PropertiesUtil.toString(config.get(DAM_EDITOR_URL_CLASSIC),
                DAM_EDITOR_URL_CLASSIC_DEFAULT);
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        configure(config);

    }
}
