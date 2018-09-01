package com.adobe.acs.commons.adobeio.service.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntegrationServiceImplTest {
	
	@Test
	public void testGenerateKeywithInvalidKey() {
		byte[] result = IntegrationServiceImpl.buildPkcs8Key("invalid");
		
        assertEquals(new String(result), "");
	}
	
	@Test
	public void testGenerateKeywithValidKey() {
		String validkey = "";
		validkey += "-----BEGIN PRIVATE KEY-----";
		validkey += "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC450lHInz9NFAN";
		validkey += "LZ1rmEscnQHFvH+E25cnFUuuOvW5ng5/nH4g7YUx5Yx9m+4uW9CWExgJvwd0ATdQ";
		validkey += "RP4cBKTygfZPi4G9j1Vfn9+QqW1ukDLyi1yzwT1cNCqM7QUlDb9TuD5jtH9jc7y2";
		validkey += "RnN+c82DD88OZ4BeQhlZJbzmF7y/tEIbijcdemUoT/qgl8B4u7lIVkXuM7UjKrDH";
		validkey += "qygeiHInrORUEcICngt4fY/xh/p8R2rcnX4RBwpoIENcdTrW56uL1/EN4ElfCeWh";
		validkey += "/boBPufcx4+WJRab/RDgZXYfqa9v4TTyJaazJ3UVg81qPDI53CAHiAhUwcCoS1Xq";
		validkey += "7fMRHKz3AgMBAAECggEAbYRIKcLqpwpyE6wd3lmgu3zfTOA941Iw7+w0HCk928gb";
		validkey += "LEhjiFtGlewxT0CpTXoCTLFYfoVQA8yol3mVzMCPdje1zR2DJeScm3vU7hj8AQ2V";
		validkey += "r4UNzkY5csBPJ5NHhc/jr9Gj2mhRP3WQo6GMzKwIYIVfjVeOd8XMv7mTbfFHUX+Q";
		validkey += "GUzxCY5Ker1oeTNDGt/qnGoz7nztEEa60+VIMy4oheCM+Ex4aFphnfFKosmlwvUd";
		validkey += "28/RMslWrTVRUxucf7Hrcq9UlKz57OsQZ+LuDt7Lin/WczsXbx/PN241weRoSkQf";
		validkey += "dWUPWma2vypgRR48LC1pZn589pFMdU2uCeDcnLy/WQKBgQDdAweHRO1jUweXkri6";
		validkey += "MaK850ERoSW763rMk2uzaNqUAPu3F+0T7nlt73liQbFlGtoD+/Owz/NygvKYr8jU";
		validkey += "w+IObDv0UqaliAH6f977W+Re7tiuuYSLYW3TmsEtNuOAuItgGzkHhl1/gDTxCblf";
		validkey += "mmMIlp8wmpvZzjfTN45n5Li4UwKBgQDWLOOQ4P2EuZCD/GBTNUOy429ouiGLQcdj";
		validkey += "jpRcI9w7oQ5fUZgVA+8M8yha5ulegysdwj4SU6a4HyK95XGsI1k3308exLLVT2QA";
		validkey += "xZ9Qx8LQJbzoc8g1bP2BnWz1K9g+wDkBmnNtHZEsyorfd4EvbP2b0UbxxUddaDY3";
		validkey += "0Rq26GZUTQKBgF5WWrJMo5+P41+5iww+/53ugAHMbVTnUMVd78zm5tXiDY+7sMCf";
		validkey += "PEnGvGNKczQa4BBvTTedq+anKnIuJmVbL3TEklrZpLRnd6C7UCyurq4u+WKrYX41";
		validkey += "GjIGjoqEMVvSafud4xvpKKOxz+dLJUs/lSInWM/gTsILmhyYTDrgkFBtAoGADQDe";
		validkey += "tC4k1kkZ8BmDk6m5OYQ7gGVEohrVS0Md2NZIicpGgB0JGcWKXwPoYFSCuL1IAd+I";
		validkey += "Oz6e8bDaQCNNGoFu6kiBmkGhBfhy/uUAvjvSpSaVaJuW/T9nyNXRBrWpxG2RSHqj";
		validkey += "JW3VIZ4OfnDYBBQe9bXoE8fBhHdOS2dDsvU7OUUCgYAxifwerkcJ4bkY56ne258a";
		validkey += "H3+1XSvh7vmYwi4Kk5N5QiwdmOfrFC9ZydanUC/U6egKBuAkv1aJrlyABhc7k4vr";
		validkey += "FyoKF2bn4qlvLo5XkELDySEVY2Ra4zLWUxohyAOn/MrnBDwmbZy5lu7HHfoi6I5s";
		validkey += "k0TDGdyE4ZrZnEQ/UC3acg==";
		validkey += "-----END PRIVATE KEY-----";		
			
		
		byte[] result = IntegrationServiceImpl.buildPkcs8Key(validkey);
		
        assertNotEquals(new String(result), "");
        assertNotNull(result);
	}
}
