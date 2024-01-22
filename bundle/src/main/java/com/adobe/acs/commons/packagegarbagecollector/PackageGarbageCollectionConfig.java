/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.packagegarbagecollector;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "ACS Commons - Package Garbage Collection Configuration", description = "Used to config a package garbage collection job.")
public @interface PackageGarbageCollectionConfig {
    @AttributeDefinition(name = "Schedule", description = "Cron expression detailing when the garbage collection is run. Default runs at 02:30 every day.")
    String scheduler() default "0 30 2 ? * * *";

    @AttributeDefinition(name = "Package group name", description = "The group name of the packages to remove.")
    String groupName() default "";

    @AttributeDefinition(name = "Max upload age of package", description = "Packages uploaded more than the given amount of days ago will be removed. Default is 60 days.")
    int maxAgeInDays() default 60;

    @AttributeDefinition(name = "Remove not installed packages", description = "Remove packages that are not installed (or have been uninstalled) and  the created date is older than maxAgeInDays. Default is false.")
    boolean removeNotInstalledPackages() default false;

    @AttributeDefinition(name = "webconsole.configurationFactory.nameHint")
    String webconsole_configurationFactory_nameHint() default "Package Garbage Collection - Clear packages in {groupName} older than {maxAgeInDays} days using the schedule [{scheduler}]";
}
