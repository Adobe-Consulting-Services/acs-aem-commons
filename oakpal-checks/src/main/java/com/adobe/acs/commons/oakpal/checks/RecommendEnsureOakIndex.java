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
import org.apache.jackrabbit.vault.packaging.PackageId;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static net.adamcin.oakpal.api.JavaxJson.arrayOrEmpty;

/**
 * Report explicitly-imported oak:index nodes as violations to encourage migration to ACS AEM Commons - Ensure Oak Index.
 * {@code config} items:
 * <dl>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.api.Severity#MINOR}) specify the severity of violations reported
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
public final class RecommendEnsureOakIndex implements ProgressCheckFactory {
    public static final String NN_OAK_INDEX = "oak:index";
    public static final String CONFIG_SEVERITY = "severity";
    public static final String CONFIG_RECOMMENDATION = "recommendation";
    public static final String CONFIG_SCOPE_PATHS = "scopePaths";

    /**
     * This constant value is referenced as an i18n key in {@link RecommendEnsureOakIndex}.properties.
     */
    public static final String DEFAULT_RECOMMENDATION = "DEFAULT_RECOMMENDATION";

    @Override
    public ProgressCheck newInstance(final JsonObject config) {
        final Severity severity = Severity.valueOf(config.getString(CONFIG_SEVERITY,
                Severity.MINOR.name()).toUpperCase());
        final String recommendation = config.getString(CONFIG_RECOMMENDATION, DEFAULT_RECOMMENDATION);
        final List<Rule> scopePaths = Rules.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_PATHS));
        return new Check(severity, recommendation, scopePaths);
    }

    static final class Check extends SimpleProgressCheckFactoryCheck<RecommendEnsureOakIndex> {
        private final Severity severity;
        private final String recommendation;
        private final List<Rule> scopePaths;

        Check(final Severity severity, final String recommendation, final List<Rule> scopePaths) {
            super(RecommendEnsureOakIndex.class);
            this.severity = severity;
            this.recommendation = recommendation;
            this.scopePaths = new ArrayList<>(scopePaths);
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {

            // evaluate scope paths
            Rule lastMatched = Rules.lastMatch(scopePaths, path);

            // short circuit if out of scope
            if (lastMatched.isExclude()) {
                return;
            }

            // report for every immediate child of an oak:index node.
            if (NN_OAK_INDEX.equals(node.getParent().getName())) {
                reporting(violation -> violation.withSeverity(severity)
                        .withPackage(packageId)
                        .withDescription("{0}: imported explicit oak:index. {1}")
                        .withArgument(path, recommendation));
            }
        }
    }
}
