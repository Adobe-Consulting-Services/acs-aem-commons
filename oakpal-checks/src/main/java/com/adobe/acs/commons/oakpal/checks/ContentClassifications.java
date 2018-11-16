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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.util.Text;
import org.json.JSONObject;

/**
 * Enforce rules for "Content Classifications" in
 * <a href="https://helpx.adobe.com/experience-manager/6-4/sites/deploying/using/sustainable-upgrades.html#ContentClassifications">Sustainable Upgrades</a>.
 * <p>
 * This check assumes that all the reference nodes under /libs have been provided by forced roots or pre-install packages.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code libsPathPrefix}</dt>
 * <dd>(default: {@code /libs}) specify where to look for granite protected areas.</dd>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}) specify the severity of violations reported
 * by this check.</dd>
 * <dt>{@code scopePaths} (type: {@link Rule[]})</dt>
 * <dd>(default: allow all) specify a list of pattern rules to allow or deny import paths from the scope of this check.
 * When no pattern rules are specified, ALLOW ALL imported paths into scope for the check. When the first rule is type
 * ALLOW, the default first rule becomes DENY ALL.
 * </dd>
 * </dl>
 */
public final class ContentClassifications implements ProgressCheckFactory {
    private static final String P_SLING_RESOURCE_TYPE = "{http://sling.apache.org/jcr/sling/1.0}resourceType";
    private static final String P_SLING_RESOURCE_SUPER_TYPE = "{http://sling.apache.org/jcr/sling/1.0}resourceSuperType";
    private static final String T_GRANITE_PUBLIC_AREA = "{http://www.adobe.com/jcr/granite/1.0}PublicArea";
    private static final String T_GRANITE_ABSTRACT_AREA = "{http://www.adobe.com/jcr/granite/1.0}AbstractArea";
    private static final String T_GRANITE_FINAL_AREA = "{http://www.adobe.com/jcr/granite/1.0}FinalArea";
    private static final String T_GRANITE_INTERNAL_AREA = "{http://www.adobe.com/jcr/granite/1.0}InternalArea";
    private static final String LIBS_PATH_PREFIX = "/libs";

    private static final String CONFIG_LIBS_PATH_PREFIX = "libsPathPrefix";
    private static final String CONFIG_SEVERITY = "severity";
    private static final String CONFIG_SCOPE_PATHS = "scopePaths";

    @Override
    public ProgressCheck newInstance(final JSONObject jsonObject) {
        final String libsPathPrefix = jsonObject.optString(CONFIG_LIBS_PATH_PREFIX, LIBS_PATH_PREFIX);
        final Violation.Severity severity = Violation.Severity.valueOf(jsonObject.optString(CONFIG_SEVERITY,
                Violation.Severity.MAJOR.name()).toUpperCase());
        final List<Rule> scopePaths = Rule.fromJSON(jsonObject.optJSONArray(CONFIG_SCOPE_PATHS));

        return new Check(libsPathPrefix, severity, scopePaths);
    }

    class Check extends SimpleProgressCheck {
        final String libsPathPrefix;
        final Violation.Severity severity;
        final List<Rule> scopePaths;

        public Check(final String libsPathPrefix, final Violation.Severity severity, final List<Rule> scopePaths) {
            this.libsPathPrefix = libsPathPrefix;
            this.severity = severity;
            this.scopePaths = scopePaths;
        }

        @Override
        public String getCheckName() {
            return ContentClassifications.this.getClass().getSimpleName();
        }

        @Override
        public void importedPath(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {

            // short circuit if we happen to be installing /libs content. we won't check for
            // those violations here.
            if (path.startsWith(libsPathPrefix)) {
                return;
            }

            // default to ALLOW ALL
            // if first rule is allow, change default to DENY ALL
            Rule lastMatched = scopePaths.isEmpty() || scopePaths.get(0).isDeny()
                    ? Rule.DEFAULT_ALLOW
                    : Rule.DEFAULT_DENY;
            for (Rule rule : scopePaths) {
                if (rule.matches(path)) {
                    lastMatched = rule;
                }
            }

            // if path is denied from scope, short circuit.
            if (lastMatched.isDeny()) {
                return;
            }

            // check sling:resourceType against libsPathPrefix.
            checkResourceType(packageId, path, node);

            // check sling:resourceSuperType against libsPathPrefix.
            checkResourceSuperType(packageId, path, node);

        }

        void checkResourceType(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {
            if (node.hasProperty(P_SLING_RESOURCE_TYPE)) {
                String rt = node.getProperty(P_SLING_RESOURCE_TYPE).getString();
                if (rt.length() > 0) {
                    final String libsRt;
                    if (rt.startsWith("/")) {
                        libsRt = rt;
                    } else {
                        libsRt = libsPathPrefix + "/" + rt;
                    }

                    assertClassifications(node.getSession(), libsRt, AreaType.ALLOWED_FOR_RESOURCE_TYPE)
                            .ifPresent(message -> reportViolation(new SimpleViolation(severity,
                                    String.format("%s [protected resource type]: %s", path, message),
                                    packageId)));
                }
            }
        }

        void checkResourceSuperType(final PackageId packageId, final String path, final Node node)
                throws RepositoryException {
            if (node.hasProperty(P_SLING_RESOURCE_SUPER_TYPE)) {
                String rst = node.getProperty(P_SLING_RESOURCE_SUPER_TYPE).getString();
                if (rst.length() > 0) {
                    final String libsRst;
                    if (rst.startsWith("/")) {
                        libsRst = rst;
                    } else {
                        libsRst = libsPathPrefix + "/" + rst;
                    }

                    assertClassifications(node.getSession(), libsRst, AreaType.ALLOWED_FOR_RESOURCE_SUPER_TYPE)
                            .ifPresent(message -> reportViolation(new SimpleViolation(severity,
                                    String.format("%s [protected super type]: %s", path, message),
                                    packageId)));
                }
            }
        }

        Optional<String> assertClassifications(final Session session, final String libsPath,
                                               final Set<AreaType> allowedAreas) throws RepositoryException {
            final Node leaf = getLibsLeaf(session, libsPath);
            if (leaf != null) {
                final AreaType leafArea = AreaType.fromNode(leaf);
                if (leafArea == AreaType.FINAL && libsPath.startsWith(leaf.getPath() + "/")) {
                    return Optional.of(String.format("%s is implicitly marked %s", libsPath, AreaType.INTERNAL));
                }
                if (!allowedAreas.contains(leafArea)) {
                    return Optional.of(String.format("%s is marked %s", leaf.getPath(), leafArea));
                }
            }

            return Optional.empty();
        }

        Node getLibsLeaf(final Session session, final String absPath) throws RepositoryException {
            if ("/".equals(absPath)) {
                return session.getRootNode();
            } else if (absPath.startsWith(libsPathPrefix)) {
                final String parentPath = Text.getRelativeParent(absPath, 1, true);
                Node parent = getLibsLeaf(session, parentPath);
                if (parent != null) {
                    final String name = Text.getName(absPath, true);
                    if (parent.hasNode(name)) {
                        return parent.getNode(name);
                    } else {
                        return parent;
                    }
                }
            }
            return null;
        }
    }

    public enum AreaType {
        PUBLIC(T_GRANITE_PUBLIC_AREA),
        ABSTRACT(T_GRANITE_ABSTRACT_AREA),
        FINAL(T_GRANITE_FINAL_AREA),
        INTERNAL(T_GRANITE_INTERNAL_AREA);
        final String mixinType;

        AreaType(final String mixinType) {
            this.mixinType = mixinType;
        }

        public static Set<AreaType> ALLOWED_FOR_RESOURCE_TYPE = new HashSet<>(Arrays.asList(PUBLIC, FINAL));
        public static Set<AreaType> ALLOWED_FOR_RESOURCE_SUPER_TYPE = new HashSet<>(Arrays.asList(PUBLIC, ABSTRACT));

        public static final AreaType fromNode(final Node node) throws RepositoryException {
            for (AreaType value : values()) {
                if (node.isNodeType(value.mixinType)) {
                    return value;
                }
            }
            if (node.getSession().getRootNode().isSame(node)) {
                return PUBLIC;
            } else {
                AreaType parentType = fromNode(node.getParent());
                if (parentType == FINAL) {
                    return INTERNAL;
                } else {
                    return parentType;
                }
            }
        }
    }
}
