/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.forms.impl;

import com.adobe.acs.commons.forms.FormsRouter;
import com.adobe.acs.commons.util.PathInfoUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(label = "ACS AEM Commons - Forms Router",
        description = "Provides functionality for routing ACS AEM Commons FORM Requests through AEM.",
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true)
@Service
public class FormsRouterImpl implements FormsRouter {
    private static final Logger log = LoggerFactory.getLogger(FormsRouterImpl.class);

    private static final String DEFAULT_SUFFIX = "/submit/form";

    private String suffix = DEFAULT_SUFFIX;

    @Property(label = "Forms Suffix",
            description = "HTTP Request Suffix used to identify ACS AEM Commons Forms POST Requests and route them "
                    + "appropriately. [ Default: /submit/form ]",
            value = DEFAULT_SUFFIX)
    private static final String PROP_SUFFIX = "suffix";


    /**
     * Gets the Form Selector for the form POST request.
     *
     * @param slingRequest
     * @return
     */
    @Override
    public final String getFormSelector(final SlingHttpServletRequest slingRequest) {
        final String requestSuffix = slingRequest.getRequestPathInfo().getSuffix();
        if (StringUtils.equals(requestSuffix, this.getSuffix())
                || !StringUtils.startsWith(requestSuffix, this.getSuffix() + "/")) {
            return null;
        }

        final int segments = StringUtils.split(this.getSuffix(), '/').length;
        if (segments < 1) {
            return null;
        }

        final String formSelector = PathInfoUtil.getSuffixSegment(slingRequest, segments);
        return StringUtils.stripToNull(formSelector);
    }

    @Override
    public final String getSuffix() {
        return this.suffix;
    }

    @Override
    public final boolean hasValidSuffix(final SlingHttpServletRequest slingRequest) {
        final String requestSuffix = slingRequest.getRequestPathInfo().getSuffix();
        if (StringUtils.equals(requestSuffix, this.getSuffix())
                || StringUtils.startsWith(requestSuffix, this.getSuffix() + "/")) {
            return true;
        }

        return false;
    }

    @Activate
    protected final void activate(final Map<String, String> properties) {
        this.suffix = PropertiesUtil.toString(properties.get(PROP_SUFFIX), DEFAULT_SUFFIX);
        if (StringUtils.isBlank(this.suffix)) {
            // No whitespace please
            this.suffix = DEFAULT_SUFFIX;
        }

        log.debug("Forms Router suffix: {}", this.suffix);
    }
}
