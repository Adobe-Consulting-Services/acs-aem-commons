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
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.json.JSONObject;

/**
 * Report explicitly-imported oak:index nodes as violations to encourage migration to ACS AEM Commons - Ensure Oak Index.
 * {@code config} items:
 * <dl>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.core.Violation.Severity#MINOR}) specify the severity of violations reported
 * by this check.</dd>
 * <dt>{@code scopePaths} (type: {@link Rule[]})</dt>
 * <dd>(default: allow all) specify a list of pattern rules to allow or deny import paths from the scope of this check.
 * When no pattern rules are specified, ALLOW ALL imported paths into scope for the check. When the first rule is type
 * ALLOW, the default first rule becomes DENY ALL.
 * </dd>
 * <dt>{@code recommendation}</dt>
 * <dd>(default: {@link #DEFAULT_RECOMMENDATION}) provide a recommendation message.</dd>
 * </dl>
 */
public class RecommendEnsureOakIndex implements ProgressCheckFactory {
    public static final String NN_OAK_INDEX = "oak:index";
    public static final String CONFIG_SEVERITY = "severity";
    public static final String CONFIG_RECOMMENDATION = "recommendation";
    public static final String CONFIG_SCOPE_PATHS = "scopePaths";
    public static final String DEFAULT_RECOMMENDATION = "We recommend using Ensure Oak Index instead. https://adobe-consulting-services.github.io/acs-aem-commons/features/ensure-oak-index/index.html";

    @Override
    public ProgressCheck newInstance(final JSONObject config) throws Exception {
        final Violation.Severity severity = Violation.Severity.valueOf(config.optString(CONFIG_SEVERITY,
                Violation.Severity.MINOR.name()).toUpperCase());
        final String recommendation = config.optString(CONFIG_RECOMMENDATION, DEFAULT_RECOMMENDATION);
        final List<Rule> scopePaths = Rule.fromJSON(config.optJSONArray(CONFIG_SCOPE_PATHS));
        return new Check(severity, recommendation, scopePaths);
    }

    class Check extends SimpleProgressCheck {
        private final Violation.Severity severity;
        private final String recommendation;
        private final List<Rule> scopePaths;

        public Check(final Violation.Severity severity, final String recommendation, final List<Rule> scopePaths) {
            this.severity = severity;
            this.recommendation = recommendation;
            this.scopePaths = scopePaths;
        }

        @Override
        public String getCheckName() {
            return RecommendEnsureOakIndex.this.getClass().getSimpleName();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {

            // evaluate scope paths
            Rule lastMatched = Rule.fuzzyDefaultAllow(scopePaths);
            for (Rule rule : scopePaths) {
                if (rule.matches(path)) {
                    lastMatched = rule;
                }
            }

            // short circuit if out of scope
            if (lastMatched.isDeny()) {
                return;
            }

            // report for every immediate child of an oak:index node.
            if (NN_OAK_INDEX.equals(node.getParent().getName())) {
                reportViolation(new SimpleViolation(severity,
                        String.format("%s: imported explicit oak:index. %s",
                                path, recommendation), packageId));
            }
        }
    }

}
