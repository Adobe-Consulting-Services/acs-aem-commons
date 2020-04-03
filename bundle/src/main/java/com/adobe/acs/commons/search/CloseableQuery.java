/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.search;

import java.io.Closeable;
import java.util.Iterator;

import javax.jcr.Session;

import com.day.cq.search.Query;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CQ Search Query retains a cheap internal {@link org.apache.sling.api.resource.ResourceResolver} that wraps a JCR
 * session after any iteration over search result Hits, which can produce much stack trace in error.log.
 *
 * This extension closes the retained resolver via the {@link Closeable#close()} method for use in a try-with-resources
 * block.
 *
 * Because {@link ResourceResolver#close()} calls {@link Session#logout()} on the underlying session, it is important that
 * the logout method be guarded by a session wrapper. (This is done automatically by the {@link CloseableQueryBuilder}
 * service.)
 */
@ProviderType
public interface CloseableQuery extends Query, Closeable {

    @Override
    default void close() {
        Iterator<Resource> resourceIterator = getResult().getResources();
        if (resourceIterator.hasNext()) {
            resourceIterator.next().getResourceResolver().close();
        }
    }
}
