# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ACS AEM Commons is a community library of reusable AEM (Adobe Experience Manager) components, utilities, and OSGi services. It targets both AEM as a Cloud Service (AEMaaCS) and AEM 6.5.x.

## Build Commands

```bash
# Build everything (AEM 6.5 only)
mvn clean verify

# Build and deploy to local AEM 6.5 instance (localhost:4502)
mvn -PautoInstallPackage clean install

# Build and deploy to local AEMaaCS instance
mvn -PautoInstallPackage,cloud clean install

# Build and deploy only the OSGi bundle
mvn -PautoInstallBundle clean install -pl bundle

# Build and deploy only the ui.apps package
mvn -PautoInstallUiAppsPackage clean install

# Run tests only (no install)
mvn clean verify -pl bundle

# Run a single test class
mvn test -pl bundle -Dtest=RedirectRuleTest

# Run a single test method
mvn test -pl bundle -Dtest=RedirectRuleTest#testSomething

# Deploy to a non-default AEM host/port
mvn -PautoInstallPackage clean install -Dcrx.host=otherhost -Dcrx.port=5502

# Fix/add license headers to Java files
mvn license:update-file-header

# Skip the cloud profile (build AEM 6.5 artifacts only)
mvn clean verify -P '!cloud'

# Run with code coverage
mvn clean verify -Pcoverage
```

## Module Structure

The project is a Maven multi-module build:

- **`bundle/`** — Main OSGi bundle. Contains the vast majority of Java source code. Target: both AEM 6.5 and AEMaaCS.
- **`bundle-cloud/`** — OSGi fragment bundle with code only for AEMaaCS (e.g. `email/`, `replication/`).
- **`bundle-onprem/`** — OSGi fragment bundle with code only for AEM 6.5 (e.g. `email/`, `logging/`).
- **`ui.apps/`** — AEM content package for JCR content: components, client libraries, dialogs, datasources, overlays.
- **`ui.content/`** — Sample/default JCR content.
- **`ui.config/`** — OSGi configurations (runmode-specific).
- **`content/`** — Vault filter and packaging metadata.
- **`all/`** — Aggregator package that embeds all sub-packages; the deployable unit.
- **`oakpal-checks/`** — Custom OakPAL checks for content package validation.

## Key Architectural Patterns

### OSGi Services
Most Java code registers as OSGi components using `@Component`, `@Service`, and `@Model` annotations from the Sling/OSGi ecosystem. Services that should remain inactive until explicitly configured must use `policy = ConfigurationPolicy.REQUIRE` — this applies especially to Filters, Scheduled Services, Event Listeners, and Authentication Handlers.

### Sling Models
`bundle/src/main/java/com/adobe/acs/commons/` organizes features by domain (e.g. `redirects/`, `httpcache/`, `workflow/`). Each feature typically has:
- `models/` — Sling Models (`@Model`) adaptable from `Resource` or `SlingHttpServletRequest`
- `servlets/` — Sling Servlets (`@SlingServlet`)
- `filter/` — Sling Filters (require `ConfigurationPolicy.REQUIRE`)
- `impl/` — OSGi service implementations

### Cloud vs On-Prem Splits
When a feature needs different implementations for AEMaaCS vs AEM 6.5, the common interface/API stays in `bundle/`, and the platform-specific implementations go in `bundle-cloud/` or `bundle-onprem/`.

### Redirect Manager (active area of work)
The Redirect Manager lives in `bundle/src/main/java/com/adobe/acs/commons/redirects/` and `ui.apps/src/main/content/jcr_root/apps/acs-commons/content/redirect-manager/`. Key classes:
- `RedirectRule` — Sling Model representing one redirect entry stored in the JCR; read from a `Resource` or `SlingHttpServletRequest`
- `RedirectFilter` — Sling Filter (requires OSGi config) that intercepts requests, caches rules via Guava Cache, and performs 301/302 redirects
- `MatchType` enum — controls how the source URL is interpreted: `AUTO_DETECT`, `PLAIN_TEXT`, `REGEX`
- `ExportColumn` enum — defines the columns in the Excel import/export spreadsheet
- Servlets handle import (xlsx), export (xlsx), replication, and rewrite-map generation

### Testing
Tests use JUnit 5 with Sling Mocks (`io.wcm.testing.aem-mock.junit5` / `SlingContext`) and Mockito. The `AemContext` or `SlingContext` context rule sets up an in-memory Sling/JCR environment. Test resources (JSON fixtures) are placed under `src/test/resources/` alongside the test class package path.

## Contribution Rules

- Use spaces, not tabs.
- API classes and interfaces require Javadoc; implementation classes do not.
- Do not use `@author` tags.
- Add an entry to `CHANGELOG.md` with every change.
- All new features must target AEM as a Cloud Service by default. AEM 6.5-only features require a Feature Review GitHub Issue first.
- Third-party dependencies must either be marked `optional` in OSGi imports or shaded via `maven-shade-plugin` (see `CONTRIBUTING.md` for the multi-step embedding procedure). Shaded packages are prefixed `acscommons.` to avoid classpath conflicts.
- Client libraries must not auto-load into AEM categories; they require an explicit `<ui:includeClientLib>` reference.
