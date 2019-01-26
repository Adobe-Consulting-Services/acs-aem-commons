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

package com.adobe.acs.commons.users.impl;

import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {
                EnsureServiceUser.class,
                EnsureAuthorizable.class
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        factory = "com.adobe.acs.commons.users.impl.EnsureServiceUser",
        property = {
                "webconsole.configurationFactory.nameHint=Ensure Service User: {operation} {principalName}"
        }
)
@Designate(
        ocd = EnsureServiceUser.Config.class
)
public final class EnsureServiceUser implements EnsureAuthorizable {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Ensure Service User"
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Ensure immediately",
                defaultValue = "true",
                description = "Ensure on activation. When set to false, this must be ensured via the JMX MBean."
        )
        boolean ensure$_$immediately();

        @AttributeDefinition(
                name = "Operation",
                description = "Defines if the service user (principal name) should be adjusted to align with this config or removed completely",
                options = {
                        @Option(value = "add", label = "Ensure existence (add)"),
                        @Option(value = "remove", label = "Ensure extinction (remove)")
                }
        )
        String operation();

        @AttributeDefinition(
                name = "Principal Name",
                description = "The service user's principal name"
        )
        String principalName();

        @AttributeDefinition(
                name = "ACEs",
                description = "This field is ignored if the Operation is set to 'Ensure extinction' (remove)",
                cardinality = Integer.MAX_VALUE
        )
        String[] aces();
    }

    public static final String PROP_ENSURE_IMMEDIATELY = "ensure-immediately";
    public static final String DEFAULT_OPERATION = "add";

    public static final String PROP_OPERATION = "operation";

    public static final String PROP_PRINCIPAL_NAME = "principalName";

    public static final String PROP_ACES = "aces";
    private static final Logger log = LoggerFactory.getLogger(EnsureServiceUser.class);
    private static final String SERVICE_NAME = "ensure-service-user";
    private static final Map<String, Object> AUTH_INFO;
    public static boolean DEFAULT_ENSURE_IMMEDIATELY = true;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private ServiceUser serviceUser = null;
    private Operation operation = null;
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private EnsureAce ensureAce;

    /**
     * @return the Operation this OSGi Config represents
     */
    @Override
    public Operation getOperation() {
        return operation;
    }

    /**
     * @return the Service User this OSGi Config represents
     */
    @Override
    public ServiceUser getAuthorizable() {
        return serviceUser;
    }

    /**
     * Entry point for Ensuring a System User.
     *
     * @param operation
     *            the ensure operation to execute (ADD or REMOVE)
     * @param serviceUser
     *            the service user configuration to ensure
     * @throws EnsureAuthorizableException
     */
    @Override
    public void ensure(Operation operation, AbstractAuthorizable serviceUser) throws EnsureAuthorizableException {
        final long start = System.currentTimeMillis();

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
            if (Operation.ADD.equals(operation)) {
                ensureExistance(resourceResolver, (ServiceUser) serviceUser);
            } else if (Operation.REMOVE.equals(operation)) {
                ensureRemoval(resourceResolver, (ServiceUser) serviceUser);
            } else {
                throw new EnsureAuthorizableException(
                        "Unable to determine Ensure Service User operation Could not create or locate value system user (it is null).");
            }

            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
                log.debug("Persisted change to Service User [ {} ]", serviceUser.getPrincipalName());
            } else {
                log.debug("No changes required for Service User [ {} ]. Skipping...", serviceUser.getPrincipalName());
            }

            log.info(
                    "Successfully ensured [ {} ] of Service User [ {} ] in [ {} ms ]",
                    operation.toString(), getAuthorizable().getPrincipalName(),
                            String.valueOf(System.currentTimeMillis() - start));
        } catch (Exception e) {
            throw new EnsureAuthorizableException(String.format("Failed to ensure [ %s ] of Service User [ %s ]",
                    operation.toString(), serviceUser.getPrincipalName()), e);
        }
    }

    /**
     * Ensures that the provided ServiceUser and configured ACEs exist. Any extra ACEs will be removed, and any missing
     * ACEs added.
     *
     * @param resourceResolver
     *            the resource resolver to perform the user and ACE management
     * @param serviceUser
     *            the service user to ensure
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    @SuppressWarnings("squid:S2583")
    protected void ensureExistance(ResourceResolver resourceResolver, ServiceUser serviceUser)
            throws RepositoryException, EnsureAuthorizableException {
        final User systemUser = ensureSystemUser(resourceResolver, serviceUser);

        if (systemUser != null) {
            ensureAce.ensureAces(resourceResolver, systemUser, serviceUser);
        } else {
            log.error("Could not create or locate System User with principal name [ {} ]",
                    serviceUser.getPrincipalName());
        }
    }

    /**
     * Ensures that the provided ServiceUser and any of its ACEs are removed.
     *
     * @param resourceResolver
     *            the resource resolver to perform the user and ACE management
     * @param serviceUser
     *            the service user to ensure
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    private void ensureRemoval(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException,
            EnsureAuthorizableException {
        final User systemUser = findSystemUser(resourceResolver, serviceUser.getPrincipalName());

        ensureAce.removeAces(resourceResolver, systemUser, serviceUser);

        if (systemUser != null) {
            systemUser.remove();
        }
    }

    /**
     * Ensures a System User exists with the principal name provided by the Service User configuration.
     *
     * @param resourceResolver
     *            the resource resolver to perform the user management
     * @param serviceUser
     *            the service user to ensure
     * @return the System User; this should never return null
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    private User ensureSystemUser(ResourceResolver resourceResolver, ServiceUser serviceUser)
            throws RepositoryException, EnsureAuthorizableException {
        User user = findSystemUser(resourceResolver, serviceUser.getPrincipalName());

        if (user == null) {
            final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

            // No principal found with this name; create the system user
            log.debug("Requesting creation of system user [ {} ] at [ {} ]", serviceUser.getPrincipalName(),
                    serviceUser.getIntermediatePath());
            user = userManager.createSystemUser(serviceUser.getPrincipalName(), serviceUser.getIntermediatePath());
            log.debug("Created system user at [ {} ]", user.getPath());
        }

        return user;
    }

    /**
     * Locates a System User by principal name, or null. Note, if a rep:User can be found but it is NOT a system user,
     * this method will throw an exception.
     *
     * @param resourceResolver
     *            the resource resolver to perform the user management
     * @param principalName
     *            the principal name
     * @return the System User or null
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    private User findSystemUser(ResourceResolver resourceResolver, String principalName) throws RepositoryException,
            EnsureAuthorizableException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        User user = null;

        // Handle the actual user creation

        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable != null) {
            // Am authorizable was found with this name; check if this is a system user
            if (authorizable instanceof User) {
                user = (User) authorizable;
                if (!user.isSystemUser()) {
                    throw new EnsureAuthorizableException(String.format(
                            "User [ %s ] ensureExistance at [ %s ] but is NOT a system user", principalName,
                            user.getPath()));
                }
            } else {
                throw new EnsureAuthorizableException(String.format("Authorizable [ %s ] at [ %s ] is not a user",
                        principalName, authorizable.getPath()));
            }
        }

        return user;
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        boolean ensureImmediately =
                PropertiesUtil.toBoolean(config.get(PROP_ENSURE_IMMEDIATELY), DEFAULT_ENSURE_IMMEDIATELY);

        String operationStr =
                StringUtils.upperCase(PropertiesUtil.toString(config.get(PROP_OPERATION), DEFAULT_OPERATION));
        try {
            this.operation = Operation.valueOf(operationStr);
            // Parse OSGi Configuration into Service User object
            this.serviceUser = new ServiceUser(config);

            if (ensureImmediately) {
                // Ensure
                ensure(operation, getAuthorizable());
            } else {
                log.info("This Service User is configured to NOT ensure immediately. Please ensure this Service User via the JMX MBean.");
            }

        } catch (EnsureAuthorizableException e) {
            log.error("Unable to ensure Service User [ {} ]",
                    PropertiesUtil.toString(config.get(PROP_PRINCIPAL_NAME), "Undefined Service User Principal Name"),
                    e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown Ensure Service User operation [ " + operationStr + " ]", e);
        }
    }

}
