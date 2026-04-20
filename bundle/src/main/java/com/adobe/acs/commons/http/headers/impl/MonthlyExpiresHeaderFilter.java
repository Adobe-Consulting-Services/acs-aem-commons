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

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "webconsole.configurationFactory.nameHint=Expires each month on the {expires.day-of-month} day at {expires.time} for Patterns: [{filter.pattern}]"
})
@Designate(ocd = MonthlyExpiresHeaderFilter.Config.class, factory = true)
public class MonthlyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    @ObjectClassDefinition(name = "ACS AEM Commons - Cache Expires Header - Monthly", description = "Adds an Expires header to responses (for example to enable Dispatcher TTL support).")
    public @interface Config {
        @AttributeDefinition(name = "Filter Patterns", description = "Restricts adding the headers to request paths which match any of the supplied regular expression patterns.", cardinality = Integer.MAX_VALUE, min = "1")
        String[] filter_pattern() default {};

        @AttributeDefinition(name = "Expires Time", description = "Time of day at which response expires. Must match SimpleDateFormat of \"HH:mm\".")
        String expires_time();

        @AttributeDefinition(name = "Expires Day", description = "Day of month on which response expires. Use keyword 'LAST' to enable last day of month, as setting to 31 will generate errors in February.")
        String expires_day$_$of$_$month();

        @AttributeDefinition(name = "Allow Authorized Requests", description = "If the header should be added also to authorized requests (carrying a \"Authorization\" header, or cookie with name \"login-token\" or \"authorizization\").")
        boolean allow_authorized() default true;

        @AttributeDefinition(name = "Allow All Parameters", description = "If the header should be added also to requests carrying any parameters except for those given in \"block.params\".")
        boolean allow_all_params() default false;

        @AttributeDefinition(name = "Disallowed Parameter Name", description = "List of request parameter names that are not allowed to be present for the header to be added. Only relevant if \"allow.all.params\" is true.", cardinality = Integer.MAX_VALUE)
        String[] block_params() default {};

        @AttributeDefinition(name = "Allow Parameter Names", description = "List of request parameter names that are allowed to be present for the header to be added. Only relevant if \"allow.all.params\" is false.", cardinality = Integer.MAX_VALUE)
        String[] pass_through_params() default {};

        @AttributeDefinition(name = "Allow Non-Dispatcher Requests", description = "If the header should be added also to requests not coming from a dispatcher (i.e. requests not carrying the \"Server-Agent\" header containing value \"Communique-Dispatcher\").")
        boolean allow_nondispatcher() default false;

        @AttributeDefinition(name = "Service Ranking", description = "Service Ranking for the OSGi service.")
        int service_ranking() default 0;
    }

    private final Config config;
    private static final String LAST = "LAST";

    @Activate
    public MonthlyExpiresHeaderFilter(Config config, BundleContext bundleContext) {
        super(config.expires_time(), new AbstractCacheHeaderFilter.ServletRequestPredicates(config.filter_pattern(), config.allow_all_params(), config.block_params(), config.pass_through_params(), config.allow_authorized(), config.allow_nondispatcher()), config.service_ranking(), bundleContext);
        this.config = config;
        if (!StringUtils.equalsIgnoreCase(LAST, config.expires_day$_$of$_$month())) {
            // Make sure it's a valid value for Calendar.
            try {
                int intDay = Integer.parseInt(config.expires_day$_$of$_$month());
                Calendar test = Calendar.getInstance();
                if (intDay < test.getMinimum(Calendar.DAY_OF_MONTH)) {
                    throw new IllegalArgumentException("Day of month is smaller than minimum allowed value.");
                }
                if (intDay > test.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    throw new IllegalArgumentException("Day of month is larger than least maximum allowed value.");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Day of month is not a valid value.");
            }
        }
    }

    @Override
    protected void adjustExpires(Calendar next) {
        if (StringUtils.equalsIgnoreCase(LAST, config.expires_day$_$of$_$month())) {
            next.set(Calendar.DAY_OF_MONTH, next.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            next.set(Calendar.DAY_OF_MONTH, Integer.parseInt(config.expires_day$_$of$_$month()));
        }
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.MONTH, 1);
        }
    }
}
