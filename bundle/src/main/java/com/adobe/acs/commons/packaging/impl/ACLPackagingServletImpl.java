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

package com.adobe.acs.commons.packaging.impl;

import com.day.jcr.vault.fs.api.PathFilterSet;
import com.day.jcr.vault.fs.config.DefaultWorkspaceFilter;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.Packaging;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.*;

@SlingServlet(
        label = "ACS AEM Commons - ACL Packaging Servlet",
        description = "...",
        methods = {"POST"},
        resourceTypes = {"acs-commons/components/utilities/acl-packaging"},
        selectors = {"package"},
        extensions = {"json"}
)
public class ACLPackagingServletImpl extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(ACLPackagingServletImpl.class);

    private static final String PRINCIPAL_NAMES = "principalNames";
    private static final String PACKAGE_NAME = "packageName";
    private static final String PACKAGE_GROUP_NAME = "packageGroupName";
    private static final String PACKAGE_VERSION = "packageVersion";

    private static final String QUERY = "SELECT * FROM [rep:ACL]";
    private static final String QUERY_LANG = Query.JCR_SQL2;

    @Reference
    private Packaging packaging;

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        final ValueMap parameters = this.getParameters(request);

        final Set<Resource> repPolicyResources = this.findResources(request.getResourceResolver(),
                Arrays.asList(parameters.get(PRINCIPAL_NAMES, new String[]{})));

        this.createPackage(repPolicyResources, request.getResourceResolver().adaptTo(Session.class),
                parameters.get(PACKAGE_NAME, "ACL Package"),
                parameters.get(PACKAGE_GROUP_NAME, "ACLs"),
                parameters.get(PACKAGE_VERSION, "1.0.0"));
    }

    private ValueMap getParameters(final SlingHttpServletRequest request) {
        final Map<String, Object> map = new HashMap<String, Object>();

        map.put(PACKAGE_NAME, request.getRequestParameter(PACKAGE_NAME).toString());
        map.put(PACKAGE_GROUP_NAME, request.getRequestParameter(PACKAGE_GROUP_NAME).toString());
        map.put(PACKAGE_VERSION, request.getRequestParameter(PACKAGE_VERSION).toString());

        log.debug("{} = [ {} ]", PACKAGE_NAME, map.get(PACKAGE_NAME));
        log.debug("{} = [ {} ]", PACKAGE_GROUP_NAME, map.get(PACKAGE_GROUP_NAME));
        log.debug("{} = [ {} ]", PACKAGE_VERSION, map.get(PACKAGE_VERSION));

        final List<String> principalNames = new ArrayList<String>();

        log.debug("{}", PRINCIPAL_NAMES);

        if(request.getRequestParameters(PRINCIPAL_NAMES) != null) {
            for(final RequestParameter requestParameter : request.getRequestParameters(PRINCIPAL_NAMES)) {
                principalNames.add(requestParameter.toString());
                log.debug(" > {}", requestParameter.toString());
            }
        } else {
            log.debug(" > No principal names passed in.");
        }

        map.put(PRINCIPAL_NAMES, principalNames.toArray(new String[principalNames.size()]));

        return new ValueMapDecorator(map);
    }

   private Set<Resource> findResources(final ResourceResolver resourceResolver, final List<String> principalNames) {
       final Set<Resource> resources = new TreeSet<Resource>(resourceComparator);

       final Iterator<Resource> repPolicies = resourceResolver.findResources(QUERY, QUERY_LANG);

       while(repPolicies.hasNext()) {
           final Resource repPolicy = repPolicies.next();
           log.debug("Found rep:policy node at: {}", repPolicy.getPath());
           final Iterator<Resource> aces = repPolicy.listChildren();

           while(aces.hasNext()) {
               final Resource ace = aces.next();
               final ValueMap props = ace.adaptTo(ValueMap.class);
               final String repPrincipalName = props.get("rep:principalName", String.class);

               if(principalNames == null || principalNames.isEmpty() || principalNames.contains(repPrincipalName)) {
                    resources.add(repPolicy);
                   log.debug(" > {}", repPolicy.getPath());
                   break;
               }
           }
       }

       log.debug("Found {} matching rep:policy resources.", resources.size());
       return resources;
   }

   private JcrPackage createPackage(final Set<Resource> resources, final Session session,
                                    final String name, final String groupName, final String version) {

       final JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);

       try {
           final JcrPackage jcrPackage = jcrPackageManager.create(name, groupName, version);
           final JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
           final DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();

           for(final Resource resource : resources) {
               workspaceFilter.add(new PathFilterSet(resource.getPath()));
           }

           jcrPackageDefinition.setFilter(workspaceFilter, true);

           jcrPackageDefinition.set(JcrPackageDefinition.PN_AC_HANDLING, "Overwrite", true);


           log.debug("Sucessfully created Jcr Package");

           return jcrPackage;
       } catch (RepositoryException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       }

       return null;
   }

    private static Comparator<Resource> resourceComparator = new Comparator<Resource>() {
        public int compare(Resource r1, Resource r2) {
            return r1.getPath().compareTo(r2.getPath());
        }
    };
}
