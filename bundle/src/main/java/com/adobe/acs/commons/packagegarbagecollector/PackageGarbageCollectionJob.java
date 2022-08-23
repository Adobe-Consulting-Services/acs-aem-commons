/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.packagegarbagecollector;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.*;

@Component(
    service = JobConsumer.class,
    immediate = true,
    property = { JobConsumer.PROPERTY_TOPICS + "=" + PackageGarbageCollectionScheduler.JOB_TOPIC })
public class PackageGarbageCollectionJob implements JobConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(PackageGarbageCollectionJob.class);

    private static final String SERVICE_USER = "package-garbage-collection";

    @Reference
    Packaging packaging;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public JobResult process(Job job) {
        Session session = null;
        String groupName = job.getProperty(GROUP_NAME, String.class);
        Integer maxAgeInDays = job.getProperty(MAX_AGE_IN_DAYS, Integer.class);
        LOG.debug("Job Configuration: ["
                + "Group Name: {}, "
                + "Service User: {}, "
                + "Age of Package {} days,]", groupName, SERVICE_USER, maxAgeInDays);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER))) {
            session = resourceResolver.adaptTo(Session.class);
            JcrPackageManager packageManager = packaging.getPackageManager(session);
            List<JcrPackage> packages = packageManager.listPackages(groupName, false);

            for (JcrPackage jcrPackage : packages) {
                String packageDescription = getPackageDescription(jcrPackage);
                if (isPackageOldEnough(jcrPackage, maxAgeInDays)) {
                    packageManager.remove(jcrPackage);
                    LOG.info("Deleted package {}", packageDescription);
                } else {
                    LOG.info("Not removing package because it's not old enough {}", packageDescription);
                }
            }
        } catch (LoginException | RepositoryException | IOException e) {
            LOG.error("Unable to clear packages due to {}", e.toString());
            return JobResult.FAILED;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        LOG.info("Package Garbage Collector job finished");
        return JobResult.OK;
    }

    private boolean isPackageOldEnough(JcrPackage jcrPackage, Integer maxAgeInDays) throws RepositoryException, IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        Period maxAge = Period.ofDays(maxAgeInDays);
        LocalDate oldestAge = LocalDate.now().minus(maxAge);
        Calendar packageCreatedAtCalendar = jcrPackage.getPackage().getCreated();
        LocalDate packageCreatedAt = LocalDateTime.ofInstant(
            packageCreatedAtCalendar.toInstant(),
            packageCreatedAtCalendar.getTimeZone().toZoneId()).toLocalDate();
        String packageDescription = getPackageDescription(jcrPackage);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking if package is old enough: Name: {}, Created At: {}, Oldest Age: {}",
                packageDescription, packageCreatedAt.format(formatter), oldestAge.format(formatter));
        }
        return packageCreatedAt.isBefore(oldestAge) || packageCreatedAt.isEqual(oldestAge);
    }

    private String getPackageDescription(JcrPackage jcrPackage) throws RepositoryException {
        Node packageNode = jcrPackage.getNode();
        if (packageNode != null) {
            return packageNode.getPath();
        }
        return "Unknown package";
    }
}
