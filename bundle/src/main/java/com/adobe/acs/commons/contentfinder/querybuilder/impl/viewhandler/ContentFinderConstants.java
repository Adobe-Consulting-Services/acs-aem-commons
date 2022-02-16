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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

public final class ContentFinderConstants {

    private ContentFinderConstants() {
    }

    public static final String CONVERT_TO_QUERYBUILDER_KEY = "_ctqb";
    public static final String CONVERT_TO_QUERYBUILDER_VALUE = "true";

    public static final String DELIMITER = ",";

    public static final String CF_TYPE = "type";
    public static final String CF_PATH = "path";
    public static final String CF_FULLTEXT = "query";
    public static final String CF_MIMETYPE = "mimeType";
    public static final String CF_ORDER = "order";
    public static final String CF_LIMIT = "limit";
    public static final String CF_OFFSET = "offset";
    public static final String CF_NAME = "name";
    public static final String CF_TAGS = "tags";
    public static final String CF_TITLE = "title";
    public static final String CF_EXCERPT = "excerpt";
    public static final String CF_DD_GROUPS = "ddGroups";
    public static final String CF_LAST_MODIFIED = "lastModified";
    public static final String CF_SIZE = "size";
    public static final String CF_CACHE_KILLER = "ck";

    public static final int GROUP_PATH = 1;
    public static final int GROUP_TYPE = 2;
    public static final int GROUP_NAME = 3;
    public static final int GROUP_MIMETYPE = 4;
    public static final int GROUP_TAGS = 5;
    public static final int GROUP_FULLTEXT = 6;

    public static final int GROUP_ORDERBY_USERDEFINED = 110;
    public static final int GROUP_ORDERBY_SCORE = 100;
    public static final int GROUP_ORDERBY_MODIFIED = 101;

    public static final int GROUP_PROPERTY_USERDEFINED = 10000;

    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_LIMIT = 20;

    static final String[] QUERYBUILDER_BLACKLIST = new String[] {
            "_dc", "ck", "_charset_", "wcmmode",
            CF_MIMETYPE, CF_FULLTEXT, CF_LIMIT, CONVERT_TO_QUERYBUILDER_KEY
    };
    static final String[] PROPERTY_BLACKLIST = new String[] {
            "_dc", "ck", "_charset_", "wcmmode", CF_FULLTEXT,
            CF_LIMIT, CONVERT_TO_QUERYBUILDER_KEY
    };

}
