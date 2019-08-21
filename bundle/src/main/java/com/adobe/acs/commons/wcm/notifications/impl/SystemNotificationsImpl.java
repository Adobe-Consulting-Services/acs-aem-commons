/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.wcm.notifications.impl;

import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.http.injectors.AbstractHtmlRequestInjector;
import com.adobe.acs.commons.util.CookieUtil;
import com.adobe.acs.commons.wcm.notifications.SystemNotifications;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(immediate = true)
@Service(value = SystemNotifications.class)
public class SystemNotificationsImpl extends AbstractHtmlRequestInjector implements SystemNotifications, ResourceChangeListener, ExternalResourceChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SystemNotificationsImpl.class);

    public static final String COOKIE_NAME = "acs-commons-system-notifications";

    private static final String PATH_NOTIFICATIONS = "/etc/acs-commons/notifications";

    private static final String PN_ON_TIME = "onTime";

    private static final String PN_OFF_TIME = "offTime";

    private static final String PN_ENABLED = "enabled";

    private static final String INJECT_TEXT =
            "<script>"
                    + "if(window === top) {"
                    + "   window.jQuery || document.write('<script src=\"%s\"><\\/script>');"
                    + "   document.write('<script src=\"%s\"><\\/script>');"
                    + "}"
                    + "</script>";

    private static final String SERVICE_NAME = "system-notifications";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private AtomicBoolean isFilter = new AtomicBoolean(false);

    private ComponentContext osgiComponentContext;

    private ServiceRegistration<ResourceChangeListener> eventHandlerRegistration;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingSettingsService slingSettings;

    @Override
    protected void inject(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter printWriter) {
        final String jquerySrc = servletRequest.getContextPath()
                + "/etc/clientlibs/granite/jquery.js";
        final String notificationsSrc = servletRequest.getContextPath()
                + "/apps/acs-commons/components/utilities/system-notifications/notification/clientlibs.js";

        printWriter.println(String.format(INJECT_TEXT, jquerySrc, notificationsSrc));
    }

    @Override
    protected boolean accepts(final ServletRequest servletRequest,
                              final ServletResponse servletResponse) {

        if (!(servletRequest instanceof SlingHttpServletRequest)
                || !(servletResponse instanceof SlingHttpServletResponse)) {
            return false;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;

        if (StringUtils.startsWith(slingRequest.getResource().getPath(), PATH_NOTIFICATIONS)) {
            // Do NOT inject on the notifications Authoring pages
            return false;
        }

        final Resource notificationsFolder = slingRequest.getResourceResolver().getResource(PATH_NOTIFICATIONS);
        if (notificationsFolder == null || this.getNotifications(slingRequest, notificationsFolder).size() < 1) {
            // If no notifications folder or no active notifications; do not inject JS
            return false;
        }

        return super.accepts(servletRequest, servletResponse);
    }

    @Override
    protected int getInjectIndex(String originalContents) {
        // Inject immediately before the ending body tag
        return StringUtils.indexOf(originalContents, "</body>");
    }

    @Override
    public List<Resource> getNotifications(final SlingHttpServletRequest request,
                                           final Resource notificationsFolder) {

        final ActiveNotificationsResourceVisitor visitor = new ActiveNotificationsResourceVisitor(request);
        visitor.accept(notificationsFolder);
        return visitor.getNotifications();
    }

    @Override
    @SuppressWarnings("squid:S2070") // SHA1 not used cryptographically
    public String getNotificationId(final Page notificationPage) {
        final String path = notificationPage.getPath();
        final String lastModified = String.valueOf(notificationPage.getLastModified().getTimeInMillis());
        return "uid-" + DigestUtils.sha1Hex(path + lastModified);
    }

    @Override
    public String getMessage(String message, String onTime, String offTime) {
        if (StringUtils.isBlank(message)) {
            return message;
        }

        message = StringUtils.trimToEmpty(message);

        boolean allowHTML = false;
        if (StringUtils.startsWith(message, "html:")) {
            allowHTML = true;
            message = StringUtils.removeStart(message, "html:");
        }

        if (onTime != null) {
            message = StringUtils.replace(message, "{{ onTime }}", onTime);
        }

        if (offTime != null) {
            message = StringUtils.replace(message, "{{ offTime }}", offTime);
        }

        if (!allowHTML) {
            message = message.replaceAll("(\r\n|\n)", "<br />");
        }

        return message;
    }

    private boolean hasNotifications() {

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {

            final Resource notificationsFolder = resourceResolver.getResource(PATH_NOTIFICATIONS);
            final NotificationsResourceVisitor visitor = new NotificationsResourceVisitor();

            visitor.accept(notificationsFolder);

            return visitor.hasNotifications();
        } catch (LoginException e) {
            log.error("Could not get an service ResourceResolver", e);
        }

        return false;
    }

    private void registerAsFilter() {
            super.registerAsSlingFilter(this.osgiComponentContext, 0, ".*");
            log.debug("Registered System Notifications as Sling Filter");
    }

    @SuppressWarnings("squid:S1149")
    private void registerAsEventHandler() {
            final Hashtable<String, Object> filterProps = new Hashtable<>();

            // Listen on Add and Remove under /etc/acs-commons/notifications
            filterProps.put(ResourceChangeListener.CHANGES,
                    new String[]{
                            ResourceChange.ChangeType.ADDED.name(),
                            ResourceChange.ChangeType.REMOVED.name() });

            filterProps.put(ResourceChangeListener.PATHS, new String[]{ SystemNotificationsImpl.PATH_NOTIFICATIONS });

            this.eventHandlerRegistration =
                    this.osgiComponentContext.getBundleContext().registerService(ResourceChangeListener.class, this,
                            filterProps);

            log.debug("Registered System Notifications as Resource Change Listener");
    }

    @Override
    public void onChange(final List<ResourceChange> changes) {
        final long start = System.currentTimeMillis();

        if (!this.isAuthor()) {
            log.warn("This change listener should ONLY run on AEM Author.");
            return;
        }

        /** The following code will ONLY execute on AEM Author **/

        if (this.hasNotifications()) {
            if (!this.isFilter.getAndSet(true)) {
                this.registerAsFilter();
            }
        } else {
            if (this.isFilter.getAndSet(false)) {
                this.unregisterFilter();
                log.debug("Unregistered System Notifications Sling Filter");
            }
        }

        if (System.currentTimeMillis() - start > 2500) {
            log.warn("Change handling for System notifications took [ {} ] ms.",
                    System.currentTimeMillis() - start);
        }
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        this.osgiComponentContext = ctx;

        if (this.isAuthor()) {
            this.registerAsEventHandler();

            if (this.hasNotifications()) {
                this.isFilter.set(true);
                this.registerAsFilter();
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);

        // Unregister the event handler is was registered
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }

        this.osgiComponentContext = null;
    }

    private boolean isAuthor() {
        return slingSettings.getRunModes().contains("author");
    }

    private class NotificationsResourceVisitor extends AbstractResourceVisitor {

        boolean hasNotifications;

        public boolean hasNotifications() {
            return hasNotifications;
        }

        @Override
        protected void visit(Resource resource) {
            hasNotifications = hasNotifications || isNotification(resource);
        }

        protected boolean isNotification(Resource resource) {
            // Treat any cq:Page as a notification
            return !PATH_NOTIFICATIONS.equals(resource.getPath())
                && NT_PAGE.equals(resource.getValueMap().get(JCR_PRIMARYTYPE));
        }
    }


    private class ActiveNotificationsResourceVisitor extends NotificationsResourceVisitor {

        private final List<Resource> notifications = new ArrayList<Resource>();
        private SlingHttpServletRequest request;

        public ActiveNotificationsResourceVisitor(SlingHttpServletRequest request) {
            this.request = request;
        }

        public List<Resource> getNotifications() {
            return notifications;
        }

        @Override
        protected void visit(Resource resource) {
            if (isActiveNotification(request, resource)) {
                notifications.add(resource);
            }
        }

        private boolean isActiveNotification(final SlingHttpServletRequest request, final Resource resource) {
            if (!isNotification(resource)) {
                return false;
            }

            final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
            final Page notificationPage = pageManager.getContainingPage(resource);

            if (notificationPage == null) {
                log.warn("Trying to get a invalid System Notification page at [ {} ]", resource.getPath());
                return false;
            } else if (this.isDismissed(request, notificationPage)) {
                // System Notification previously dismissed by the user
                return false;
            }

            // Looks like a valid Notification Page; now check if the properties are valid
            final ValueMap properties = notificationPage.getProperties();

            final boolean enabled = properties.get(PN_ENABLED, false);
            if (!enabled) {
                // Disabled
                return false;
            } else {
                final Calendar onTime = properties.get(PN_ON_TIME, Calendar.class);
                final Calendar offTime = properties.get(PN_OFF_TIME, Calendar.class);

                if (onTime == null && offTime == null) {
                    // No on time or off time is set, but is enabled so always show
                    return true;
                }

                final Calendar now = Calendar.getInstance();

                if (onTime != null && now.before(onTime)) {
                    return false;
                }

                if (offTime != null && now.after(offTime)) {
                    return false;
                }

                return true;
            }
        }

        private boolean isDismissed(final SlingHttpServletRequest request,
            final Page notificationPage) {
            final Cookie cookie = CookieUtil.getCookie(request, COOKIE_NAME);
            if (cookie != null) {
                return StringUtils.contains(cookie.getValue(), getNotificationId(notificationPage));
            } else {
                // No cookie has been set, so nothing has been dismissed
                return false;
            }
        }
    }

}
