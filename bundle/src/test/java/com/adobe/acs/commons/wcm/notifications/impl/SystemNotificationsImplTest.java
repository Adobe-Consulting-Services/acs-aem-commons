/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_ORDERED_FOLDER;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.services.MockSlingSettingService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

public class SystemNotificationsImplTest {

    @Rule
    public AemContext aemContext = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private SystemNotificationsImpl notifications = new SystemNotificationsImpl();
    private ResourceResolver resourceResolver;

    private Calendar zeroHour;

    @Before
    public void setup() {
        resourceResolver = aemContext.resourceResolver();
        zeroHour = Calendar.getInstance();
        zeroHour.setTime(new Date(0));
    }

    public <T> void assertNoSystemNotificationsImplService(Class<T> serviceClass) {
        for (T service : aemContext.getServices(serviceClass, null)) {
            if (service instanceof SystemNotificationsImpl) {
                Assert.fail("Found unexpected service implementation SystemNotificationsImpl for type " + serviceClass);
            }
        }
    }

    public <T> void assertSystemNotificationsImplService(Class<T> serviceClass) {
        for (T service : aemContext.getServices(serviceClass, null)) {
            if (service instanceof SystemNotificationsImpl) {
                return;
            }
        }
        Assert.fail("Found no service implementation SystemNotificationsImpl for type " + serviceClass);
    }

    @Test
    public void testOnPublish() {
        aemContext.registerInjectActivateService(notifications);
        assertNoSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);
    }

    @Test
    public void testOnAuthorNoNotifications() {
        setAuthorRunmode();
        aemContext.registerInjectActivateService(notifications);
        assertSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);
    }

    @Test
    public void testOnAuthorEmptyNotificationsFolder() throws Exception {
        setAuthorRunmode();
        aemContext.build()
                .resource("/etc/acs-commons/notifications", JCR_PRIMARYTYPE, NT_PAGE)
                .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
        commit();
        aemContext.registerInjectActivateService(notifications);
        assertSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);

        aemContext.create().resource("/etc/acs-commons/notifications/first", JCR_PRIMARYTYPE, NT_PAGE);
        commit();
        sendEvent();
        assertSystemNotificationsImplService(Filter.class);

        notifications.deactivate(aemContext.componentContext());
        assertNoSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);
    }

    @Test
    public void testOnAuthorPopulatedNotificationsFolder() throws Exception  {
        setAuthorRunmode();
        createEnabledNotification();
        aemContext.registerInjectActivateService(notifications);
        assertSystemNotificationsImplService(ResourceChangeListener.class);
        assertSystemNotificationsImplService(Filter.class);

        // then remove the resource
        delete("/etc/acs-commons/notifications/enabled");
        commit();
        sendEvent();
        assertNoSystemNotificationsImplService(Filter.class);

        notifications.deactivate(aemContext.componentContext());
        assertNoSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);
    }

    @Test
    public void testOnAuthorPopulatedNotificationsSubFolder() throws Exception  {
        setAuthorRunmode();
        aemContext.build()
            .resource("/etc/acs-commons/notifications", JCR_PRIMARYTYPE, NT_PAGE)
            .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED);

        createEnabledNotification("/etc/acs-commons/notifications/subfolder/enabled");
        aemContext.registerInjectActivateService(notifications);
        assertSystemNotificationsImplService(ResourceChangeListener.class);
        assertSystemNotificationsImplService(Filter.class);

        // then remove the resource
        delete("/etc/acs-commons/notifications/subfolder/enabled");
        commit();
        sendEvent();
        assertNoSystemNotificationsImplService(Filter.class);

        notifications.deactivate(aemContext.componentContext());
        assertNoSystemNotificationsImplService(ResourceChangeListener.class);
        assertNoSystemNotificationsImplService(Filter.class);
    }

    @Test
    public void testFilter() throws Exception  {
        setAuthorRunmode();
        createEnabledNotification();
        aemContext.registerInjectActivateService(notifications);

        aemContext.request().setResource(aemContext.resourceResolver().getResource("/"));

        notifications.doFilter(aemContext.request(), aemContext.response(), outputChain);
        String output = aemContext.response().getOutputAsString();
        assertThat(output, stringContainsInOrder(Arrays.asList("<html><body>", "<script>", "</script>", "</body></html>")));
    }

    @Test
    public void testFilterOnNotificationPage() throws Exception  {
        setAuthorRunmode();
        createEnabledNotification();
        aemContext.registerInjectActivateService(notifications);

        aemContext.request().setResource(aemContext.resourceResolver().getResource("/etc/acs-commons/notifications"));

        notifications.doFilter(aemContext.request(), aemContext.response(), outputChain);
        String output = aemContext.response().getOutputAsString();
        assertEquals(output.trim(), "<html><body></body></html>");
    }

    @Test
    public void testFilterWhenDismissed() throws Exception  {
        setAuthorRunmode();
        createEnabledNotification();
        aemContext.registerInjectActivateService(notifications);

        aemContext.request().setResource(aemContext.resourceResolver().getResource("/"));
        aemContext.request().addCookie(new Cookie("acs-commons-system-notifications", "uid-" + DigestUtils.sha1Hex("/etc/acs-commons/notifications/enabled0")));

        notifications.doFilter(aemContext.request(), aemContext.response(), outputChain);
        String output = aemContext.response().getOutputAsString();
        assertEquals(output.trim(), "<html><body></body></html>");
    }

    @Test
    public void testFilterWhenDisabled() throws Exception  {
        setAuthorRunmode();
        createDisabledNotification();
        aemContext.registerInjectActivateService(notifications);

        aemContext.request().setResource(aemContext.resourceResolver().getResource("/"));

        notifications.doFilter(aemContext.request(), aemContext.response(), outputChain);
        String output = aemContext.response().getOutputAsString();
        assertEquals(output.trim(), "<html><body></body></html>");
    }

    private FilterChain outputChain = (req, res) -> {
        res.setContentType("text/html");
        res.getWriter().println("<html><body></body></html>");
    };

    private void createEnabledNotification() throws PersistenceException {
        createEnabledNotification("/etc/acs-commons/notifications/enabled");
    }

    private void createEnabledNotification(String path) throws PersistenceException {
        aemContext.build()
            .resource("/etc/acs-commons/notifications", JCR_PRIMARYTYPE, NT_PAGE)
            .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED)
            .withIntermediatePrimaryType(NT_SLING_ORDERED_FOLDER)
            .resource(path, JCR_PRIMARYTYPE, NT_PAGE)
            .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED, "enabled", true, "cq:lastModified", zeroHour);
        commit();
    }

    private void createDisabledNotification() throws PersistenceException {
        aemContext.build()
                .resource("/etc/acs-commons/notifications", JCR_PRIMARYTYPE, NT_PAGE)
                .siblingsMode()
                .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED)
                .hierarchyMode()
                .resource("disabled", JCR_PRIMARYTYPE, NT_PAGE)
                .resource("jcr:content", JCR_PRIMARYTYPE, NT_UNSTRUCTURED, "enabled", false, "cq:lastModified", zeroHour);
        commit();
    }

    private void sendEvent() {
        notifications.onChange(Collections.singletonList(new ResourceChange(ChangeType.ADDED, "/etc/acs-commons/notifications", false, null, null, null)));
    }

    private void delete(String path) throws PersistenceException {
        resourceResolver.delete(aemContext.resourceResolver().getResource(path));
    }

    private void commit() throws PersistenceException {
        resourceResolver.commit();
    }

    private void setAuthorRunmode() {
        MockSlingSettingService settingService = (MockSlingSettingService) aemContext.getService(SlingSettingsService.class);
        settingService.setRunModes(Collections.singleton("author"));
    }

}
