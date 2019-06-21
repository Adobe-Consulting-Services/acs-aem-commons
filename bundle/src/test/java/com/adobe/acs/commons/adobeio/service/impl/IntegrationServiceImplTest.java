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
package com.adobe.acs.commons.adobeio.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import junitx.util.PrivateAccessor;

public final class IntegrationServiceImplTest {

    private static final String validkey;

    static {
        String key = "";
        key += "-----BEGIN PRIVATE KEY-----";
        key += "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC450lHInz9NFAN";
        key += "LZ1rmEscnQHFvH+E25cnFUuuOvW5ng5/nH4g7YUx5Yx9m+4uW9CWExgJvwd0ATdQ";
        key += "RP4cBKTygfZPi4G9j1Vfn9+QqW1ukDLyi1yzwT1cNCqM7QUlDb9TuD5jtH9jc7y2";
        key += "RnN+c82DD88OZ4BeQhlZJbzmF7y/tEIbijcdemUoT/qgl8B4u7lIVkXuM7UjKrDH";
        key += "qygeiHInrORUEcICngt4fY/xh/p8R2rcnX4RBwpoIENcdTrW56uL1/EN4ElfCeWh";
        key += "/boBPufcx4+WJRab/RDgZXYfqa9v4TTyJaazJ3UVg81qPDI53CAHiAhUwcCoS1Xq";
        key += "7fMRHKz3AgMBAAECggEAbYRIKcLqpwpyE6wd3lmgu3zfTOA941Iw7+w0HCk928gb";
        key += "LEhjiFtGlewxT0CpTXoCTLFYfoVQA8yol3mVzMCPdje1zR2DJeScm3vU7hj8AQ2V";
        key += "r4UNzkY5csBPJ5NHhc/jr9Gj2mhRP3WQo6GMzKwIYIVfjVeOd8XMv7mTbfFHUX+Q";
        key += "GUzxCY5Ker1oeTNDGt/qnGoz7nztEEa60+VIMy4oheCM+Ex4aFphnfFKosmlwvUd";
        key += "28/RMslWrTVRUxucf7Hrcq9UlKz57OsQZ+LuDt7Lin/WczsXbx/PN241weRoSkQf";
        key += "dWUPWma2vypgRR48LC1pZn589pFMdU2uCeDcnLy/WQKBgQDdAweHRO1jUweXkri6";
        key += "MaK850ERoSW763rMk2uzaNqUAPu3F+0T7nlt73liQbFlGtoD+/Owz/NygvKYr8jU";
        key += "w+IObDv0UqaliAH6f977W+Re7tiuuYSLYW3TmsEtNuOAuItgGzkHhl1/gDTxCblf";
        key += "mmMIlp8wmpvZzjfTN45n5Li4UwKBgQDWLOOQ4P2EuZCD/GBTNUOy429ouiGLQcdj";
        key += "jpRcI9w7oQ5fUZgVA+8M8yha5ulegysdwj4SU6a4HyK95XGsI1k3308exLLVT2QA";
        key += "xZ9Qx8LQJbzoc8g1bP2BnWz1K9g+wDkBmnNtHZEsyorfd4EvbP2b0UbxxUddaDY3";
        key += "0Rq26GZUTQKBgF5WWrJMo5+P41+5iww+/53ugAHMbVTnUMVd78zm5tXiDY+7sMCf";
        key += "PEnGvGNKczQa4BBvTTedq+anKnIuJmVbL3TEklrZpLRnd6C7UCyurq4u+WKrYX41";
        key += "GjIGjoqEMVvSafud4xvpKKOxz+dLJUs/lSInWM/gTsILmhyYTDrgkFBtAoGADQDe";
        key += "tC4k1kkZ8BmDk6m5OYQ7gGVEohrVS0Md2NZIicpGgB0JGcWKXwPoYFSCuL1IAd+I";
        key += "Oz6e8bDaQCNNGoFu6kiBmkGhBfhy/uUAvjvSpSaVaJuW/T9nyNXRBrWpxG2RSHqj";
        key += "JW3VIZ4OfnDYBBQe9bXoE8fBhHdOS2dDsvU7OUUCgYAxifwerkcJ4bkY56ne258a";
        key += "H3+1XSvh7vmYwi4Kk5N5QiwdmOfrFC9ZydanUC/U6egKBuAkv1aJrlyABhc7k4vr";
        key += "FyoKF2bn4qlvLo5XkELDySEVY2Ra4zLWUxohyAOn/MrnBDwmbZy5lu7HHfoi6I5s";
        key += "k0TDGdyE4ZrZnEQ/UC3acg==";
        key += "-----END PRIVATE KEY-----";
        validkey = key;
   }

    private final IntegrationConfiguration config = mock(IntegrationConfiguration.class);
    private final IntegrationServiceImpl impl = new IntegrationServiceImpl();

    @Before
    public void setUp() {
        when(config.amcOrgId()).thenReturn("client-id");
        when(config.privateKey()).thenReturn(validkey);
        impl.activate(config);
    }

    @Test
    public void testJwtToken() {
        assertNotNull(impl.getJwtToken());
    }

    @Test
    public void testGetApiKey() {
        final String value = "api-key";
        when(config.clientId()).thenReturn(value);
        assertEquals(value, impl.getApiKey());
    }

    @Test
    public void testGetTimeoutinMilliSeconds() {
        final int value = 12345;
        when(config.timeoutInMilliSeocnds()).thenReturn(value);
        assertEquals(value, impl.getTimeoutinMilliSeconds());
    }

    @Test
    public void testConifg() throws NoSuchFieldException {
        impl.activate(config);;
        assertEquals(config, PrivateAccessor.getField(impl, "jwtServiceConfig"));
    }

    @Test
    public void testGenerateKeywithInvalidKey() {
        final byte[] result = IntegrationServiceImpl.buildPkcs8Key("invalid");
        assertEquals(new String(result), "");
    }

    @Test
    public void testGenerateKeywithValidKey() {
        final byte[] result = IntegrationServiceImpl.buildPkcs8Key(config.privateKey());
        assertNotEquals(new String(result), "");
        assertNotNull(result);
    }

}
