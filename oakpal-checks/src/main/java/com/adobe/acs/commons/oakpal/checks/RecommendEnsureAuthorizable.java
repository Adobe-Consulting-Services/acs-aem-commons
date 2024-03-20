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
package com.adobe.acs.commons.oakpal.checks;

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.ProgressCheckFactory;
import net.adamcin.oakpal.api.Rule;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.api.Severity;
import net.adamcin.oakpal.api.SimpleProgressCheckFactoryCheck;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.packaging.PackageId;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;

/**
 * Report explicitly-imported rep:SystemUser and rep:Group nodes as violations, to encourage migration to ACS AEM Commons
 * - Ensure Authorizable (aka Ensure System User).
 * {@code config} items:
 * <dl>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.api.Severity#MINOR}) specify the severity of violations reported
 * by this check.</dd>
 * <dt>{@code scopeIds} ({@link Rule[]})</dt>
 * <dd>(default: include all) List of scope rules matching rep:authorizableId values for inclusion in the scope for
 * compatibility check.</dd>
 * <dt>{@code recommendation} (String)</dt>
 * <dd>(default: {@link #DEFAULT_RECOMMENDATION}) provide a recommendation message.</dd>
 * </dl>
 */
public final class RecommendEnsureAuthorizable implements ProgressCheckFactory {
    public static final String NT_REP_AUTHORIZABLE = "rep:Authorizable";
    public static final String CONFIG_SEVERITY = "severity";
    public static final String CONFIG_RECOMMENDATION = "recommendation";
    public static final String CONFIG_SCOPE_IDS = "scopeIds";

    /**
     * This constant value is referenced as an i18n key in {@link RecommendEnsureAuthorizable}.properties.
     */
    public static final String DEFAULT_RECOMMENDATION = "DEFAULT_RECOMMENDATION";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final Severity severity = Severity.valueOf(config.getString(CONFIG_SEVERITY,
                Severity.MINOR.name()).toUpperCase());
        final String recommendation = config.getString(CONFIG_RECOMMENDATION, DEFAULT_RECOMMENDATION);
        final List<Rule> scopeIds = Rules.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_IDS));
        return new Check(severity, recommendation, scopeIds);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<RecommendEnsureAuthorizable> {
        private final Severity severity;
        private final String recommendation;
        private final List<Rule> scopeIds;

        Check(final Severity severity, final String recommendation, final List<Rule> scopeIds) {
            super(RecommendEnsureAuthorizable.class);
            this.severity = severity;
            this.recommendation = recommendation;
            this.scopeIds = new ArrayList<>(scopeIds);
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {
            // fast check for authorizables
            if (node.isNodeType(NT_REP_AUTHORIZABLE)) {
                final UserManager userManager = ((JackrabbitSession) node.getSession()).getUserManager();
                final Authorizable authz = userManager.getAuthorizableByPath(path);

                // if an authorizable is not loaded from the path, short circuit.
                if (authz != null) {
                    final String id = authz.getID();

                    // check for inclusion based on authorizableId
                    Rule lastMatched = Rules.lastMatch(scopeIds, id);

                    // if id is excluded, or is user and not system user, short circuit
                    if (lastMatched.isExclude() || (!authz.isGroup() && !((User) authz).isSystemUser())) {
                        return;
                    }

                    // report for groups and system users
                    reporting(violation -> violation
                            .withSeverity(severity)
                            .withPackage(packageId)
                            .withDescription("{0}: imported explicit {1}. {2}")
                            .withArgument(path,
                                    authz.isGroup() ? getString("group") : getString("system user"),
                                    recommendation));
                }
            }
        }
    }
}
