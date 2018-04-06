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

package com.adobe.acs.commons.users.impl;

import org.apache.commons.lang.ArrayUtils;

public final class ProtectedAuthorizables {
    private ProtectedAuthorizables() {}

    private static final String[] PRINCIPAL_NAMES = new String[] {

                /** AEM SYSTEM USERS **/

                "activity-service",
                "personalization-service",
                "pageexporterservice",
                "projects-service",
                "foundation-forms-store-service",
                "hierarchymodification-listener-service",
                "foundation-forms-service",
                "media-service",
                "authentication-service",
                "monitoringScripts",
                "cryptoservice",
                "snapshotservice",
                "tag-garbage-collection-service",
                "contentsync-service",
                "account-manager",
                "index-admin",
                "searchpromote-service",
                "device-identification-service",
                "offloading-jobcloner",
                "workflow-service",
                "scheduled-exporter-service",
                "replication-receiver",
                "commerce-orders-service",
                "commerce-frontend-service",
                "commerce-backend-service",
                "recs-deleted-products-listener-service",
                "audiencemanager-syncsegments-service",
                "audiencemanager-configlistener-service",
                "linkstorage-service",
                "campaign-reader",
                "dam-replication-service",
                "packagelist-service",
                "targetservice",
                "webservice-support-servicelibfinder",
                "webservice-support-replication",
                "oauthservice",
                "compat-codeupgrade-service",
                "taskmanagement-service",
                "analytics-content-updater-service",
                "spellchecker-service",
                "dam-teammgr-service",
                "activitypurgesrv",
                "scene7-asset-service",
                "idsjobprocessor",
                "dam-formitemseditor-service",
                "dam-update-service",
                "assetusagetrackeruser",
                "youtube-authenticator-user",
                "webdavbkpservice",
                "dynamicmedia-config-service",
                "dam-activitywriter-service",
                "scene7-config-service",
                "dynamic-media-replication-filter",
                "dynamic-media-replication",
                "s7dam-config-service",
                "assetlinkshareservice",
                "resourcecollectionservice",
                "dam-asseteventmonitor-service",
                "offloading-agentmanager",
                "workflow-user-service",
                "msm-service",
                "dtmservice",
                "communities-analytics-admin",
                "communities-acl-manager",
                "social-enablement-tmp-manager",
                "social-enablement-replication-user",
                "communities-tag-admin",
                "communities-srp-config-reader",
                "communities-enablement-property-writer",
                "communities-index-admin",
                "communities-ugc-writer",
                "communities-user-admin",
                "communities-workflow-launcher",
                "communities-utility-reader",
                "primary-resource-search-service",
                "undo-service",
                "canvaspage-delete-service",
                "canvaspage-activate-service",
                "wcm-workflow-service",
                "notification-service",
                "version-purge-service",
                "design-cache-service",
                "launch-event-service",
                "launch-promote-service",
                "language-manager-service",
                "components-search-service",
                "repository-change-listener-service",
                "reference-adjustment-service",
                "version-manager-service",
                "wurfl-loader-service",
                "clientlibs-service",
                "offloading-service",
                "security-userproperties-service",
                "fd-service",
                "replication-service",
                "fontconfig-service",
                "workflow-process-service",
                "workflow-repo-reader-service",
                "analyticsservice",
                "statistics-service",
                "cug-service",
                "polling-importer-service",
                "history-listener-service",
                "campaign-remote",
                "sling-installer-service",
                "tag-validation-service",
                "campaign-cloudservice",
                "repository-reader-service",
                "omnisearch-service",
                "audit-service",
                "translation-job-service",
                "translation-config-service",

                /** ACS COMMONS SYSTEM USERS **/

                "acs-commons-workflow-remover-service",
                "acs-commons-ensure-oak-index-service",
                "acs-commons-system-notifications-service",
                "acs-commons-form-helper-service",
                "acs-commons-bulk-workflow-service",
                "acs-commons-error-page-handler-service",
                "acs-commons-wcm-inbox-cleanup-service",
                "acs-commons-email-service",
                "acs-commons-package-replication-status-event-service",
                "acs-commons-component-error-handler-service",
                "acs-commons-dispatcher-flush-service",
                "acs-commons-twitter-updater-service",
                "acs-commons-review-task-asset-mover-service",
                "acs-commons-httpcache-jcr-storage-service",
                "acs-commons-automatic-package-replicator-service",
                "acs-commons-manage-controlled-processes-service",

                /** AEM Groups **/

                "administrators",
                "content-authors",
                "contributor",
                "dam-users",
                "everyone",
                "tag-administrators",
                "template-authors",
                "user-administrators",
                "workflow-administrators",
                "workflow-users",
                "workflow-editors"
    };

    public static final boolean isProtected(String principalName) {
        return ArrayUtils.contains(PRINCIPAL_NAMES, principalName);
    }
}
