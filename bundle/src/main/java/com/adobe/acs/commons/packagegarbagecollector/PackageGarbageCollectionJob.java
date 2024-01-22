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

import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.GROUP_NAME;
import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.MAX_AGE_IN_DAYS;
import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.REMOVE_NOT_INSTALLED_PACKAGES;

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
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

@Component(
        service = JobConsumer.class,
        immediate = true,
        property = {JobConsumer.PROPERTY_TOPICS + "=" + PackageGarbageCollectionScheduler.JOB_TOPIC})
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
        boolean removeNotInstalledPackages = job.getProperty(REMOVE_NOT_INSTALLED_PACKAGES, false);
        int packagesRemoved = 0;
        LOG.debug("Job Configuration: ["
                + "Group Name: {}, "
                + "Service User: {}, "
                + "Age of Package {} days,"
                + "Remove not installed packages: {}]", groupName, SERVICE_USER, maxAgeInDays, removeNotInstalledPackages);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER))) {
            Session session = resourceResolver.adaptTo(Session.class);
            JcrPackageManager packageManager = packaging.getPackageManager(session);
            List<JcrPackage> packages = packageManager.listPackages(groupName, false);

            for (JcrPackage tmpPackage : packages) {
                try (JcrPackage jcrPackage = tmpPackage) {
                    JcrPackageDefinition definition = jcrPackage.getDefinition();
                    if (definition == null) {
                        LOG.warn("Skipping package without definition: {}", jcrPackage.getNode().getPath());
                    }
                    String packageDescription = getPackageDescription(definition);
                    LOG.info("Processing package {}", packageDescription);

                    if (isPackageOldEnough(definition, maxAgeInDays)) {
                        if (removeNotInstalledPackages && !isInstalled(definition)) {
                            packageManager.remove(jcrPackage);
                            packagesRemoved++;
                            LOG.info("Deleted not-installed package {}", packageDescription);
                        } else if (isInstalled(definition) && !isLatestInstalled(definition, packageManager.listPackages(groupName, false).stream())) {
                            packageManager.remove(jcrPackage);
                            packagesRemoved++;
                            LOG.info("Deleted installed package {} since it is not the latest installed version.", packageDescription);
                        } else {
                            LOG.info("Not removing package because it's the current installed one {}", packageDescription);
                        }
                    } else {
                        LOG.debug("Not removing package because it's not old enough {}", packageDescription);
                    }
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

    private boolean isInstalled(JcrPackageDefinition pkgDefinition) {
        // lastUnpacked is when the package was installed (aka unpacked) to this AEM/JCR.
        return pkgDefinition.getLastUnpacked() != null;
    }

    private static final class UncheckedRepositoryException extends RuntimeException {
        private static final long serialVersionUID = 8851421623772855854L;

        protected UncheckedRepositoryException(RepositoryException e) {
            super(e);
        }

        /**
         * Returns the cause of this exception.
         *
         * @return the {@code RepositoryException} which is the cause of this exception.
         */
        @Override
        public RepositoryException getCause() {
            return (RepositoryException) super.getCause();
        }

    }

    private boolean isLatestInstalled(JcrPackageDefinition referencePkgDefinition, Stream<JcrPackage> installedPackages) throws RepositoryException {
        try {
            Optional<JcrPackageDefinition> lastInstalledPckDefinitionOptional = installedPackages
                    .map(p -> {
                        try {
                            return p.getDefinition();
                        } catch (RepositoryException e) {
                            String pckPath;
                            try {
                                pckPath = p.getNode().getPath();
                            } catch (RepositoryException nestedException) {
                                pckPath = "Unknown";
                            }
                            throw new UncheckedRepositoryException(new RepositoryException("Cannot read package definition of package " + pckPath, e));
                        }
                    })
                    .filter(def -> isSameNameAndGroup(referencePkgDefinition.getId(), def.getId()))
                    .filter(def -> def.getLastUnpacked() != null)
                    .max(Comparator.comparing(def -> def.getLastUnpacked()));

            if (lastInstalledPckDefinitionOptional.isPresent()) {
                return lastInstalledPckDefinitionOptional.get().getId().equals(referencePkgDefinition.getId());
            }
            return false;
        } catch (UncheckedRepositoryException e) {
            throw e.getCause();
        }
    }

    public static boolean isSameNameAndGroup(PackageId thisPackageId, PackageId otherPackageId) {
        return otherPackageId.getGroup().equals(thisPackageId.getGroup())
                && otherPackageId.getName().equals(thisPackageId.getName());
    }


    private boolean isPackageOldEnough(JcrPackageDefinition pkgDefinition, Integer maxAgeInDays) throws RepositoryException, IOException {
        Period maxAge = Period.ofDays(maxAgeInDays);
        LocalDate oldestAge = LocalDate.now().minus(maxAge);
        // lastUnwrapped is when the package was UPLOADED to this AEM/JCR, lastUnpacked is what it was last INSTALLED!
        Calendar packageUploadedDate;

        try {
            // getLastUnwrapped() is when the package was introduced (aka uploaded) to this AEM/JCR.
            // getCreated() is when the package was created (aka built) by the package manager; which could be years ago.
            packageUploadedDate = pkgDefinition.getLastUnwrapped();

            if (packageUploadedDate == null) {
                // This should not happen, but if it does, we don't want to delete the package.
                LOG.warn("Package [ {} ] has no lastUnwrapped (uploaded) date, assuming it's NOT old enough", pkgDefinition.getNode().getPath());
                return false;
            }
        } catch (RepositoryException e) {
            LOG.error("Unable to get lastUnwrapped (uploaded) date for package [ {} ]", pkgDefinition.getNode().getPath(), e);
            return false;
        }

        LocalDate packageUploadedAt = LocalDateTime.ofInstant(
                packageUploadedDate.toInstant(),
                packageUploadedDate.getTimeZone().toZoneId()).toLocalDate();
        String packageDescription = getPackageDescription(pkgDefinition);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking if package is old enough: Name: {}, Uploaded at: {}, Oldest age: {}",
                    packageDescription, packageUploadedAt.format(LOCALIZED_DATE_FORMATTER), oldestAge.format(LOCALIZED_DATE_FORMATTER));
        }
        return !packageUploadedAt.isAfter(oldestAge);
    }

    private String getPackageDescription(JcrPackageDefinition definition) throws RepositoryException {
        return String.format("%s:%s:v%s [%s]", definition.getId().getName(), definition.getId().getGroup(), definition.getId().getVersionString(), definition.getNode().getPath());
    }
}
