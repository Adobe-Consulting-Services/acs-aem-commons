package com.adobe.acs.commons.version;

import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.resource.JcrResourceUtil;

public class EvolutionConfig {

	private String[] ignoreProperties;
	private String[] ignoreResources;

	public EvolutionConfig(String[] ignoreProperties, String[] ignoreResources) {
		this.ignoreProperties = ignoreProperties;
		this.ignoreResources = ignoreResources;
	}
	
	public int getDepthForPath(String path){
		return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:frozenNode"), "/");
	}
	
	public String getRelativePropertyName(String path){
		return StringUtils.substringAfterLast(path, "jcr:frozenNode").replaceFirst("/", "");
	}
	
	public String getRelativeResourceName(String path){
		return StringUtils.substringAfterLast(path, "jcr:frozenNode/");
	}
	
	public boolean handleProperty(String name) {
		for(String entry : ignoreProperties){
			if(Pattern.matches(entry, name)){
				return false;
			}
		}
		return true;
	}

	public boolean handleResource(String name) {
		for(String entry : ignoreResources){
			if(Pattern.matches(entry, name)){
				return false;
			}
		}
		return true;
	}
	
	public String printProperty(javax.jcr.Property property) {
		try {
			return printObject(JcrResourceUtil.toJavaObject(property));
		} catch (RepositoryException e1) {
			return e1.getMessage();
		}
	}

	public String printObject(Object obj) {
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
