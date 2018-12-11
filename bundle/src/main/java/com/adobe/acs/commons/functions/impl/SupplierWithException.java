package com.adobe.acs.commons.functions.impl;


@FunctionalInterface
public interface SupplierWithException<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    @SuppressWarnings("squid:S00112")
    T get() throws Exception;
}
