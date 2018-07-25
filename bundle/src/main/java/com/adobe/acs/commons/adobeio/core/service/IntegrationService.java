package com.adobe.acs.commons.adobeio.core.service;

/**
 * Service to communicate to Adobe I/O with regards to authentication. 
 * 
 * Use the following command the generate the public/private keyfile
 * 
 * openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout private.key -out certificate_pub.crt
 *
 */
public interface IntegrationService {
	
	/**
	 * Get the api-key, can be used as the X-Api-Key header
	 * @return the configured api-key
	 */
	String getAPIKey();
	
	/**
	 * Get the access-token used as the Authorization header.
	 * This is fetched once per hour via a scheduler.
	 * @return the access token
	 */
	String getAccessToken();

	/**
	 * @return The domain of the integration
	 */
//	String getIODomain();

	/**
	 * @return The tenant of the integration
	 */
	String getTenant();

	/**
	 * @return The service of the integration
	 */
//	String getService();
	
	/**
	 * @return The ID of the integration
	 */
	String getIntegrationID();
}
