package com.adobe.acs.commons.ondeploy.scripts;

/**
 * An exception representing a failed on-deploy script.
 */
public class OnDeployScriptException extends RuntimeException {
    public OnDeployScriptException(Throwable cause) {
        super("On-deploy script failure", cause);
    }
}
