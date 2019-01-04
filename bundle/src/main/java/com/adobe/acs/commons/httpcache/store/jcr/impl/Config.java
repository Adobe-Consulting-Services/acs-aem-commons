/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "ACS AEM Commons - Http Cache - JCR Cache Store.",
        description = "Cache data store implementation for JCR storage.")
public @interface Config {

    //By default, we go for the maximum bucket depth. This uses the full hashcode of 10 digits.
    int DEFAULT_BUCKETDEPTH = 10;

    //Perform a save on a delta of 500 by default.
    int DEFAULT_SAVEDELTA = 500;

    // 1 week.
    long DEFAULT_EXPIRETIMEINMILISECONDS = 604800;

    String DEFAULT_ROOTPATH = "/var/acs-commons/httpcache";
    String DEFAULT_CRON_EXPRESSION = "0 0 12 1/1 * ? *";

    @AttributeDefinition(
            name = "Cache clean-up schedule",
            description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
            defaultValue = DEFAULT_CRON_EXPRESSION
    )
    String scheduler_expression() default DEFAULT_CRON_EXPRESSION;

    @AttributeDefinition(
            name = "Cache bucketing tree depth",
            description = "The depth the bucket tree goes. Minimum value is 1. "
                    + "This value can be used for tweaking performance. "
                    + "The more data cached, the higher this value should be. "
                    + "Downside is that the higher the value, the longer the retrieval of cache entries takes if the buckets are relatively low on entries.",
            defaultValue = "" + DEFAULT_BUCKETDEPTH
    )
    int httpcache_config_jcr_bucketdepth() default DEFAULT_BUCKETDEPTH;


    @AttributeDefinition(
            name = "Cache-root Parent Path location",
            description = "Points to the location of the cache root parent node in the JCR repository",
            defaultValue = DEFAULT_ROOTPATH
    )
    String httpcache_config_jcr_rootpath() default DEFAULT_ROOTPATH;

    @AttributeDefinition(
            name = "Save threshold",
            description = "The threshold to add,remove and modify nodes when handling the cache",
            defaultValue = "" + DEFAULT_SAVEDELTA
    )
    int httpcache_config_jcr_savedelta() default DEFAULT_SAVEDELTA;

    @AttributeDefinition(
            name = "Expire time in miliseconds",
            description = "The time in miliseconds after which nodes will be removed by the scheduled cleanup service. ",
            defaultValue = "" + DEFAULT_EXPIRETIMEINMILISECONDS
    )
    long httpcache_config_jcr_expiretimeinmiliseconds() default DEFAULT_EXPIRETIMEINMILISECONDS;
}
