package com.adobe.acs.commons.version;

import java.util.GregorianCalendar;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.resource.JcrResourceUtil;

public class EvolutionHelper {
	
	public static String printProperty(Property property) {
		try {
			return printObject(JcrResourceUtil.toJavaObject(property));
		} catch (RepositoryException e1) {
			return e1.getMessage();
		}
	}

	public static String printObject(Object obj) {
		if (obj == null) {
			return "";
		}
		if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof String[]) {
			String[] values = (String[]) obj;
			String result = "[";
			for (int i = 0; i < values.length; i++) {
				result += values[i];
				if (i != (values.length - 1)) {
					result += ", ";
				}
			}
			result += "]";
			return result;
		} else if (obj instanceof GregorianCalendar) {
			GregorianCalendar value = (GregorianCalendar) obj;
			return value.getTime().toGMTString();
		} else {
			return obj.toString();
		}
	}

}
