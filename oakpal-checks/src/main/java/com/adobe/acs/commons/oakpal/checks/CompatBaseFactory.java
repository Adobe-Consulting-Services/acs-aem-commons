package com.adobe.acs.commons.oakpal.checks;

import javax.json.JsonObject;

import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary Shim to facilitate deprecation of usages of org.json in favor of javax.json.
 *
 * @deprecated 4.0.0 this base class will be removed after oakpal-core drops support for org.json
 */
@Deprecated
abstract class CompatBaseFactory implements ProgressCheckFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Create a new instance of a ProgressCheck using the provided config object.
     * <p>
     * Legacy method implementation which indicates the deprecation in the logger and delegates to
     * {@link #newInstance(JsonObject)}.
     *
     * @param config the org.json.JSONObject config
     * @return the new ProgressCheck
     * @deprecated 4.0.0 migrate usages of {@link #newInstance(JSONObject)} to {@link #newInstance(JsonObject)}
     */
    @Deprecated
    @Override
    public final ProgressCheck newInstance(final JSONObject config) {
        logger.info("ProgressCheckFactory.newInstance(org.json.JSONObject config) is deprecated as of 1.2.0.");
        logger.info("Please use newInstance(javax.json.JsonObject config) instead.");
        return newInstance(JavaxJson.wrap(config.toMap()).asJsonObject());
    }

    /**
     * Create a new instance of a ProgressCheck using the provided config object.
     * <p>
     * Overrides default interface method, which currently exists solely to facilitate deprecation of org.json, to force
     * implementation in our own subclasses.
     *
     * @param config the config object
     * @return the new ProgressCheck
     */
    @Override
    public abstract ProgressCheck newInstance(final JsonObject config);

}
