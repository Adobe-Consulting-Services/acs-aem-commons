/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.acs.commons.contentsync.impl.LastModifiedStrategy;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.adobe.acs.commons.contentsync.RemoteInstance.CONNECT_TIMEOUT;
import static com.adobe.acs.commons.contentsync.RemoteInstance.SOCKET_TIMEOUT;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;

public class ConfigurationUtils {
    public static final String CONFIG_PATH = "/var/acs-commons/contentsync";
    public static final String SETTINGS_PATH = CONFIG_PATH + "/settings";
    public static final String HOSTS_PATH = CONFIG_PATH + "/hosts";

    public static final String UPDATE_STRATEGY_KEY = "update-strategy";
    public static final String EVENT_USER_DATA_KEY = "event-user-data";
    public static final String SO_TIMEOUT_STRATEGY_KEY = "soTimeout";
    public static final String CONNECT_TIMEOUT_KEY = "connTimeout";
    public static final String DISABLE_CERT_CHECK_KEY = "disableCertCheck";

    private ConfigurationUtils(){

    }

    public static Resource getSettingsResource(ResourceResolver resourceResolver) throws PersistenceException {
        Map<String, Object> resourceProperties = new HashMap<>();
        resourceProperties.put(JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
        resourceProperties.put(UPDATE_STRATEGY_KEY, LastModifiedStrategy.class.getName());
        resourceProperties.put(EVENT_USER_DATA_KEY, "changedByPageManagerCopy");
        resourceProperties.put(SO_TIMEOUT_STRATEGY_KEY, SOCKET_TIMEOUT);
        resourceProperties.put(CONNECT_TIMEOUT_KEY, CONNECT_TIMEOUT);
        return ResourceUtil.getOrCreateResource(resourceResolver, SETTINGS_PATH, resourceProperties, NT_SLING_FOLDER, true);
    }

    public static Resource getHostsResource(ResourceResolver resourceResolver) throws PersistenceException {
        Map<String, Object> resourceProperties = new HashMap<>();
        resourceProperties.put(JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
        return ResourceUtil.getOrCreateResource(resourceResolver, HOSTS_PATH, resourceProperties, NT_SLING_FOLDER, true);
    }

    public static void persistAuditLog(ResourceResolver resourceResolver, String path, long count, String data) throws PersistenceException {

        String auditHome = CONFIG_PATH + "/audit";
        ResourceUtil.getOrCreateResource(resourceResolver, auditHome,
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED), JcrConstants.NT_FOLDER, false);

        String auditResourcePath = auditHome + "/" + UUID.randomUUID();
        Map<String, Object> auditProps = new HashMap<>();
        auditProps.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        auditProps.put("syncPath", path);
        auditProps.put(JcrConstants.JCR_MIXINTYPES, "mix:created");
        auditProps.put("count", count);
        ResourceUtil.getOrCreateResource(resourceResolver, auditResourcePath, auditProps, null, false);

        String auditLogPath = auditResourcePath + "/log";
        ResourceUtil.getOrCreateResource(resourceResolver, auditLogPath,
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE), null, false);

        Map<String, Object> props = new HashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        props.put(JcrConstants.JCR_MIMETYPE, "text/plain");

        props.put(JcrConstants.JCR_DATA, new ByteArrayInputStream(data.getBytes()));

        ResourceUtil.getOrCreateResource(resourceResolver, auditLogPath + "/" + JcrConstants.JCR_CONTENT,
                props, null, false);

        resourceResolver.commit();
    }
}
