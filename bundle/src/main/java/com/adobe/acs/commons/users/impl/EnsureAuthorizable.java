package com.adobe.acs.commons.users.impl;

public interface EnsureAuthorizable {

    Operation getOperation();

    AbstractAuthorizable getAuthorizable();

    void ensure(Operation operation, AbstractAuthorizable authorizable) throws EnsureAuthorizableException;
}
