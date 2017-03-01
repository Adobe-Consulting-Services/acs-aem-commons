package com.adobe.acs.commons.cmf;

import javax.jcr.Session;

public class MigrationOptions {

	Session session;
	boolean dryRun;
	
	
	public MigrationOptions() {
		session = null;
		dryRun = false;
	}
	
	
	public MigrationOptions setSession (Session s) {
		session = s;
		return this;
	}
	
	public MigrationOptions setDryRun (boolean dryrun) {
		this.dryRun = dryrun;
		return this;
	}
	
}
