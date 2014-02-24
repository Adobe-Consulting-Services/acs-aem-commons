package com.adobe.acs.commons.twitter;

import org.apache.commons.lang.StringUtils;

public class TwitterConfiguration {

	private String username;

	private String consumerKey;

	private String consumerSecret;

	public TwitterConfiguration() {

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getConsumerKey() {
		return consumerKey;
	}

	public void setConsumerKey(String accessToken) {
		this.consumerKey = accessToken;
	}

	public String getConsumerSecret() {
		return consumerSecret;
	}

	public void setConsumerSecret(String accessTokenSecret) {
		this.consumerSecret = accessTokenSecret;
	}

	public boolean isValid() {
		if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(consumerKey)
				&& !StringUtils.isEmpty(consumerSecret))
			return true;

		return false;

	}

}
