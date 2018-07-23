package com.adobe.acs.commons.adobeio.core.internal;

/**
 * Class defining commonly used constants.
 */
public final class Constants {

    private Constants() {
    }
    /**
     * JSON Key for the extra clients
     */
    public static final String PN_EXTRA_CLIENTS = "extraclients";

    /**
     * Name of the Path-entry in json
     */
    public static final String PN_PATH = "path";

    /**
     * Name of exporter used for exporting components properties.
     */
    public static final String EXPORTER_NAME = "jackson";

    /**
     * Extension to register {@link org.apache.sling.models.annotations.Exporter} named {@link #EXPORTER_NAME}
     */
    public static final String EXPORTER_EXTENSION = "json";

    /**
     * Request-parameter containing the redirect-path
     */
    public static final String PN_REDIRECT = ":redirect";

    /**
     * Request-parameter containing the redirect-path
     */
    public static final String PN_URL = "url";

    /**
     * JSON parameter containing mainclient data
     */
    public static final String PN_MAINCLIENT = "mainclient";

}
