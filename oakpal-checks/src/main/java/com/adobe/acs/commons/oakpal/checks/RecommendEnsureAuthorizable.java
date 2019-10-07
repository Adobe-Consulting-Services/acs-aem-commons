/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2019 Adobe
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
package com.adobe.acs.commons.oakpal.checks;

import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.packaging.PackageId;

/**
 * Report explicitly-imported rep:SystemUser and rep:Group nodes as violations, to encourage migration to ACS AEM Commons
 * - Ensure Authorizable (aka Ensure System User).
 * {@code config} items:
 * <dl>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link Violation.Severity#MINOR}) specify the severity of violations reported
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
    public static final String DEFAULT_RECOMMENDATION = "We recommend using Ensure Authorizable instead. https://adobe-consulting-services.github.io/acs-aem-commons/features/ensure-service-users/index.html";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final Violation.Severity severity = Violation.Severity.valueOf(config.getString(CONFIG_SEVERITY,
                Violation.Severity.MINOR.name()).toUpperCase());
        final String recommendation = config.getString(CONFIG_RECOMMENDATION, DEFAULT_RECOMMENDATION);
        final List<Rule> scopeIds = Rule.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_IDS));
        return new Check(severity, recommendation, scopeIds);
    }

    static final class Check extends SimpleProgressCheck {
        private final Violation.Severity severity;
        private final String recommendation;
        private final List<Rule> scopeIds;

        Check(final Violation.Severity severity, final String recommendation, final List<Rule> scopeIds) {
            this.severity = severity;
            this.recommendation = recommendation;
            this.scopeIds = new ArrayList<>(scopeIds);
        }

        @Override
        public String getCheckName() {
            return RecommendEnsureAuthorizable.class.getSimpleName();
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
                    Rule lastMatched = Rule.lastMatch(scopeIds, id);

                    // if id is excluded, or is user and not system user, short circuit
                    if (lastMatched.isExclude() || (!authz.isGroup() && !((User) authz).isSystemUser())) {
                        return;
                    }

                    // report for groups and system users
                    reportViolation(severity,
                            String.format("%s: imported explicit %s. %s",
                                    path, authz.isGroup() ? "group" : "system user", recommendation), packageId);
                }
            }
        }
    }

}
