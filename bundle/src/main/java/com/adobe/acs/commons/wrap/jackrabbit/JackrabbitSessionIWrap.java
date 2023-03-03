/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.wrap.jackrabbit;

import javax.annotation.Nonnull;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import com.adobe.acs.commons.wrap.jcr.BaseSessionIWrap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for wrappers of JackrabbitSessions.
 */
@ProviderType
public interface JackrabbitSessionIWrap extends BaseSessionIWrap<JackrabbitSession>, JackrabbitSession {

    @Override
    default boolean hasPermission(final @Nonnull String absPath, final @Nonnull String... actions)
            throws RepositoryException {
        return unwrapSession().hasPermission(absPath, actions);
    }

    @Override
    default PrincipalManager getPrincipalManager()
            throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return unwrapSession().getPrincipalManager();
    }

    @Override
    default UserManager getUserManager()
            throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return unwrapSession().getUserManager();
    }

    @Override
    default Item getItemOrNull(final String absPath) throws RepositoryException {
        Item internal = unwrapSession().getItemOrNull(absPath);
        return internal != null ? wrapItem(internal) : null;
    }

    @Override
    default Property getPropertyOrNull(final String absPath) throws RepositoryException {
        Property internal = unwrapSession().getPropertyOrNull(absPath);
        return internal != null ? wrapItem(internal) : null;
    }

    @Override
    default Node getNodeOrNull(final String absPath) throws RepositoryException {
        Node internal = unwrapSession().getNodeOrNull(absPath);
        return internal != null ? wrapItem(internal) : null;
    }
}
