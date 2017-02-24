package com.adobe.acs.commons.users.impl;

import aQute.bnd.annotation.ProviderType;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

@ProviderType
@Description("ACS AEM Commons - Ensure Service User MBean")
public interface EnsureServiceUserManager {

    @Description("Execute all Ensure Service User configurations")
    void ensureAll();

    @Description("Execute all Ensure Service User configurations for the provided principal name")
    void ensurePrincipalName(@Name(value="Principal Name")String principalName);
}
