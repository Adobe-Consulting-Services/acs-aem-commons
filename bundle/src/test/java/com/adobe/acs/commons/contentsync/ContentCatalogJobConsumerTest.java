package com.adobe.acs.commons.contentsync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ContentCatalogJobConsumerTest {

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private Job job;

    @Mock
    private JobExecutionContext jobContext;

    @Mock
    private JobExecutionResult jobResult;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Session session;

    @Mock
    private Node parentNode;

    @Mock
    private UpdateStrategy mockStrategy;

    @InjectMocks
    private ContentCatalogJobConsumer consumer;

    @BeforeEach
    void setUp() throws Exception {
        when(jobContext.result()).thenReturn(jobResult);
        when(jobResult.succeeded()).thenReturn(jobResult);
        when(jobResult.cancelled()).thenReturn(jobResult);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    }

    @Test
    void testProcessJob_Success() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE,
            ContentCatalogJobConsumer.SERVICE_NAME
        );

        when(job.getId()).thenReturn(jobId);
        when(job.getPropertyNames()).thenReturn(Arrays.asList("path", "strategy"));
        when(job.getProperty("path")).thenReturn("/content/test");
        when(job.getProperty("strategy")).thenReturn(mockStrategy.getClass().getName());

        CatalogItem mockItem = mock(CatalogItem.class);
        when(mockItem.getJsonObject()).thenReturn(mock(JsonObject.class));
        when(mockStrategy.getItems(any())).thenReturn(Collections.singletonList(mockItem));

        when(resourceResolverFactory.getServiceResourceResolver(authInfo))
            .thenReturn(resourceResolver);

        // Bind the strategy
        consumer.bindDeltaStrategy(mockStrategy);

        // Execute
        JobExecutionResult result = consumer.process(job, jobContext);

        // Verify
        verify(resourceResolverFactory).getServiceResourceResolver(authInfo);
        verify(mockStrategy).getItems(any());
        verify(resourceResolver).commit();
        assertEquals(jobResult, result);
    }

    @Test
    void testProcessJob_StrategyError() {
        // Setup
        when(job.getProperty("strategy")).thenReturn("non.existent.Strategy");

        // Execute
        JobExecutionResult result = consumer.process(job, jobContext);

        // Verify
        verify(jobResult).message(anyString());
        verify(jobResult).cancelled();
    }

    @Test
    void testGetStrategy_DefaultStrategy() {
        // Setup
        UpdateStrategy defaultStrategy = mock(UpdateStrategy.class);
        consumer.bindDeltaStrategy(defaultStrategy);

        // Execute
        UpdateStrategy result = consumer.getStrategy(null);

        // Verify
        assertNotNull(result);
        assertEquals(defaultStrategy, result);
    }

    @Test
    void testGetStrategy_SpecificStrategy() {
        // Setup
        String strategyName = mockStrategy.getClass().getName();
        consumer.bindDeltaStrategy(mockStrategy);

        // Execute
        UpdateStrategy result = consumer.getStrategy(strategyName);

        // Verify
        assertNotNull(result);
        assertEquals(mockStrategy, result);
    }

    @Test
    void testGetStrategy_InvalidStrategy() {
        // Setup
        String invalidStrategy = "invalid.strategy";

        // Verify
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.getStrategy(invalidStrategy);
        });
    }

    @Test
    void testBindUnbindStrategy() {
        // Setup
        UpdateStrategy strategy = mock(UpdateStrategy.class);
        String strategyName = strategy.getClass().getName();

        // Execute bind
        consumer.bindDeltaStrategy(strategy);
        UpdateStrategy boundStrategy = consumer.getStrategy(strategyName);

        // Verify bind
        assertNotNull(boundStrategy);
        assertEquals(strategy, boundStrategy);

        // Execute unbind
        consumer.unbindDeltaStrategy(strategy);

        // Verify unbind
        assertThrows(IllegalArgumentException.class, () -> {
            consumer.getStrategy(strategyName);
        });
    }

    @Test
    void testSave_ResourceResolverError() throws Exception {
        // Setup
        when(resourceResolverFactory.getServiceResourceResolver(any()))
            .thenThrow(new LoginException("Test error"));

        // Verify
        assertThrows(LoginException.class, () -> {
            consumer.save(mock(JsonObject.class), job);
        });
    }

    @Test
    void testProcessJob_ItemProcessingError() throws Exception {
        // Setup
        when(job.getProperty("strategy")).thenReturn(mockStrategy.getClass().getName());
        consumer.bindDeltaStrategy(mockStrategy);
        when(mockStrategy.getItems(any())).thenThrow(new RuntimeException("Test error"));

        // Execute
        JobExecutionResult result = consumer.process(job, jobContext);

        // Verify
        verify(jobResult).message(anyString());
        verify(jobResult).cancelled();
    }
}