package com.adobe.acs.commons.cors;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

public class CorsUtilTest {
    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public final void tearDown() throws Exception {

    }

    @Test
    public final void testgetHeadersAsArraySingleValue() throws Exception {
        String inputHeader = "Authorization";
        String[] expected = new String[]{"Authorization"};
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }

    @Test
    public final void testgetHeadersAsArrayMultipleeValue() throws Exception {
        String inputHeader = "Authorization,COOKIE";
        String[] expected = new String[]{"Authorization", "COOKIE"};
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }

    @Test
    public final void testgetHeadersAsArrayNullValue() throws Exception {
        String inputHeader = "";
        String[] expected = new String[0];
        String[] response = CORSUtil.getHeadersAsArray(inputHeader);
        Assert.assertTrue(Arrays.equals(expected, response));
    }
}