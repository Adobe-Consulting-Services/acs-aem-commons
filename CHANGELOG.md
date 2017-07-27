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
- Updated Fast Action Manager retry logic to  be savvy about interrupted exceptions thrown by the watchdog trying to kill the thread.

### Fixed

- Error page handler OSGi confiuration missing web hit for 'not-found' behavior.
- Touch UI Multi-field saved Userpicker values were not populated in dialog
- Emails sent via EmailService do not have connection/socket timeouts #1051
- Fast Action Manager is much more efficient in how it gauges CPU usage, which makes it even faster than before.

<!---
 
### Removed

---->
