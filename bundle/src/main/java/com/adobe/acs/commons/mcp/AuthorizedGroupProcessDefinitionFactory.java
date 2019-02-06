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
package com.adobe.acs.commons.mcp;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.User;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * ProcessDefinitionFactory which limits availability of a process to the admin users and members of the specified
 * groups.
 *
 * @param <P> Process definition generated by this factory
 */
@ConsumerType
public abstract class AuthorizedGroupProcessDefinitionFactory<P extends ProcessDefinition> extends ProcessDefinitionFactory<P> {

    @Override
    @SuppressWarnings("squid:S1141")
    public boolean isAllowed(User user) {
        try {
            return user.isAdmin()
                    || StreamSupport.stream(Spliterators.spliteratorUnknownSize(user.memberOf(), Spliterator.ORDERED), false)
                            .anyMatch(g -> {
                                try {
                                    for (String group : getAuthorizedGroups()) {
                                        if (g.getID().equals(group)) {
                                            return true;
                                        }
                                    }
                                    return false;
                                } catch (RepositoryException ex) {
                                    return false;
                                }
                            });
        } catch (RepositoryException e) {
            return false;
        }
    }

    protected abstract String[] getAuthorizedGroups();

    @Override
    protected abstract P createProcessDefinitionInstance();
}
