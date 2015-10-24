/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum;

import aQute.bnd.annotation.ProviderType;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Map;

/**
 * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types
 * (via {@link ChecksumGeneratorOptions}).
 */
@ProviderType
public interface ChecksumGenerator {
    /**
     * Convenience method for generateChecksums(session, path, new DefaultChecksumGeneratorOptions()).
     *
     * @param session the session
     * @param path    the root path to generate checksums for
     * @return the map of abs path ~> checksums
     * @throws RepositoryException
     * @throws IOException
     */
    Map<String, String> generateChecksums(Session session, String path) throws RepositoryException,
            IOException;

    /**
     * Traverses the content tree whose root is defined by the path param, respecting the {@link
     * ChecksumGeneratorOptions}.
     * Generates map of checksum hashes in the format [ ABSOLUTE PATH ] : [ CHECKSUM OF NODE SYSTEM ]
     *
     * @param session the session
     * @param path    the root path to generate checksums for
     * @param options the {@link ChecksumGeneratorOptions} that define the checksum generation
     * @return the map of abs path ~> checksums
     * @throws RepositoryException
     * @throws IOException
     */
    Map<String, String> generateChecksums(Session session, String path, ChecksumGeneratorOptions options)
            throws RepositoryException, IOException;
}