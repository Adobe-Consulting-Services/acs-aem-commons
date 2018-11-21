/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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

import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.json.JSONObject;

/**
 * Checks the authorizableId of imported {@link Authorizable} nodes to ensure they do not begin with "acs-commons".
 * <p>
 * {@code config} items:
 * <dl>
 * <dt>{@code scopeIds} ({@link Rule[]})</dt>
 * <dd>(default: include all) List of scope rules matching rep:authorizableId values for inclusion in the scope for
 * compatibility check.</dd>
 * </dl>
 */
public class AcsCommonsAuthorizableCompatibilityCheck implements ProgressCheckFactory {
    public static final String NT_REP_AUTHORIZABLE = "rep:Authorizable";
    public static final String CONFIG_SCOPE_IDS = "scopeIds";

    class Check extends SimpleProgressCheck {
        private final List<Rule> scopeIds;

        public Check(final List<Rule> scopeIds) {
            this.scopeIds = scopeIds;
        }

        @Override
        public String getCheckName() {
            return AcsCommonsAuthorizableCompatibilityCheck.this.getClass().getSimpleName();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {
            // fast check for authorizables
            if (node.isNodeType(NT_REP_AUTHORIZABLE)) {
                UserManager userManager = ((JackrabbitSession) node.getSession()).getUserManager();
                Authorizable authz = userManager.getAuthorizableByPath(path);

                // if an authorizable is not loaded from the path, short circuit.
                if (authz == null) {
                    return;
                }

                final String id = authz.getID();

                // check for inclusion based on authorizableId
                Rule lastMatched = Rule.fuzzyDefaultAllow(scopeIds);
                for (Rule scopeId : scopeIds) {
                    if (scopeId.matches(id)) {
                        lastMatched = scopeId;
                    }
                }

                // if id is excluded, short circuit
                if (lastMatched.isDeny()) {
                    return;
                }

                if (authz.getID().startsWith("acs-commons")) {
                    reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("%s: reserved ID prefix [%s]", path, authz.getID()),
                            packageId));
                }
            }
        }
    }

    @Override
    public ProgressCheck newInstance(final JSONObject config) {
        final List<Rule> scopeIds = Rule.fromJSON(config.optJSONArray(CONFIG_SCOPE_IDS));
        return new Check(scopeIds);
    }
}
