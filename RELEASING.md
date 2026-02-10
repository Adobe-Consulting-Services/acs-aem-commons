# Release Process

## Prerequistes

* You must have commit rights on this repository.

## GHA based Process

## Manual pre-release steps

1. Add a new heading to the CHANGELOG.md file with the release number and today's date. Change the link for the Unreleased changes. See https://github.com/Adobe-Consulting-Services/acs-aem-commons/commit/9a3f6e44870b8d5be353f7dd6fd6cf0d47a872fd for an example. Commit and push this change.

2. If this is a minor release, create two new Milestones in GitHub -- one for the next minor release and one for the first patch release. For example,
if you are releasing 3.18.0, create 3.20.0 and 3.18.2.

3. If this is a patch release, create a new Milestone in GitHub for the next patch release. For example, if you are releasing 3.18.2, create 3.18.4.

4. Close the current milestone in GitHub issues.

5. Make sure that the issues and pull requests are associated with the proper milestone -- anything open for the current release should be moved to the next release, either minor or patch depending on the nature of the issue.

## Automated release

One can trigger the release from GitHub Actions with the workflow [Release to GitHub and Maven Central](https://github.com/Adobe-Consulting-Services/acs-aem-commons/actions/workflows/release.yml).

This requires the following parameters

1. Release version: The version the new release version should have. The SNAPSHOT version in the master branch will be automatically set to same version with `<patch>` being incremented by one and ending with `-SNAPSHOT`.
2. Email Release Manager: An email address associated with the release commits to the Git repository. You can use the [GitHub noreply address](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference#your-noreply-email-address).
3. A personal access token used for performing the write operations on the Git repository. Set it up in [Personal access tokens (classic)](https://github.com/settings/tokens). *[Fine-granied personal access tokens](https://github.com/settings/personal-access-tokens)* require [approval](https://docs.github.com/en/organizations/managing-programmatic-access-to-your-organization/managing-requests-for-personal-access-tokens-in-your-organization) from the organization owner (only <https://github.com/davidjgonzalez> at the moment) while classic ones have the repository access as the underlying user and don't require approval.

It uses a PGP key and Sonatype Central Portal credentials stored in the repository secrets. The release process will perform the following steps:

1. executes both [`release:prepare`](https://maven.apache.org/maven-release/maven-release-plugin/prepare-mojo.html) and [`release:perform`](https://maven.apache.org/maven-release/maven-release-plugin/perform-mojo.html) on the master branch
2. uses [Njord](https://maveniverse.eu/docs/njord/) to deploy the generated release artifacts to Central Portal (with `autoPublish` being set to `true`, i.e. no additional steps required before artifacts are pushed to Maven Central).
3. creates a new [GitHub release](https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases) as draft

## Manual post-release steps

The following manual steps need to be performed afterwardsL:

1. Review and publish the draft [release notes](https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases)
1. Add a release announcement (and any other docs) to the [documentation site](https://github.com/Adobe-Consulting-Services/adobe-consulting-services.github.io). At least update [`version` in acs-aem-commons.yml](https://github.com/Adobe-Consulting-Services/adobe-consulting-services.github.io/blob/master/_data/acs-aem-commons.yml).

## Manual Process (Legacy)

* You must have an account at [Sonatype's Central Portal][central-portal] with deploy rights for namespace/groupId `com.adobe.acs`.

### Setup

In your Maven settings.xml file (`~/.m2/settings.xml`), add a server entry with the id `sonatype-central-portal`. The credentials must be a [Central Portal User Token][central-portal-token].
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

7. Run the release: `mvn release:prepare` followed by `mvn release:perform`. You may need to pass `-Dgpg.passphrase=****` if your passphrase is not persisted in your `settings.xml`.  If you want to enter your passphrase manually at a prompt, add this to .bashrc or execute prior to mvn release: `export GPG_TTY=$(tty)` and you can verify it works via `echo "test" | gpg --clearsign`. This will automatically publish the release artifacts to Sonatype's Central Portal and from there publish further to Maven Central. The build waits for the deploy to finish therefore might take a bit.

8. Go to https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases and edit the release tag, using the CHANGELOG data as the release text and attaching the content package zip files (both min and regular) to the release.

9. Add a release announcement (and any other docs) to the documentation site.


[central-portal]: https://central.sonatype.org/register/central-portal/
[central-portal-token]: https://central.sonatype.org/publish/generate-portal-token/

