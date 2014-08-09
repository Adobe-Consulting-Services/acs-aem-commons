package com.adobe.acs.commons.workflow.bulk.impl;

import com.day.cq.commons.jcr.JcrUtil;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrUtil.class)
public class BucketTest {
    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;

    @Mock
    Node node;

    @Before
    public void setUp() throws Exception {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    }

    @Test
    public void testGetNextPath() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        int total = 1000;
        int bucketSize = 10;
        String bucketType = "sling:Folder";
        Bucket bucket = new Bucket(bucketSize, total, "/content/test", bucketType);

        when(node.getPath()).thenReturn("check the captor not this value");
        when(resourceResolver.getResource(anyString())).thenReturn(null);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        for(int i = 0; i < total; i++) {

            if(i == 0) {
                final String expected = "/content/test/1/1";

                when(JcrUtil.createPath(captor.capture(), eq(bucketType), eq(bucketType), eq(session), eq(false))).thenReturn(node);
                bucket.getNextPath(resourceResolver);
                assertEquals(expected, captor.getValue());

            } else if(i == 10) {

                final String expected = "/content/test/1/2";

                when(JcrUtil.createPath(captor.capture(), eq(bucketType), eq(bucketType), eq(session), eq(false))).thenReturn(node);
                bucket.getNextPath(resourceResolver);
                assertEquals(expected, captor.getValue());

            } else if(i == 99) {

                final String expected = "/content/test/1/10";

                when(JcrUtil.createPath(captor.capture(), eq(bucketType), eq(bucketType), eq(session), eq(false))).thenReturn(node);
                bucket.getNextPath(resourceResolver);
                assertEquals(expected, captor.getValue());

            } else if(i == 999) {

                final String expected = "/content/test/10/10";

                when(JcrUtil.createPath(captor.capture(), eq(bucketType), eq(bucketType), eq(session), eq(false))).thenReturn(node);
                bucket.getNextPath(resourceResolver);
                assertEquals(expected, captor.getValue());
            } else {
                // Call to increment bucket
                bucket.getNextPath(resourceResolver);
            }
        }
    }
}