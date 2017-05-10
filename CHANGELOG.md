# Change Log

All notable changes to this project will be documented in this file. This format was adapated
after the 3.9.0 release. All changes up until the 3.9.0 release can be found in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com/)

## [Unreleased]

[Unreleased]: https://github.com/Adobe-Consulting-Services/acs-aem-commons/compare/acs-aem-commons-3.9.0...HEAD

### Added

- `native` QueryBuilder PredicateEvaluator
- `com.adobe.acs.commons.fam.actions.ActionsBatch` for bundling Fast Action Manager actions so multiple changes can be retried if any of them fail and break the commit.

### Changed

- Updated Fast Action Manager retry logic to support more failure cases properly.
- Updated Fast Action Manager retry logic to  be savvy about interrupted exceptions thrown by the watchdog trying to kill the thread.

<!---

### Fixed
 
### Removed

---->
