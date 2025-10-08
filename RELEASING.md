# Release Process

## Prerequistes

* You must have commit rights on this repository.
* You must have an account with deploy rights to [Sonatype's Central Portal][central-portal] for namespace `com.adobe.acs`.

### Setup

In your Maven settings.xml file (~/.m2/settings.xml), add a server entry with the id `ossrh`. The credentials are your [Central Portal User Token][central-portal-token].
The password should only be [stored in encrypted form](http://maven.apache.org/guides/mini/guide-encryption.html#How_to_encrypt_server_passwords):

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
        ...
        <servers>
            <server>
                <id>sonatype-central-portal</id>
                <username>central-portal-token-user</username>
                <password>central-portal-token-password</password>
                <configuration>
                    <!-- njord publisher id for central portal, https://maveniverse.eu/docs/njord/using-it/ -->
                    <njord.publisher>sonatype-cp</njord.publisher>
                    <!-- the njord validation template to use, https://maveniverse.eu/docs/njord/what-is-it/ -->
                    <njord.releaseUrl>njord:template:release-sca</njord.releaseUrl>
                </configuration>
            </server>
        </servers>
        ...
    </settings>

These credentials are used to deploy to [Sonatype's Central Portal][central-portal].

In addition you need to setup [GPG](https://central.sonatype.org/pages/working-with-pgp-signatures.html) to create OpenPGP signatures. After installing https://www.gnupg.org/download/ you need to create key pair (if you don't have one yet) and make sure that the public key is distributed via hkp://pool.sks-keyservers.net.
e.g. `gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys YOUR_KEY_ID_HERE` -- OSSRH also checks hkp://keys.openpgp.org and hkp://keyserver.ubuntu.com as backups so it might be a good idea to upload there.

It is recommended that your private key is protected with a passphrase. You can persist the passphrase in the settings.xml as well

     <server>
         <!-- has the passphrase for the gpg signing in encrypted format: http://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html#passphraseServerId -->
         <id>gpg.passphrase</id>
         <!-- passphrase for your private key -->
         <password>****</password>
    </server>


### Prior to release

Make sure that all issues assigned to the current milestone have been closed and all necessary pull requests have been merged and closed out.  Don't proceed with the release until you know what you're releasing. ;)

### Release Process

1. Add a new heading to the CHANGELOG.md file with the release number and today's date. Change the link for the Unreleased changes. See https://github.com/Adobe-Consulting-Services/acs-aem-commons/commit/9a3f6e44870b8d5be353f7dd6fd6cf0d47a872fd for an example. Commit and push this change.

2. If this is a minor release, create two new Milestones in GitHub -- one for the next minor release and one for the first patch release. For example,
if you are releasing 3.18.0, create 3.20.0 and 3.18.2.

3. If this is a patch release, create a new Milestone in GitHub for the next patch release. For example, if you are releasing 3.18.2, create 3.18.4.

4. Close the current milestone in GitHub issues.

5. Make sure that the issues and pull requests are associated with the proper milestone -- anything open for the current release should be moved to the next release, either minor or patch depending on the nature of the issue.

6. Ensure **Java 8** is active (Java 11 breaks on the JavaDocs build in `mvn release:perform`)

7. Run the release: `mvn release:prepare` followed by `mvn release:perform`. You may need to pass `-Dgpg.passphrase=****` if your passphrase is not persisted in your `settings.xml`.  If you want to enter your passphrase manually at a prompt, add this to .bashrc or execute prior to mvn release: `export GPG_TTY=$(tty)` and you can verify it works via `echo "test" | gpg --clearsign`

8. Check and validate the stage repositories content as outlined in <https://maveniverse.eu/docs/njord/using-it/#using-it>. If everything is fine publish the stage repository via [`mvn njord:publish`](https://maveniverse.eu/docs/njord/plugin-documentation/publish-mojo.html)

9. Go to https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases and edit the release tag, using the CHANGELOG data as the release text and attaching the content package zip files (both min and regular) to the release.

10. Publish the remote bundle in https://central.sonatype.com/publishing/deployments to Maven Central. Publishing the remote bundle automatically pushes the artifacts to Maven Central after a small delay (usually some minutes).

10. Add a release announcement (and any other docs) to the documentation site.

11. If this is a minor release, check out the release tag and run the script `copy-javadoc.sh` to update the JavaDoc on the documentation site. Commit and push the changes the script makes.  Note: This script assumes you have the docs site checked out in a directory called `adobe-consulting-services.github.io`


[central-portal]: https://central.sonatype.org/register/central-portal/
[central-portal-token]: https://central.sonatype.org/publish/generate-portal-token/

