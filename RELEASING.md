# Release Process

## Prerequistes

* You must have commit rights on this repository.
* You must be a member of the ACS organzation on bintray (https://bintray.com/acs/).

### Setup

In your Maven settings.xml file (~/.m2/settings.xml), add a server entry with the id `bintray`. The username is your bintray username.
The password is your API key (not your password), which you can find via https://bintray.com/profile/edit :

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
        ...
        <servers>
            <server>
                <id>bintray</id>
                <username>justinedelson</username>
                <password>*****</password>
            </server>
        </servers>
        ...
    </settings>

### Prior to release

Make sure that all issues assigned to the current milestone have been closed and all necessary pull requests have been merged and closed out.  Don't proceed with the release until you know what you're releasing. ;)

### Release Process

1. Add a new heading to the CHANGELOG.md file with the release number and today's date. Change the link for the Unreleased changes. See https://github.com/Adobe-Consulting-Services/acs-aem-commons/commit/9a3f6e44870b8d5be353f7dd6fd6cf0d47a872fd for an example. Commit and push this change.

2. If this is a minor release, create two new Milestones in GitHub -- one for the next minor release and one for the first patch release. For example,
if you are releasing 3.18.0, create 3.20.0 and 3.18.2.

3. If this is a patch release, create a new Milestone in GitHub for the next patch release. For example, if you are releasing 3.18.2, create 3.18.4.

4. Close the current milestone in GitHub issues.

5. Make sure that the issues and pull requests are associated with the proper milestone -- anything open for the current release should be moved to the next release, either minor or patch depending on the nature of the issue.

6. Run the release: `mvn release:prepare` followed by `mvn release:perform`.

7. Go to https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases and edit the release tag, using the CHANGELOG data as the release text and attaching the content package zip files (both min and regular) to the release.

8. Log into Bintray and go to https://bintray.com/acs/releases, publish all of the artifacts.

9. Create a new internal Adobe JIRA issue requesting that the artifacts be promoted to repo.adobe.com.

10. After the files are promoted, add a release announcement (and any other docs) to the documentation site.

11. If this is a minor release, check out the release tag and run the script `copy-javadoc.sh` to update the JavaDoc on the documentation site. Commit and push the changes the script makes.  Note: This script assumes you have the docs site checked out in a directory called `adobe-consulting-services.github.io`
