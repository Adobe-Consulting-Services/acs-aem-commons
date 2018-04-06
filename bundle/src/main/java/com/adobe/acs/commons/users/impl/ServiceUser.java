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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceUser extends AbstractAuthorizable {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ServiceUser.class);

    private static final String PATH_SYSTEM_USERS = "/home/users/system";

    public ServiceUser(Map<String, Object> config) throws EnsureAuthorizableException {
        super(config);
    }

    @Override
    public String getDefaultPath() {
        return PATH_SYSTEM_USERS;
    }
}