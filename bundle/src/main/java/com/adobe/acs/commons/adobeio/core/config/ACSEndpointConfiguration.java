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

    String webconsole_configurationFactory_nameHint() default "ACS Endpoint <b>{getTenant} - {getId}</b><br/> {getMethod} -&gt; {getEndpoint}";
}
