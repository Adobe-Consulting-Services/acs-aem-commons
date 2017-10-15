package com.adobe.acs.commons.jcrpersist.extension;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * An extension point that can be used to provide reading
 * via {@link ValueMap} for JCR objects that do not directly
 * provide that functionality.
 * 
 * @author sangupta
 *
 */
public interface ValueMapReader {
	
	public ValueMap readValueMap(Resource resource);

}
