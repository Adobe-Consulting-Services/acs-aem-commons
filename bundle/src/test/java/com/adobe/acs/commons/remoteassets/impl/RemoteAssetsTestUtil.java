/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.remoteassets.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.serviceusermapping.impl.MappingConfigAmendment;

import io.wcm.testing.mock.aem.junit.AemContext;

public class RemoteAssetsTestUtil {
    public static final String TEST_SERVER_URL = "https://remote-aem-server:4502";
    public static final String TEST_SERVER_USERNAME = "admin";
    public static final String TEST_SERVER_PASSWORD = "passwd";
    public static final String TEST_TAGS_PATH_A = "/content/cq:tags/a";
    public static final String TEST_TAGS_PATH_B = "/content/cq:tags/b";
    public static final String TEST_DAM_PATH_A = "/content/dam/a";
    public static final String TEST_DAM_PATH_B = "/content/dam/b";
    public static final int TEST_RETRY_DELAY = 30;
    public static final int TEST_SAVE_INTERVAL = 500;
    public static final String TEST_WHITELISTED_SVC_USER_A = "user_a";
    public static final String TEST_WHITELISTED_SVC_USER_B = "user_b";

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        byte[] fileBytes = null;
        try {
            fileBytes = IOUtils.toByteArray(inputStream);
        } finally {
            inputStream.close();
        }
        return fileBytes;
    }

    public static byte[] getPlaceholderAsset(String fileExt) throws IOException {
        return getBytes(ClassLoader.getSystemResourceAsStream("remoteassets/remote_asset." + fileExt));
    }

    public static Map<String, Object> getRemoteAssetsConfigs() {
        Map<String, Object> remoteAssetsConfigs = new HashMap<>();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL);
        remoteAssetsConfigs.put("server.user", TEST_SERVER_USERNAME);
        remoteAssetsConfigs.put("server.pass", TEST_SERVER_PASSWORD);
        remoteAssetsConfigs.put("server.insecure", false);
        remoteAssetsConfigs.put("tag.paths", new String[]{TEST_TAGS_PATH_A, "", TEST_TAGS_PATH_B});
        remoteAssetsConfigs.put("dam.paths", new String[]{TEST_DAM_PATH_A, "", TEST_DAM_PATH_B});
        remoteAssetsConfigs.put("retry.delay", TEST_RETRY_DELAY);
        remoteAssetsConfigs.put("save.interval", TEST_SAVE_INTERVAL);
        remoteAssetsConfigs.put("whitelisted.service.users", new String[]{TEST_WHITELISTED_SVC_USER_A, "", TEST_WHITELISTED_SVC_USER_B});

        return remoteAssetsConfigs;
    }

    public static void setupRemoteAssetsServiceUser(AemContext context) throws RepositoryException {
        Map<String, Object> serviceUserMapperConfig = new HashMap<>();
        serviceUserMapperConfig.put("user.mapping", context.bundleContext().getBundle().getSymbolicName() + ":remote-assets=[acs-commons-remote-assets-service]");
        context.registerInjectActivateService(new MappingConfigAmendment(), serviceUserMapperConfig);
    }
}
