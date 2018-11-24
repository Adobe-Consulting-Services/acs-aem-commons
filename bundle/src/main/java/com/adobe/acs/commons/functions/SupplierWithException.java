package com.adobe.acs.commons.functions;


@FunctionalInterface
public interface SupplierWithException<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}
