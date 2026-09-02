# Changelog

Notable changes to the Jira Watcher Field plugin. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions correspond to git tags
named `jira-watcher-field-<version>`.

## [3.0.0-rc3] - unreleased

### Added
- Unit tests for `WatcherFieldType` (value equality, changelog rendering, add-watcher
  permission filtering) with JUnit 5 + Mockito, run by the AMPS build's surefire.
- GitHub Actions CI: builds the plugin and runs the tests on every push and pull request.
- Atlassian repositories declared in the pom, so the project builds with plain Maven on a
  clean machine (previously only `atlas-mvn` with the SDK's settings worked).
- Project metadata in the pom (`url`, `scm`, `licenses`, `issueManagement`, maintainer).
- `TODO.md` recording known defects and improvements from the post-rc2 code review.
- This changelog.

### Changed
- Warnings are now logged when watcher mutations are silently skipped (permission or
  editability gates), since Jira may still record a changelog entry for the field.
- `isUserPermitted` checks for a logged-in user before reading settings, avoiding uncached
  database reads on every field render.
- README rewritten: correct description of what the field does, plus install, configuration
  and build documentation.

### Removed
- Stale generated javadoc (`doc/`, 84 files from 2012 documenting mostly-removed classes).
- Unused `commons-lang3` dependency; OSGi `Import-Package` collapsed to `*`.

## [3.0.0-rc2] - 2026-09-02

### Fixed
- Settings screen returned **405** on stock Jira 11.3: webwork actions are POST-only by
  default there; added `@SupportedMethods` (GET on the class, POST on `doEdit`).
- Saving settings failed the XSRF check: added `@RequiresXsrfCheck` and the `atl_token`
  hidden field to the edit form.
- The field searcher never worked (its indexer threw `ClassCastException` on every issue):
  rebased `WatcherSearcher` on Jira's own `UserPickerGroupSearcher`.

### Security
- Settings action hardened with `@AdminOnly`, `@WebSudoRequired` and
  `roles-required="admin"` on the webwork actions.

## [3.0.0-rc1] - 2026-09-01

### Changed
- Jira Data Center 11 compatibility (Platform 8): JDK 21, Spring 6, Jakarta EE 10,
  `jakarta.inject` annotations, Atlassian Spring Scanner 6, SLF4J logging, AMPS 9.13.

## [2.10.0] - 2025-09-23

### Changed
- Jira Data Center 10 compatibility improvements on top of 2.6.0.

## [2.6.0] - 2025-07-25

### Changed
- First Jira Data Center 10 compatible release by this fork.

## Earlier versions

Versions 1.0 (2009) through 2.5.5 (2012) by the original authors predate this changelog;
see the git tags for their history.
