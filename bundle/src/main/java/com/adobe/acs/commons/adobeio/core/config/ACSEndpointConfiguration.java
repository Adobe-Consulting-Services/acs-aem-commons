package com.adobe.acs.commons.adobeio.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Adobe I/O. ACS Endpoint Factory Configuration",
        description = "Configuration of Adobe.io ACS endpoints")
public @interface ACSEndpointConfiguration {

    @AttributeDefinition(name = "ID", description = "Id of the endpoint")
    String getId();

    @AttributeDefinition(name = "Tenant", description = "Tenant used for this endpoint")
    String getTenant();

    @AttributeDefinition(name = "Method", description = "Used method for the endpoint")
    String getMethod();

    @AttributeDefinition(name = "URL Endpoint", description = "ACS Endpoint, without /campaign/, but starting with /")
    String getEndpoint();
    
    @AttributeDefinition(name = "Adobe I/O Domain", description = "Domain of Adobe I/O. egg: https://[mc|stock|...].adobe.io", defaultValue = "https://mc.adobe.io")
	String getIODomain();
    
    @AttributeDefinition(name = "Adobe I/O Endpoint ID", description = "ID used to bind service configuration with I/O integration configuration", defaultValue = "test")
    String getEndPointConfigID();
    
	@AttributeDefinition(name = "Service", description = "Service. Enter campaign, stock, analytics, etc...", defaultValue = "")
	String getService();
    
    @AttributeDefinition(name = "Service specific Header", description = "egg: x-product:app-name,custom-header:custom,...", defaultValue = "")
    String getSpecificServiceHeader();

    String webconsole_configurationFactory_nameHint() default "ACS Endpoint <b>{getTenant} - {getId}</b><br/> {getMethod} -&gt; {getEndpoint}";

}
