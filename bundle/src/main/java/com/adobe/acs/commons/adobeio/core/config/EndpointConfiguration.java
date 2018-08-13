package com.adobe.acs.commons.adobeio.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "ACS AEM Commons - Adobe I/O. Endpoint Factory Configuration",
        description = "Configuration of Adobe.io endpoints")
public @interface EndpointConfiguration {

    @AttributeDefinition(name = "ID", description = "Id of the endpoint")
    String getId();

    @AttributeDefinition(
    	name = "Method",
    	description = "Used method for the endpoint",
		options = {
				@Option(label = "DELETE", value = "DELETE"),
                @Option(label = "GET", value = "GET"),
                @Option(label = "PATCH", value = "PATCH"),
                @Option(label = "POST", value = "POST"),
                @Option(label = "PUT", value = "PUT"),  
        }
    )
    String getMethod();

    @AttributeDefinition(name = "URL Endpoint", description = "Endpoint, without /campaign/, but starting with /")
    String getEndpoint();
    
    @AttributeDefinition(name = "Adobe I/O Domain", description = "Domain of Adobe I/O. egg: https://[mc|stock|...].adobe.io", defaultValue = "https://mc.adobe.io")
	String getIODomain();
    
    @AttributeDefinition(name = "Service specific Header", description = "{header_key:header_value} egg: x-product:app-name")
    String[] getSpecificServiceHeader();

    String webconsole_configurationFactory_nameHint() default "Endpoint <b>{getTenant} - {getId}</b><br/> {getMethod} -&gt; {getEndpoint}";

}
