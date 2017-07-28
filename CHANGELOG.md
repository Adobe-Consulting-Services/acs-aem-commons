# Change Log

All notable changes to this project will be documented in this file. This format was adapated
after the 3.9.0 release. All changes up until the 3.9.0 release can be found in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com/)

## [Unreleased]

[Unreleased]: https://github.com/Adobe-Consulting-Services/acs-aem-commons/compare/acs-aem-commons-3.9.0...HEAD

### Added

- Managed Controlled Processes framework with 4 sample tools: Folder Relocator, Asset Report (space usage), Deep Prune, Asset Ingestor (aka AntEater v2)
- `com.adobe.acs.commons.fam.actions.ActionsBatch` for bundling Fast Action Manager actions so multiple changes can be retried if any of them fail and break the commit.
- Fast Action Manager now has a halt feature in the API which instantly stops an action manager and any of its scheduled work
- Asset Folder Properties Support to allow custom fields/properties to be added to AEM Assets Folders in UI
- Content Modification Framework API
- Named Image Transform Servlet Sharpen transform 
- AEM Assets Brand Portal workflow process and Agent filter

### Changed

- Updated Fast Action Manager retry logic to support more failure cases properly.
- Updated Fast Action Manager retry logic to be savvy about interrupted exceptions thrown by the watchdog trying to kill the thread.
- #1033: Allow Rewrite attributes to be passed in as an array

### Fixed

- Error page handler OSGi configuration missing web hint for 'not-found' behavior.
- Touch UI Multi-field saved User-picker values were not populated in dialog 
- #1051: Emails sent via EmailService do not have connection/socket timeouts 
- #982: Fixed issue with Touch UI Icon Picker was prefixing icon classes with 'fa' 
- #108: Email subject mangled for non-latin chars
- #1043: JCR Package Replication now populates the replicated by properties of the packaged resources with the actual user that requested the replication of the package (with configurable override via OSGi config for backwards compat) 
- #1044: JCR Package Replication fixes a resource leak where the JCR Packages were not closed after being opened 
- Fast Action Manager is much more efficient in how it gauges CPU usage, which makes it even faster than before.

<!---
 
### Removed

---->
