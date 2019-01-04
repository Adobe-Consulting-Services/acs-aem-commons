package com.adobe.acs.commons.fam.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Throttled task runner configuration
 * <p>
 * Provides config values for the ThrottledTaskRunner
 * </p>
 *
 * @author niek.raaijkmakers@external.cybercon.de
 * @since 2019-01-04
 */
@ObjectClassDefinition(name = "ACS AEM Commons - Throttled Task Runner Service",
        description = "WARNING: Setting a low 'Watchdog time' value that results in the interrupting of writing threads can lead to repository corruption. Ensure that this value is high enough to allow even outlier writing processes to complete.")
public @interface Config {

    double DEFAULT_MAX_CPU = 0.75;
    double DEFAULT_MAX_HEAP = 0.85;
    int DEFAULT_MAX_THREADS = 4;
    int DEFAULT_COOLDOWN_TIME = 100;
    int DEFAULT_TASK_TIMEOUT = 3600000;

    @AttributeDefinition(name = "Max threads", description = "Default is 4, recommended not to exceed the number of CPU cores", defaultValue = "" + DEFAULT_MAX_THREADS)
    int max_threads() default DEFAULT_MAX_THREADS;

    @AttributeDefinition(name = "Max cpu %", description = "Range is 0..1; -1 means disable this check", defaultValue = ""+DEFAULT_MAX_CPU)
    double max_cpu() default DEFAULT_MAX_CPU;

    @AttributeDefinition(name = "Max heap %", description = "Range is 0..1; -1 means disable this check", defaultValue = "" + DEFAULT_MAX_HEAP)
    double max_heap() default DEFAULT_MAX_HEAP;

    @AttributeDefinition(name = "Cooldown time", description = "Time to wait for cpu/mem cooldown between checks", defaultValue = "" + DEFAULT_COOLDOWN_TIME)
    int cooldown_wait_time() default DEFAULT_MAX_THREADS;

    @AttributeDefinition(name = "Watchdog time", description = "Maximum time allowed (in ms) per action before it is interrupted forcefully. Defaults to 1 hour.", defaultValue = "" + DEFAULT_TASK_TIMEOUT)
    int task_timeout() default DEFAULT_TASK_TIMEOUT;
}
