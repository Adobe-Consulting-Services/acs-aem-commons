/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2026 Adobe
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

package com.adobe.acs.commons.users.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;

@Component(
        service = DynamicMBean.class,
        property = {
                "jmx.objectname=com.adobe.acs.commons:type=Ensure Service User"
        }
)
public class EnsureAuthorizableManagerImpl extends AnnotatedStandardMBean implements EnsureAuthorizableManager {

    private static final Logger log = LoggerFactory.getLogger(EnsureAuthorizableManagerImpl.class);

    private Map<String, EnsureAuthorizable> ensureAuthorizables = new ConcurrentHashMap<>();

    public EnsureAuthorizableManagerImpl() throws NotCompliantMBeanException {
        super(EnsureAuthorizableManager.class);
    }

    @Override
    public final void ensureAll() {
        for (final EnsureAuthorizable ensureAuthorizable : ensureAuthorizables.values()) {
            try {
                ensureAuthorizable.ensure(ensureAuthorizable.getOperation(), ensureAuthorizable.getAuthorizable());
            } catch (EnsureAuthorizableException e) {
                log.error("Error Ensuring Authorizable [ {} ]", ensureAuthorizable.getAuthorizable()
                        .getPrincipalName(), e);
            }
        }
    }

    @Override
    public final void ensurePrincipalName(String principalName) {
        for (final EnsureAuthorizable ensureAuthorizable : ensureAuthorizables.values()) {
            if (StringUtils.equals(principalName, ensureAuthorizable.getAuthorizable().getPrincipalName())) {
                try {
                    ensureAuthorizable.ensure(ensureAuthorizable.getOperation(), ensureAuthorizable.getAuthorizable());
                } catch (EnsureAuthorizableException e) {
                    log.error("Error Ensuring Authorizable [ {} ]", ensureAuthorizable.getAuthorizable()
                            .getPrincipalName(), e);
                }
            }
        }
    }

    @Reference(
            service = EnsureAuthorizable.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    protected final void bindEnsureAuthorizable(final EnsureAuthorizable service, final Map<Object, Object> props) {
        final String type = (String) props.get("service.pid");
        if (type != null) {
            this.ensureAuthorizables.put(type, service);
        }
    }

    protected final void unbindEnsureAuthorizable(final EnsureAuthorizable service, final Map<Object, Object> props) {
        final String type = (String) props.get("service.pid");
        if (type != null) {
            this.ensureAuthorizables.remove(type);
        }
    }
}
