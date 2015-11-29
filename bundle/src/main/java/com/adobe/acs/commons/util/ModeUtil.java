/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.WCMMode;

import aQute.bnd.annotation.ProviderType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ProviderType
@Component(immediate = true)
@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public final class ModeUtil {

    private static Logger LOG = LoggerFactory.getLogger(ModeUtil.class);

    private static Boolean activated = false;

    private static boolean isAuthor = false;

    private static boolean isPublish = false;

    private static Object lock = new Object();

    private static Set<String> runmodes = new HashSet<String>();

    @Reference
    private SlingSettingsService slingSettings;

    private static void waitForActivation() {
        synchronized (lock) {
            while (!activated) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    LOG.error("Interuppted while waiting for lock.", ex);
                }
            }
        }
    }

    /**
     * Is AEM runmode author.
     * 
     * @return true if runmode author is present
     */
    public static boolean isAuthor() {
        waitForActivation();
        return isAuthor;
    }

    /**
     * Is AEM runmode publish.
     * 
     * @return true if runmode publish is present
     */
    public static boolean isPublish() {
        waitForActivation();
        return isPublish;
    }

    /**
     * Helper method to check for given runmode.
     * 
     * @param mode
     *            the mode to check
     * @return true if the specified mode is present
     */
    public static boolean isRunmode(String mode) {
        waitForActivation();
        return runmodes.contains(mode);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#ANALYTICS}
     * 
     * @param request
     *            request to check
     * @return true if the request is in analytics mode
     */
    public static boolean isAnalytics(SlingHttpServletRequest request) {
        return WCMMode.ANALYTICS == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#DESIGN}
     * 
     * @param request
     *            request to check
     * @return true if the request is in design mode
     */
    public static boolean isDesign(SlingHttpServletRequest request) {
        return WCMMode.DESIGN == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#DISABLED}
     * 
     * @param request
     *            request to check
     * @return true if the request is in disabled mode
     */
    public static boolean isDisabled(SlingHttpServletRequest request) {
        return WCMMode.DISABLED == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#EDIT}
     * 
     * @param request
     *            request to check
     * @return true if the request is in edit mode
     */
    public static boolean isEdit(SlingHttpServletRequest request) {
        return WCMMode.EDIT == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#PREVIEW}
     * 
     * @param request
     *            request to check
     * @return true if the request is in preview mode
     */
    public static boolean isPreview(SlingHttpServletRequest request) {
        return WCMMode.PREVIEW == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in mode {@link WCMMode#READ_ONLY}
     * 
     * @param request
     *            request to check
     * @return true if the request is in read-only mode
     */
    public static boolean isReadOnly(SlingHttpServletRequest request) {
        return WCMMode.READ_ONLY == WCMMode.fromRequest(request);
    }

    /**
     * Checks if the request is in {@link AuthoringUIMode#CLASSIC}
     * 
     * @param request
     *            request to check
     * @return true if the request is in Classic authoring mode
     */
    public static boolean isClassic(SlingHttpServletRequest request) {
        return AuthoringUIMode.CLASSIC == AuthoringUIMode.fromRequest(request);
    }

    /**
     * Checks if the request is in {@link AuthoringUIMode#TOUCH}
     * 
     * @param request
     *            request to check
     * @return true if the request is in Touch authoring mode
     */
    public static boolean isTouch(SlingHttpServletRequest request) {
        return AuthoringUIMode.TOUCH == AuthoringUIMode.fromRequest(request);
    }

    @Activate
    protected void activate(ComponentContext componentContext) throws Exception {

        synchronized (lock) {
            runmodes = slingSettings.getRunModes();
            isAuthor = runmodes.contains(Externalizer.AUTHOR);
            isPublish = runmodes.contains(Externalizer.PUBLISH);
            if (isAuthor && isPublish) {
                throw new ConfigurationException(null,
                        "Either 'author' or 'publish' run modes may be specified, not both.");
            }
            activated = true;
            lock.notify();
        }
    }

}
