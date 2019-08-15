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
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.packaging.PackageId;

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
public final class AcsCommonsAuthorizableCompatibilityCheck implements ProgressCheckFactory {
    public static final String NT_REP_AUTHORIZABLE = "rep:Authorizable";
    public static final String CONFIG_SCOPE_IDS = "scopeIds";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final List<Rule> scopeIds = Rule.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_IDS));
        return new Check(scopeIds);
    }

    static final class Check extends SimpleProgressCheck {
        private final List<Rule> scopeIds;

        Check(final List<Rule> scopeIds) {
            this.scopeIds = new ArrayList<>(scopeIds);
        }

        @Override
        public String getCheckName() {
            return AcsCommonsAuthorizableCompatibilityCheck.class.getSimpleName();
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

                    // if id is excluded, short circuit
                    if (lastMatched.isExclude()) {
                        return;
                    }

                    if (authz.getID().startsWith("acs-commons")) {
                        reportViolation(Violation.Severity.MAJOR,
                                String.format("%s: reserved ID prefix [%s]", path, authz.getID()),
                                packageId);
                    }
                }
            }
        }
    }
}
