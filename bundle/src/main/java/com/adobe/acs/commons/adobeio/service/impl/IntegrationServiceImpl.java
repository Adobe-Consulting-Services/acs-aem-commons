/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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

import static com.adobe.acs.commons.adobeio.service.impl.AdobeioConstants.JSON_ACCESS_TOKEN;
import static io.jsonwebtoken.SignatureAlgorithm.RS256;
import static org.apache.http.impl.client.HttpClients.createDefault;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.exception.AdobeioException;
import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.jsonwebtoken.Jwts;

//scheduler is set to once per hour
//you can use cronmaker.com for generating cron expressions
@SuppressWarnings("PackageAccessibility")
@Component(service = {IntegrationService.class, Runnable.class},
        property = "scheduler.expression=0 0 0/1 1/1 * ? *")
@Designate(ocd = IntegrationConfiguration.class, factory=false)
public class IntegrationServiceImpl implements IntegrationService, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationServiceImpl.class);
    private static final Base64.Decoder DECODER = Base64.getMimeDecoder();
    private String accessToken = null;
    private IntegrationConfiguration jwtServiceConfig;

    @Activate
    @Modified
    protected void activate(IntegrationConfiguration config) {
        this.jwtServiceConfig = config;
    }

    @Override
    public void run() {
        // fetch access token from adobe.io
        // this method is invoked via the scheduler
        accessToken = fetchAccessToken();
        LOGGER.info("access token in run()-method {}", accessToken);
    }

    @Override
    public String getAccessToken() {
        if (StringUtils.isEmpty(accessToken)) {
            accessToken = fetchAccessToken();
        }
        return accessToken;
    }

    @Override
    public String getApiKey() {
        return jwtServiceConfig.getClientId();
    }
    
    // --------    PRIVATE METHODS    ----------
    private String fetchAccessToken() {
        String token = StringUtils.EMPTY;

        try(CloseableHttpClient client = createDefault()) {
            HttpPost post = new HttpPost(jwtServiceConfig.getEndpoint());
            post.addHeader("Cache-Control", "no-cache");
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            List<BasicNameValuePair> params = Lists.newArrayList();
            params.add(new BasicNameValuePair("client_id", jwtServiceConfig.getClientId()));
            params.add(new BasicNameValuePair("client_secret", jwtServiceConfig.getClientSecret()));
            params.add(new BasicNameValuePair("jwt_token", getJwtToken()));

            post.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.info("response code {} ", response.getStatusLine().getStatusCode());
            }
            String result = IOUtils.toString(response.getEntity().getContent());

            LOGGER.info("JSON Response : {}", result);
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(result).getAsJsonObject();

            if (json.has(JSON_ACCESS_TOKEN)) {
                token = json.get(JSON_ACCESS_TOKEN).getAsString();
            } else {
                LOGGER.error("JSON does not contain an access_token");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("JWT Access Token : {}", token);
        return token;
    }

    private String getJwtToken() {
        String jwtToken = StringUtils.EMPTY;
        try {
            jwtToken = Jwts
                    .builder()
                    .setClaims(getJwtClaims())
                    .signWith(RS256, getPrivateKey())
                    .compact();
        } catch (Exception e) {
            LOGGER.error("JWT claims {}", getJwtClaims());
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("JWT Token : \n {}", jwtToken);
        return jwtToken;
    }

    private PrivateKey getPrivateKey() throws AdobeioException, NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buildPkcs8Key(jwtServiceConfig.getPrivateKey()));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    private static byte[] buildPkcs8Key(String privateKey) throws AdobeioException {
        if (privateKey.contains("--BEGIN PRIVATE KEY--")) {
            return DECODER.decode(privateKey.replaceAll("-----\\w+ PRIVATE KEY-----", ""));
        }
        if (!privateKey.contains("--BEGIN RSA PRIVATE KEY--")) {
            throw new AdobeioException("Invalid cert format: " + privateKey);
        }

        final byte[] innerKey = DECODER.decode(privateKey.replaceAll("-----\\w+ RSA PRIVATE KEY-----", ""));
        final byte[] result = new byte[innerKey.length + 26];
        System.arraycopy(DECODER.decode("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKY="), 0, result, 0, 26);
        System.arraycopy(BigInteger.valueOf(result.length - 4).toByteArray(), 0, result, 2, 2);
        System.arraycopy(BigInteger.valueOf(innerKey.length).toByteArray(), 0, result, 24, 2);
        System.arraycopy(innerKey, 0, result, 26, innerKey.length);
        return result;
    }

    private Map getJwtClaims() {
        Map jwtClaims = new HashMap<>();

        jwtClaims.put("iss", jwtServiceConfig.getAmcOrgId());
        jwtClaims.put("sub", jwtServiceConfig.getTechAccountId());
        jwtClaims.put("exp", getExpirationDate());
        jwtClaims.put("aud", String.format("%s%s", jwtServiceConfig.getLoginEndpoint(), jwtServiceConfig.getClientId()));
        String [] claims = jwtServiceConfig.getAdobeLoginClaimKey();
        if (claims != null && claims.length > 0) {
        	  for( int i=0; i < claims.length; i++) {
        		  jwtClaims.put(jwtServiceConfig.getAdobeLoginClaimKey()[i], Boolean.TRUE);
        	  }
        }

        if (LOGGER.isDebugEnabled()) {
            Gson gson = new Gson();
            LOGGER.debug(gson.toJson(jwtClaims));
        }

        return jwtClaims;
    }

    private Date getExpirationDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, jwtServiceConfig.getExpirationTimeInSeconds());
        return cal.getTime();
    }

}