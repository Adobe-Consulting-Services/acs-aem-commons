package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static com.day.cq.wcm.api.NameConstants.PN_PAGE_LAST_REPLICATED;
import static com.day.cq.wcm.api.NameConstants.PN_PAGE_LAST_REPLICATED_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class ReplicateRedirectMapServletTest {

    private static final String REDIRECTS_PARENT_PATH = "/conf/global/settings/redirects";
    private static final String PACKAGE_PATH =
            "/etc/packages/com.adobe.acs.commons.redirects/conf.global.settings.redirects-1.0.zip";

    @Mock
    private Replicator replicator;
    @Mock
    private PackageHelper packageHelper;
    @Mock
    private Packaging packaging;
    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    @Mock
    private Session session;

    private final AemContext context = new AemContext();

    private ReplicateRedirectMapServlet target;

    @BeforeEach
    void setUp() throws LoginException, NoSuchFieldException {
        context.currentResource(context.create().resource(REDIRECTS_PARENT_PATH));
        ResourceResolverWrapper resourceResolverWrapper = stubResourceResolverWithSession();
        lenient().when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolverWrapper);
        context.registerService(Replicator.class, replicator);
        context.registerService(PackageHelper.class, packageHelper);
        context.registerService(Packaging.class, packaging);
        context.registerService(ResourceResolverFactory.class, resourceResolverFactory);
        ReplicateRedirectMapServlet component = new ReplicateRedirectMapServlet();
        target = context.registerInjectActivateService(component);
        PrivateAccessor.setField(target, "resourceResolverFactory", resourceResolverFactory);
    }

    @Test
    void doPost_success() throws Exception {
        target = spy(target);
        doReturn(PACKAGE_PATH).when(target).createPackage(any(ResourceResolver.class), eq(REDIRECTS_PARENT_PATH));

        target.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        String responseBody = context.response().getOutputAsString();
        assertTrue(responseBody.contains(REDIRECTS_PARENT_PATH));
        assertTrue(responseBody.contains("\"status.code\":200"));
    }

    @Test
    void doPost_success_replicatesPackage() throws Exception {
        target = spy(target);
        doReturn(PACKAGE_PATH).when(target).createPackage(any(ResourceResolver.class), eq(REDIRECTS_PARENT_PATH));

        target.doPost(context.request(), context.response());

        verify(replicator).replicate(session, ReplicationActionType.ACTIVATE, PACKAGE_PATH);

        Map<String, Object> vm = context.currentResource().getValueMap();
        assertNotNull(vm.get(PN_PAGE_LAST_REPLICATED));
        assertInstanceOf(Calendar.class, vm.get(PN_PAGE_LAST_REPLICATED));
        assertEquals(context.resourceResolver().getUserID(), vm.get(PN_PAGE_LAST_REPLICATED_BY));

        assertEquals(200, context.response().getStatus());
        String responseBody = context.response().getOutputAsString();
        assertTrue(responseBody.contains(REDIRECTS_PARENT_PATH));
        assertTrue(responseBody.contains("\"status.code\":200"));
    }

    @Test
    void doPost_success_updatesReplicationProperties() throws Exception {
        target = spy(target);
        doReturn(PACKAGE_PATH).when(target).createPackage(any(ResourceResolver.class), eq(REDIRECTS_PARENT_PATH));

        target.doPost(context.request(), context.response());

        Map<String, Object> vm = context.currentResource().getValueMap();
        assertNotNull(vm.get(PN_PAGE_LAST_REPLICATED));
        assertInstanceOf(Calendar.class, vm.get(PN_PAGE_LAST_REPLICATED));
        assertEquals(context.resourceResolver().getUserID(), vm.get(PN_PAGE_LAST_REPLICATED_BY));

        assertEquals(200, context.response().getStatus());
        String responseBody = context.response().getOutputAsString();
        assertTrue(responseBody.contains(REDIRECTS_PARENT_PATH));
        assertTrue(responseBody.contains("\"status.code\":200"));
    }

    @Test
    void doPost_whenCreatePackageThrowsIOException_setsErrorInResponse() throws Exception {
        doThrow(new IOException("io-failure")).when(packageHelper)
                .createPackageForPaths(
                        eq(Collections.singletonList(REDIRECTS_PARENT_PATH)),
                        eq(session),
                        eq("com.adobe.acs.commons.redirects"),
                        eq("conf.global.settings.redirects"),
                        eq("1.0"),
                        eq(PackageHelper.ConflictResolution.IncrementVersion),
                        anyMap()
                );

        target.doPost(context.request(), context.response());

        assertTrue(context.response().getOutputAsString().contains("io-failure"));
        assertEquals(500, context.response().getStatus());
        verifyPackageWasNeverActivated();
    }

    @Test
    void doPost_whenCreatePackageThrowsRepositoryException_setsErrorInResponse() throws Exception {
        doThrow(new RepositoryException("repo-failure")).when(packageHelper)
                .createPackageForPaths(
                        eq(Collections.singletonList(REDIRECTS_PARENT_PATH)),
                        eq(session),
                        eq("com.adobe.acs.commons.redirects"),
                        eq("conf.global.settings.redirects"),
                        eq("1.0"),
                        eq(PackageHelper.ConflictResolution.IncrementVersion),
                        anyMap()
                );

        target.doPost(context.request(), context.response());

        assertTrue(context.response().getOutputAsString().contains("repo-failure"));
        assertEquals(500, context.response().getStatus());
        verifyPackageWasNeverActivated();
    }

    @Test
    void doPost_whenReplicatorThrowsReplicationException_setsErrorInResponse() throws Exception {
        target = spy(target);
        doReturn(PACKAGE_PATH).when(target).createPackage(any(ResourceResolver.class), eq(REDIRECTS_PARENT_PATH));
        doThrow(new ReplicationException("replication-failure")).when(replicator)
                .replicate(session, ReplicationActionType.ACTIVATE, PACKAGE_PATH);

        target.doPost(context.request(), context.response());

        assertTrue(context.response().getOutputAsString().contains("replication-failure"));
    }

    @Test
    void doPost_whenServiceResolverLoginFails_setsErrorInResponse() throws Exception {
        Mockito.reset(resourceResolverFactory);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("login-failure"));

        target.doPost(context.request(), context.response());

        verifyPackageWasNeverActivated();
        assertTrue(context.response().getOutputAsString().contains("login-failure"));
    }

    @ParameterizedTest
    @MethodSource("validConfPaths")
    void createPackageName_returnsDotSeparatedSanitizedName(String input, String expected) {
        assertEquals(expected, target.createPackageName(input));
    }

    static Stream<Arguments> validConfPaths() {
        return Stream.of(
                Arguments.of(REDIRECTS_PARENT_PATH, "conf.global.settings.redirects"),
                Arguments.of("/conf/site", "conf.site"),
                Arguments.of("/conf/project/settings", "conf.project.settings"),
                Arguments.of("/conf/some page/with spaces",
                        "conf.some_page.with_spaces"),
                Arguments.of("/conf/special@chars/test",
                        "conf.special_chars.test"),
                Arguments.of("/conf", "conf")
        );
    }

    @Test
    void createPackageName_whenPathIsNull_throwsException() {
        assertThrows(NullPointerException.class, () -> target.createPackageName(null));
    }

    @Test
    void createPackage_buildsPackageAndReplicateNodePath() throws Exception {
        JcrPackage jcrPackage = mock(JcrPackage.class);
        when(packageHelper.createPackageForPaths(
                eq(Collections.singletonList(REDIRECTS_PARENT_PATH)),
                eq(session),
                eq("com.adobe.acs.commons.redirects"),
                eq("conf.global.settings.redirects"),
                eq("1.0"),
                eq(PackageHelper.ConflictResolution.IncrementVersion),
                argThat((Map<String, String> map) ->
                        AccessControlHandling.OVERWRITE.toString().equals(map.get(JcrPackageDefinition.PN_AC_HANDLING))
                                && ("ACS Commons Redirect Manager rules for " + REDIRECTS_PARENT_PATH)
                                .equals(map.get(JcrPackageDefinition.PN_DESCRIPTION)))
        )).thenReturn(jcrPackage);
        when(jcrPackage.getSize()).thenReturn(2L * 1024 * 1024);
        JcrPackageManager jcrPackageManager = mock(JcrPackageManager.class);
        Node packageNode = mock(Node.class);
        when(packaging.getPackageManager(session)).thenReturn(jcrPackageManager);
        when(jcrPackage.getNode()).thenReturn(packageNode);
        when(packageNode.getPath()).thenReturn(PACKAGE_PATH);

        target.doPost(context.request(), context.response());

        verify(packageHelper).addThumbnail(jcrPackage, null);
        verify(jcrPackageManager).assemble(jcrPackage, null);
        verifyPackageWasActivated();
    }

    @Test
    void updateReplicationStatusWithCurrentUser_updatesProperties() throws PersistenceException {
        Resource resource = context.create().resource("/conf/site/settings/redirects");

        target.updateReplicationStatusWithCurrentUser(resource);

        assertNotNull(resource.getValueMap().get(PN_PAGE_LAST_REPLICATED));
        assertEquals(context.resourceResolver().getUserID(), resource.getValueMap().get(PN_PAGE_LAST_REPLICATED_BY));
    }

    @Test
    void updateReplicationStatusWithCurrentUser_whenValueMapIsNull_returnsWithoutCommit() throws PersistenceException {
        Resource resource = mock(Resource.class);
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(null);

        target.updateReplicationStatusWithCurrentUser(resource);

        verify(resolver, never()).commit();
    }

    private void verifyPackageWasNeverActivated() throws ReplicationException {
        verify(replicator, never()).replicate(any(Session.class), any(ReplicationActionType.class), any(String.class));
    }

    private void verifyPackageWasActivated() throws ReplicationException {
        verify(replicator).replicate(eq(session), eq(ReplicationActionType.ACTIVATE), eq(PACKAGE_PATH));
    }

    private @NotNull ResourceResolverWrapper stubResourceResolverWithSession() {
        return new ResourceResolverWrapper(context.resourceResolver()) {
            @Override
            public <AdapterType> AdapterType adaptTo(@NotNull Class<AdapterType> type) {
                if (type == Session.class) {
                    return (AdapterType) session;
                }
                return super.adaptTo(type);
            }
        };
    }
}
