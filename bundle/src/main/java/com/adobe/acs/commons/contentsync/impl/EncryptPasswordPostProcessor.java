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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.ConfigurationUtils;
import com.adobe.granite.crypto.CryptoSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * Catch create/modify events under <code>/var/acs-commons/contentsync</code> and encrypt the <code>password</code> property
 */
@Component
public class EncryptPasswordPostProcessor implements SlingPostProcessor {
    static final String PASSWORD_PROPERTY = "password";

    @Reference
    private CryptoSupport crypto;

    @Override
    public void process(SlingHttpServletRequest slingRequest, List<Modification> changes) throws Exception {
        for (Modification mod : changes) {
            switch (mod.getType()) {
                case MODIFY:
                case CREATE:
                    String path = mod.getSource();
                    if (path.startsWith(ConfigurationUtils.HOSTS_PATH)) {
                        ModifiableValueMap vm = slingRequest.getResource().adaptTo(ModifiableValueMap.class);

                        String password = vm.get(PASSWORD_PROPERTY, String.class);
                        // encrypt the password property if it is not already protected
                        if(password != null && !crypto.isProtected(password)) {
                            String encrypted = crypto.protect(password);
                            vm.put(PASSWORD_PROPERTY, encrypted);

                            slingRequest.getResourceResolver().commit();
                        }
                    }
            }
        }
    }
}
