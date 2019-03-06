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
 */
abstract class CompatBaseFactory implements ProgressCheckFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public final ProgressCheck newInstance(final JSONObject config) {
        logger.info("ProgressCheckFactory.newInstance(org.json.JSONObject config) is deprecated as of 1.2.0.");
        logger.info("Please use newInstance(javax.json.JsonObject config) instead.");
        return newInstance(JavaxJson.wrap(config.toMap()).asJsonObject());
    }

    @Override
    public abstract ProgressCheck newInstance(final JsonObject config);

}
