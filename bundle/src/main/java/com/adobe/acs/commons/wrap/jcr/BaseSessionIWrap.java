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
package com.adobe.acs.commons.wrap.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import javax.annotation.Nonnull;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;

import org.osgi.annotation.versioning.ProviderType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This is the base interface for {@link SessionIWrap} and
 * {@link com.adobe.acs.commons.wrap.jackrabbit.JackrabbitSessionIWrap}. Default methods are not only defined for all
 * {@link Session} methods, but also for a few methods to wrap other JCR types that retain references to the wrapped
 * session, and which therefore might need to be overridden to keep the integrity of the wrapper API in more complex use
 * cases.
 *
 * @param <S> the type of session that is being wrapped.
 */
@ProviderType
public interface BaseSessionIWrap<S extends Session> extends Session {

    /**
     * Return the underlying session.
     *
     * @return the underlying session
     */
    S unwrapSession();

    default @Nonnull
    <T extends Item> T wrapItem(final @Nonnull T item) {
        return item;
    }

    default @Nonnull
    Session wrapSession(final @Nonnull Session session) {
        return session;
    }

    default @Nonnull
    Workspace wrapWorkspace(final @Nonnull Workspace workspace) {
        return workspace;
    }

    @Override
    default Repository getRepository() {
        return unwrapSession().getRepository();
    }

    @Override
    default String getUserID() {
        return unwrapSession().getUserID();
    }

    @Override
    default String[] getAttributeNames() {
        return unwrapSession().getAttributeNames();
    }

    @Override
    default Object getAttribute(final String name) {
        return unwrapSession().getAttribute(name);
    }

    @Override
    default Workspace getWorkspace() {
        return wrapWorkspace(unwrapSession().getWorkspace());
    }

    @Override
    default Node getRootNode() throws RepositoryException {
        return unwrapSession().getRootNode();
    }

    @Override
    default Session impersonate(final Credentials credentials) throws LoginException, RepositoryException {
        return wrapSession(unwrapSession().impersonate(credentials));
    }

    @SuppressWarnings("deprecation")
    @Override
    default Node getNodeByUUID(final String uuid) throws ItemNotFoundException, RepositoryException {
        return wrapItem(unwrapSession().getNodeByUUID(uuid));
    }

    @Override
    default Node getNodeByIdentifier(final String id) throws ItemNotFoundException, RepositoryException {
        return wrapItem(unwrapSession().getNodeByIdentifier(id));
    }

    @Override
    default Item getItem(final String absPath) throws PathNotFoundException, RepositoryException {
        return wrapItem(unwrapSession().getItem(absPath));
    }

    @Override
    default Node getNode(final String absPath) throws PathNotFoundException, RepositoryException {
        return wrapItem(unwrapSession().getNode(absPath));
    }

    @Override
    default Property getProperty(final String absPath) throws PathNotFoundException, RepositoryException {
        return wrapItem(unwrapSession().getProperty(absPath));
    }

    @Override
    default boolean itemExists(final String absPath) throws RepositoryException {
        return unwrapSession().itemExists(absPath);
    }

    @Override
    default boolean nodeExists(final String absPath) throws RepositoryException {
        return unwrapSession().nodeExists(absPath);
    }

    @Override
    default boolean propertyExists(final String absPath) throws RepositoryException {
        return unwrapSession().propertyExists(absPath);
    }

    @Override
    default void move(final String srcAbsPath, final String destAbsPath)
            throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException,
            LockException, RepositoryException {
        unwrapSession().move(srcAbsPath, destAbsPath);
    }

    @Override
    default void removeItem(final String absPath)
            throws VersionException, LockException, ConstraintViolationException, AccessDeniedException,
            RepositoryException {
        unwrapSession().removeItem(absPath);
    }

    @Override
    default void save()
            throws AccessDeniedException, ItemExistsException, ReferentialIntegrityException,
            ConstraintViolationException, InvalidItemStateException, VersionException, LockException,
            NoSuchNodeTypeException, RepositoryException {
        unwrapSession().save();
    }

    @Override
    default void refresh(final boolean keepChanges) throws RepositoryException {
        unwrapSession().refresh(keepChanges);
    }

    @Override
    default boolean hasPendingChanges() throws RepositoryException {
        return unwrapSession().hasPendingChanges();
    }

    @Override
    default ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return unwrapSession().getValueFactory();
    }

    @Override
    default boolean hasPermission(final String absPath, final String actions) throws RepositoryException {
        return unwrapSession().hasPermission(absPath, actions);
    }

    @Override
    default void checkPermission(final String absPath, final String actions) throws AccessControlException,
            RepositoryException {
        unwrapSession().checkPermission(absPath, actions);
    }

    @Override
    default boolean hasCapability(final String methodName, final Object target, final Object[] arguments)
            throws RepositoryException {
        return unwrapSession().hasCapability(methodName, target, arguments);
    }

    @Override
    default ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return unwrapSession().getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    @Override
    default void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior)
            throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
            VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        unwrapSession().importXML(parentAbsPath, in, uuidBehavior);
    }

    @Override
    default void exportSystemView(final String absPath,
                                  final ContentHandler contentHandler,
                                  final boolean skipBinary,
                                  final boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        unwrapSession().exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    default void exportSystemView(final String absPath,
                                  final OutputStream out,
                                  final boolean skipBinary,
                                  final boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        unwrapSession().exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    default void exportDocumentView(final String absPath,
                                    final ContentHandler contentHandler,
                                    final boolean skipBinary,
                                    final boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        unwrapSession().exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    default void exportDocumentView(final String absPath,
                                    final OutputStream out,
                                    final boolean skipBinary,
                                    final boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        unwrapSession().exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    default void setNamespacePrefix(final String prefix,
                                    final String uri) throws NamespaceException, RepositoryException {
        unwrapSession().setNamespacePrefix(prefix, uri);
    }

    @Override
    default String[] getNamespacePrefixes() throws RepositoryException {
        return unwrapSession().getNamespacePrefixes();
    }

    @Override
    default String getNamespaceURI(final String prefix) throws NamespaceException, RepositoryException {
        return unwrapSession().getNamespaceURI(prefix);
    }

    @Override
    default String getNamespacePrefix(final String uri) throws NamespaceException, RepositoryException {
        return unwrapSession().getNamespacePrefix(uri);
    }

    @Override
    default void logout() {
        unwrapSession().logout();
    }

    @Override
    default boolean isLive() {
        return unwrapSession().isLive();
    }

    @SuppressWarnings("deprecation")
    @Override
    default void addLockToken(final String lt) {
        unwrapSession().addLockToken(lt);
    }

    @SuppressWarnings("deprecation")
    @Override
    default String[] getLockTokens() {
        return unwrapSession().getLockTokens();
    }

    @SuppressWarnings("deprecation")
    @Override
    default void removeLockToken(final String lt) {
        unwrapSession().removeLockToken(lt);
    }

    @Override
    default AccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return unwrapSession().getAccessControlManager();
    }

    @Override
    default RetentionManager getRetentionManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return unwrapSession().getRetentionManager();
    }
}
