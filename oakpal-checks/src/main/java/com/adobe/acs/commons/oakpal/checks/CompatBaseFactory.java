/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2019 Adobe
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

    /**
     * Private subclass logger to make it clear which concrete class' deprecated method is being called.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
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
