package com.adobe.acs.commons.cmf;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;


/**
 * This class implements the Visitor pattern for the identifyResources() method.
 *
 */
public abstract class TraversingContentModificationStep implements ContentModificationStep {

	@Override
	public List<Resource> identifyResources(Resource rootResource) {
		
		List<Resource> result = new LinkedList<Resource>();
		return visit (result, rootResource);
	}
	
	/**
	 * Visits the resource r and its sub-resources. 
	 * @param r
	 * @return
	 */
	private List<Resource> visit (List<Resource> list, Resource r) {
		
		/**
		 * This implementation tries to avoid the list.addAll method
		 */
		
		Resource foundResource = accept (r);
		if (foundResource != null) {
			list.add(foundResource);
		}
		Iterator<Resource> iter =  r.listChildren();
		while (iter.hasNext()) {
			Resource innerResource = iter.next();
			if (shouldVisitAndDescend(innerResource)) {
				list = (visit (list, innerResource));
			}
		}
		return list;
		
	}

	
	/**
	 * <code>accept</code> is called for every resource being visited.
	 * @return the resource if the resource should considered be for the inclusion in the result 
	 *   delivered by identifyResources; null otherwise
	 */
	public abstract Resource accept (Resource resource);
	
	
	/**
	 * <codeshouldVisitAndDescend</code> is used to determine if this resource (and all sub-resources)
	 * should be visited.
	 * @param resource the resource
	 * @return true if the resource should be visited.
	 */
	
	public boolean shouldVisitAndDescend (Resource resource) {
		return true;
	}
	
	
	
	
}
