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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAuthorizable {

    private static final Logger log = LoggerFactory.getLogger(AbstractAuthorizable.class);

    protected String principalName;
    protected String intermediatePath;
    protected List<Ace> aces = new ArrayList<>();

    public AbstractAuthorizable(Map<String, Object> config) throws EnsureAuthorizableException {
        String tmp = PropertiesUtil.toString(config.get(EnsureServiceUser.PROP_PRINCIPAL_NAME), null);

        if (StringUtils.contains(tmp, "/")) {
            tmp = StringUtils.removeStart(tmp, getDefaultPath());
            tmp = StringUtils.removeStart(tmp, "/");
            this.principalName = StringUtils.substringAfterLast(tmp, "/");
            this.intermediatePath =
                    PathUtil.makePath(getDefaultPath(), StringUtils.removeEnd(tmp, this.principalName));
        } else {
            this.principalName = tmp;
            this.intermediatePath = getDefaultPath();
        }

        // Check the principal name for validity
        if (StringUtils.isBlank(this.principalName)) {
            throw new EnsureAuthorizableException("No Principal Name provided to Ensure Service User");
        } else if (ProtectedAuthorizables.isProtected(this.principalName)) {
            throw new EnsureAuthorizableException(String.format(
                    "[ %s ] is an System User provided by AEM or ACS AEM Commons. You cannot ensure this user.",
                    this.principalName));
        }

        final String[] acesProperty =
                PropertiesUtil.toStringArray(config.get(EnsureServiceUser.PROP_ACES), new String[0]);
        for (String entry : acesProperty) {
            if (StringUtils.isNotBlank(entry)) {
                // issue #1552: trim entry to tolerate osgi config array elements separated by newlines
                final String aceConfig = entry.trim();
                try {
                    aces.add(new Ace(aceConfig));
                } catch (EnsureAuthorizableException e) {
                    log.warn(
                            "Malformed ACE config [ " + aceConfig + " ] for Service User [ "
                                    + StringUtils.defaultIfEmpty(this.principalName, "NOT PROVIDED") + " ]", e);
                }
            }
        }
    }

    public boolean hasAceAt(String path) {
        return getAces().stream().anyMatch(ace -> StringUtils.equals(path, ace.getContentPath()));
    }

    public String getIntermediatePath() {
        return intermediatePath;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public List<Ace> getAces() {
        return aces;
    }

    public Ace getAce(JackrabbitAccessControlEntry actual, String path) throws RepositoryException {
        for (Ace ace : getAces()) {
            if (StringUtils.equals(path, ace.getContentPath()) && ace.isSameAs(actual)) {
                return ace;
            }
        }

        return null;
    }

    public List<Ace> getMissingAces() {
        return getAces().stream().filter(ace -> !ace.isExists()).collect(Collectors.toList());
    }

    public abstract String getDefaultPath();
}
