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
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
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

import javax.annotation.Nonnull;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.GROUP_NAME;
import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.MAX_AGE_IN_DAYS;

@Component(
    service = JobConsumer.class,
    immediate = true,
    property = { JobConsumer.PROPERTY_TOPICS + "=" + PackageGarbageCollectionScheduler.JOB_TOPIC })
public class PackageGarbageCollectionJob implements JobConsumer {
    public static final DateTimeFormatter LOCALIZED_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final Logger LOG = LoggerFactory.getLogger(PackageGarbageCollectionJob.class);

    private static final String SERVICE_USER = "package-garbage-collection";

    @Reference
    Packaging packaging;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public JobResult process(Job job) {
        String groupName = job.getProperty(GROUP_NAME, String.class);
        Integer maxAgeInDays = job.getProperty(MAX_AGE_IN_DAYS, Integer.class);
        int packagesRemoved = 0;
        LOG.debug("Job Configuration: ["
                + "Group Name: {}, "
                + "Service User: {}, "
                + "Age of Package {} days,]", groupName, SERVICE_USER, maxAgeInDays);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER))) {
            Session session = resourceResolver.adaptTo(Session.class);
            JcrPackageManager packageManager = packaging.getPackageManager(session);
            List<JcrPackage> packages = packageManager.listPackages(groupName, false);

            for (JcrPackage jcrPackage : packages) {
                String packageDescription = getPackageDescription(jcrPackage);
                LOG.info("Processing package {}", packageDescription);
                if (isPackageOldEnough(jcrPackage, maxAgeInDays)) {
                    if (!isLatestInstalled(jcrPackage, packages)) {
                        packageManager.remove(jcrPackage);
                        packagesRemoved++;
                        LOG.info("Deleted package {}", packageDescription);
                    } else {
                        LOG.info("Not removing package because it's the current installed one {}", packageDescription);
                    }
                } else {
                    LOG.debug("Not removing package because it's not old enough {}", packageDescription);
                }
            }
        } catch (LoginException | RepositoryException | IOException e) {
            if (packagesRemoved > 0) {
                LOG.error("Package Garbage Collector job partially failed - Removed {} packages", packagesRemoved);
            }
            LOG.error("Unable to finish clearing packages", e);
            return JobResult.FAILED;
        }
        LOG.info("Package Garbage Collector job finished - Removed {} packages", packagesRemoved);
        return JobResult.OK;
    }

    private boolean isLatestInstalled(JcrPackage jcrPackage, List<JcrPackage> installedPackages) {
        Optional<JcrPackage> lastInstalledPackageOptional = installedPackages.stream().filter(installedPackage -> {
            PackageDefinition definition = new PackageDefinition(installedPackage);
            return definition.isSameNameAndGroup(jcrPackage);
        }).max(Comparator.comparing(pkg -> new PackageDefinition(pkg).getLastUnpacked()));

        if (lastInstalledPackageOptional.isPresent()) {
            JcrPackage lastInstalledPackage = lastInstalledPackageOptional.get();
            PackageDefinition lastInstalledPackageDefinition = new PackageDefinition(lastInstalledPackage);
            PackageDefinition thisPackageDefinition = new PackageDefinition(jcrPackage);

            // If it's not actually installed yet.
            if (lastInstalledPackageDefinition.getLastUnpacked() == null) {
                return false;
            }

            return lastInstalledPackageDefinition.hasSamePid(thisPackageDefinition);
        }

        return false;
    }

    static class PackageDefinition {
        JcrPackage jcrPackage;

        public PackageDefinition(@Nonnull JcrPackage jcrPackage) {
            this.jcrPackage = jcrPackage;
        }

        public Calendar getLastUnpacked() {
            try {
                JcrPackageDefinition definition = jcrPackage.getDefinition();
                if (definition != null) {
                    return definition.getLastUnpacked();
                }
                return null;
            } catch (RepositoryException ex) {
                return null;
            }
        }

        public boolean isSameNameAndGroup(JcrPackage otherPackage) {
            Optional<PackageId> otherPackageId = getPid(otherPackage);
            Optional<PackageId> thisPackageId = getPid(jcrPackage);
            if (otherPackageId.isPresent() && thisPackageId.isPresent()) {
                return otherPackageId.get().getGroup().equals(thisPackageId.get().getGroup())
                        && otherPackageId.get().getName().equals(thisPackageId.get().getName());
            }
            return false;
        }

        public PackageId getId() {
            try {
                JcrPackageDefinition definition = jcrPackage.getDefinition();
                if (definition != null) {
                    return definition.getId();
                }
                return null;
            } catch (RepositoryException ex) {
                return null;
            }
        }

        private Optional<PackageId> getPid(JcrPackage jcrPkg) {
            try {
                return Optional.ofNullable(jcrPkg.getDefinition()).map(JcrPackageDefinition::getId);
            } catch (RepositoryException ex) {
                return Optional.empty();
            }
        }

        public boolean hasSamePid(PackageDefinition jcrPkg) {
            try {
                Optional<PackageId> pkgId = Optional.ofNullable(jcrPkg.getId());
                return pkgId.map(packageId -> packageId.equals(getId())).orElse(false);
            } catch (NullPointerException ex) {
                return false;
            }
        }
    }

    private boolean isPackageOldEnough(JcrPackage jcrPackage, Integer maxAgeInDays) throws RepositoryException, IOException {
        Period maxAge = Period.ofDays(maxAgeInDays);
        LocalDate oldestAge = LocalDate.now().minus(maxAge);
        Calendar packageCreatedAtCalendar = jcrPackage.getPackage().getCreated();
        LocalDate packageCreatedAt = LocalDateTime.ofInstant(
            packageCreatedAtCalendar.toInstant(),
            packageCreatedAtCalendar.getTimeZone().toZoneId()).toLocalDate();
        String packageDescription = getPackageDescription(jcrPackage);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking if package is old enough: Name: {}, Created At: {}, Oldest Age: {}",
                packageDescription, packageCreatedAt.format(LOCALIZED_DATE_FORMATTER), oldestAge.format(LOCALIZED_DATE_FORMATTER));
        }
        return !packageCreatedAt.isAfter(oldestAge);
    }

    private String getPackageDescription(JcrPackage jcrPackage) throws RepositoryException {
        JcrPackageDefinition definition = jcrPackage.getDefinition();
        Node packageNode = jcrPackage.getNode();
        if (definition != null && packageNode != null) {
            return String.format("%s:%s:v%s [%s]", definition.getId().getName(), definition.getId().getGroup(), definition.getId().getVersionString(), packageNode.getPath());
        }
        return "Unknown package";
    }
}
