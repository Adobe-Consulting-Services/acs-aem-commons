/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;

/**
 * Web console plugin which allows for management of users WCM Notification Inboxes.
 */
@SuppressWarnings("serial")
@Component
@Service
@Properties({ @Property(name = "felix.webconsole.label", value = "wcm-inbox"),
        @Property(name = "felix.webconsole.title", value = "WCM Inbox") })
public class WCMInboxWebConsolePlugin extends HttpServlet {

    @Reference
    private ResourceResolverFactory rrFactory;

    private static final String SERVICE_NAME = "wcm-inbox-cleanup";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();

        PrintWriter pw = resp.getWriter();

        try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(AUTH_INFO)) {

            pw.println("<p class='statline ui-state-highlight'>Inbox Notification Configurations</p>");
            pw.println("<ul>");

            @SuppressWarnings("deprecation")
            Iterator<Resource> configured = resolver
                    .findResources(
                            "/jcr:root//element(*, rep:User)/wcm/notification/config/subscriptions/element(*)[@channel='inbox']",
                            Query.XPATH);
            while (configured.hasNext()) {
                String path = configured.next().getPath();
                pw.println("<li>");
                pw.printf("<a target='_new' href='/crx/de/index.jsp#%s'>%s</a>", path, path);
                pw.println("</li>");
            }
            pw.println("</ul>");

            pw.println("<br/>");

            pw.println("<p class='statline ui-state-highlight'>Inbox Notification Sizes</p>");
            pw.println("<table class='content'>");
            pw.println("<tr><th class='content'>Path</th><th class='content'>Count</th><th class='content'>In Last 24 Hours</th><th></th></tr>");

            @SuppressWarnings("deprecation")
            Iterator<Resource> inboxes = resolver.findResources(
                    "/jcr:root//element(*, rep:User)/wcm/notification/inbox", Query.XPATH);
            while (inboxes.hasNext()) {
                Resource inbox = inboxes.next();
                long[] childCount = countChildren(inbox, yesterday);
                pw.printf(
                        "<tr><td class='content'>%s</td><td class='content'>%s</td><td class='content'>%s</td><td><form method='POST' action=''><input type='hidden' name='path' value='%s'><input type='submit' value='Clear'></form></td></tr>%n",
                        inbox.getPath(), childCount[0], childCount[1], inbox.getPath());
            }

            pw.println("</table>");

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getParameter("path");
        if (path != null) {
            try (ResourceResolver resolver = rrFactory.getServiceResourceResolver(AUTH_INFO)){
                int counter = 0;
                Session session = resolver.adaptTo(Session.class);
                Node node = session.getNode(path);
                NodeIterator it = node.getNodes();
                while (it.hasNext()) {
                    it.nextNode().remove();
                    counter++;
                }
                session.save();

                resp.getWriter().printf("<p class='statline ui-state-error'>Deleted %s notifications</p>%n", counter);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        resp.sendRedirect((String) req.getAttribute("felix.webconsole.pluginRoot"));
    }

    private long[] countChildren(Resource inbox, Date yesterday) {
        long counter = 0;
        long yesterdayCounter = 0;
        Iterator<Resource> children = inbox.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap map = child.adaptTo(ValueMap.class);
            Date date = map.get("modifiedDate", Date.class);
            if (date != null && date.after(yesterday)) {
                yesterdayCounter++;
            }
            counter++;
        }
        return new long[] { counter, yesterdayCounter };
    }
}
