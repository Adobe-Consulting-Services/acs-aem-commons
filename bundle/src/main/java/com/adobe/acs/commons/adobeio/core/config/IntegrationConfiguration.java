package com.adobe.acs.commons.adobeio.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Adobe I/O. Integration Configuration",
		description = "Configuration of Adobe.IO access")
public @interface IntegrationConfiguration {

	@AttributeDefinition(name = "Endpoint", description = "Endpoint for the JWT-check", defaultValue = "https://ims-na1.adobelogin.com/ims/exchange/jwt")
	String getEndpoint();

	@AttributeDefinition(name = "Login Endpoint", description = "Login Endpoint for the JWT-check", defaultValue = "https://ims-na1.adobelogin.com/c/")
	String getLoginEndpoint();

	@AttributeDefinition(name = "PrivateKey", description = "Contents of the private.key file")
	String getPrivateKey();

	@AttributeDefinition(name = "ClientId", description = "Client Id")
	String getClientId();
 
	@AttributeDefinition(name = "ClientSecret", description = "Client Secret")
	String getClientSecret();
    
	@AttributeDefinition(name = "OrgId", description = "Organization id")
	String getAMCOrgId();
    
	@AttributeDefinition(name = "TechAccountId", description = "Technical Account Id")
	String getTechAccountId();
        
	@AttributeDefinition(name = "LoginClaim", description = "Login claims", defaultValue="https://ims-na1.adobelogin.com/s/ent_campaign_sdk")
	String[] getAdobeLoginClaimKey();
	
	@AttributeDefinition(name = "ExpirationTime", description = "Expiration time of the access token in seconds",
			             defaultValue="7200",type= AttributeType.INTEGER)
	int getExpirationTimeInSeconds();

	@AttributeDefinition(name = "Adobe I/O Domain", description = "Domain of Adobe I/O", defaultValue = "https://mc.adobe.io")
	String getIODomain();

	@AttributeDefinition(name = "Tenant", description = "Tenant")
	String getTenant();

	@AttributeDefinition(name = "Service", description = "Service. Enter campaign, analytics, etc...")
	String getService();

	String webconsole_configurationFactory_nameHint() default "Integration <b>{getTenant} - {getService}</b><br/> {getAMCOrgId}";
}
