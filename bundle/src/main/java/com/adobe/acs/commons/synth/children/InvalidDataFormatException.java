/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.synth.children;

import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

/**
 * Exception indicating the data representing the children is invalid.
 */
@SuppressWarnings("squid:S2166")
public final class InvalidDataFormatException extends RepositoryException {
    public InvalidDataFormatException(final Resource resource, final String propertyName, final String data) {
        super("Property Value in invalid format [ " + resource.getPath() + "/" + propertyName + " = "
                + data + " ]");
    }
}
