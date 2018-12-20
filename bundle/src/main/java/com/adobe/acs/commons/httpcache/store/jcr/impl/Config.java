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

    @AttributeDefinition(
            name = "Cache clean-up schedule",
            description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
            defaultValue = "0 0 12 1/1 * ? *"
    )
    String scheduler_expression();

    @AttributeDefinition(
            name = "Cache bucketing tree depth",
            description = "The depth the bucket tree goes. Minimum value is 1. "
                    + "This value can be used for tweaking performance. "
                    + "The more data cached, the higher this value should be. "
                    + "Downside is that the higher the value, the longer the retrieval of cache entries takes if the buckets are relatively low on entries.",
            defaultValue = "" + DEFAULT_BUCKETDEPTH

    )
    int httpcache_config_jcr_bucketdepth();


    @AttributeDefinition(
            name = "Cache-root Parent Path location",
            description = "Points to the location of the cache root parent node in the JCR repository",
            defaultValue = DEFAULT_ROOTPATH
    )
    String httpcache_config_jcr_rootpath();

    @AttributeDefinition(
            name = "Save threshold",
            description = "The threshold to add,remove and modify nodes when handling the cache",
            defaultValue = "" + DEFAULT_SAVEDELTA
    )
    int httpcache_config_jcr_savedelta();

    @AttributeDefinition(
            name = "Expire time in miliseconds",
            description = "The time in miliseconds after which nodes will be removed by the scheduled cleanup service. ",
            defaultValue = "" + DEFAULT_EXPIRETIMEINMILISECONDS
    )
    long httpcache_config_jcr_expiretimeinmiliseconds();
}
