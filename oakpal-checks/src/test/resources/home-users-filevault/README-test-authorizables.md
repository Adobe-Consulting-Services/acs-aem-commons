Creating test authorizables
===========================

1. Create the rep:AuthorizableFolder structure using .content.xml files copied from jcr_root/home/users/.content.xml

2. Create a folder for each authorizable

3. Copy an appropriate .content.xml from a different existing authorizable. Change the rep:authorizableId and
rep:principalName to the desired userId, and ensure that they match.

4. Ensure the path is covered in META-INF/vault/filter.xml.

5. Set a breakpoint in org.apache.jackrabbit.oak.commons.UUIDUtils on about line 36
(as of org/apache/jackrabbit/oak-commons/1.7.14/oak-commons-1.7.14-sources.jar), such that you can inspect the generated
UUID value when paused at `return uuid.toString();` in the `static String generateUUID(String)` method.

6. Execute the `AcsCommonsAuthorizableCompatibilityCheckTest#testCheckNone` test in your IDE. Execution will pause at
breakpoint you set in step 5 for each authorizable added to the transient Oak repository. Keep continuing execution
until the value of the hint argument for the generateUUID method matches your authorizable's rep:authorizableId value,
at which point, copy the String value of the uuid object and paste it into the `jcr:uuid` attribute of the associated
.content.xml file, replacing any previous value that was copied over.