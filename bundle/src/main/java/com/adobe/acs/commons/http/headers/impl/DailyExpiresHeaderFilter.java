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
package com.adobe.acs.commons.http.headers.impl;


import java.util.Calendar;


//@formatter:off
import org.osgi.service.component.annotations.Component;
@Component(
    label = "ACS AEM Commons - Dispatcher Expires Header - Daily",
    description = "Adds an Expires header to content to enable Dispatcher TTL support.",
    metatype = true,
    configurationFactory = true,
    policy = ConfigurationPolicy.REQUIRE)
//@formatter:on
public class DailyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    @Override
    protected void adjustExpires(Calendar next) {
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
