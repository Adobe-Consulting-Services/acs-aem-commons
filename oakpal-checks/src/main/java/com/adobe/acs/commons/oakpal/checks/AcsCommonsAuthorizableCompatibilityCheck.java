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
