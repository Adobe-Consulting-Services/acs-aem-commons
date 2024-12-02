package com.adobe.acs.commons.wcm.impl;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class CopySitesPublishUrlFeatureTest {


    @InjectMocks
    CopySitesPublishUrlFeature copySitesPublishUrlFeature = new CopySitesPublishUrlFeature();
    @Mock
    CopySitesPublishUrlFeature.Config config;

    @BeforeEach
    void setUp() {
        copySitesPublishUrlFeature.activate(config);
    }

    @Test
    void testGetName() {
        assertEquals("com.adobe.acs.commons.wcm.impl.copysitespublishurlfeature.feature.flag",
                copySitesPublishUrlFeature.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("ACS AEM Commons feature flag enables or disables the copy publish URL dropdown field in the Sites Editor.",
                copySitesPublishUrlFeature.getDescription());
    }

    @Test
    void testIsEnabled() {
        when(config.feature_flag_active_status()).thenReturn(true);
        assertTrue(copySitesPublishUrlFeature.isEnabled(null));
    }

}