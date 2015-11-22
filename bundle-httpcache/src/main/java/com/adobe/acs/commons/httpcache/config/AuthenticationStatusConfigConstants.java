package com.adobe.acs.commons.httpcache.config;

/**
 * Constants used to represent authentication status in {@link HttpCacheConfig}.
 */
public final class AuthenticationStatusConfigConstants {
    private AuthenticationStatusConfigConstants() {
        throw new Error(AuthenticationStatusConfigConstants.class.getName() + " is not meant to be instantiated.");
    }

    /** Unauthenticated public requests */
    public static final String ANONYMOUS_REQUEST = "anonymous";
    /** Authenticated requests */
    public static final String AUTHENTICATED_REQUEST = "authenticated";
    /** Both the authenticated and unauthenticated requests */
    public static final String BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS = "both";
}
