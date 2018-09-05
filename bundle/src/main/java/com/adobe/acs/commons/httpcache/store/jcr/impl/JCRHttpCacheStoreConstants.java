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

public class JCRHttpCacheStoreConstants
{
    public static final String ROOT_NODE_NAME = "root";

    public static final String PATH_CONTENTS = "contents";
    public static final String PATH_HEADERS = "headers";
    public static final String PATH_ENTRY       = "entry";

    public static final String PN_CACHEKEY = "cacheKeySerialized";
    public static final String PN_ISCACHEENTRYNODE = "isCacheEntryNode";
    public static final String PN_ISBUCKETNODE = "isBucketNode";

    public static final String PN_EXPIRES_ON = "expiresOn";
    public static final String PN_STATUS = "status";
    public static final String PN_CHAR_ENCODING = "char-encoding";
    public static final String PN_CONTENT_TYPE = "content-type";

    public static final String OAK_UNSTRUCTURED = "oak:Unstructured";

    private JCRHttpCacheStoreConstants(){

    }
}
