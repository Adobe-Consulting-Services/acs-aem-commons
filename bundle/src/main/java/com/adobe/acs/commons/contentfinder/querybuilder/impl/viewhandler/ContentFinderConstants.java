package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

public class ContentFinderConstants {

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

    static final String[] QUERYBUILDER_BLACKLIST = new String[] { "_dc", "ck", "_charset_", "wcmmode", CF_MIMETYPE, CF_FULLTEXT, CF_LIMIT, CONVERT_TO_QUERYBUILDER_KEY };
    static final String[] PROPERTY_BLACKLIST = new String[] { "_dc", "ck", "_charset_", "wcmmode", CF_FULLTEXT, CF_LIMIT, CONVERT_TO_QUERYBUILDER_KEY };

}
