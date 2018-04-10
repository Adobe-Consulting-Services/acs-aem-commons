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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;

@Component
@Properties({ @Property(label = "MBean Name", name = "jmx.objectname",
        value = "com.adobe.acs.commons:type=Ensure Service User", propertyPrivate = true) })
@References({ @Reference(referenceInterface = EnsureAuthorizable.class, policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE) })
@Service(value = DynamicMBean.class)
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

    protected final void bindEnsureAuthorizable(final EnsureAuthorizable service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get("service.pid"), null);
        if (type != null) {
            this.ensureAuthorizables.put(type, service);
        }
    }

    protected final void unbindEnsureAuthorizable(final EnsureAuthorizable service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get("service.pid"), null);
        if (type != null) {
            this.ensureAuthorizables.remove(type);
        }
    }
}
