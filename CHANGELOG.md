# Changelog

Notable changes to the Jira Watcher Field plugin. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions correspond to git tags
named `jira-watcher-field-<version>`.

## [3.0.0-rc3] - unreleased

### Security
- The acting user's *Manage Watchers* permission and the added watcher's *Browse Projects*
  permission are now checked at issue level, matching Jira's own watcher service. The
  previous project-level checks ignored issue security levels entirely and passed
  issue-scoped permission grants (Current Assignee, Reporter, user custom field) for every
  user in the project.

### Fixed
- Moving an issue (single, subtask or bulk) to a project or issue type where the field is
  not configured no longer removes all watchers: Jira's system-initiated field clear is
  now distinguished from a user deliberately emptying the picker, which still works.
- Editing an issue as a user without *Manage Watchers* no longer records a false
  "Watchers → None" change in the issue history (and no longer clears watchers via bulk
  edit): the read-only field now round-trips the current watcher list on submit. Names
  already watching an issue are exempt from picker validation, so a deactivated or
  deleted watcher can no longer block the edit screen.
- The read-only watcher list shown to users without permission now renders usernames
  instead of escaped HTML markup.
- A transient database error while reading the settings no longer silently overwrites the
  configured `ignorePermissions` value with `false`; bad stored data is only repaired on
  an explicit admin save, and read errors fail closed with a warning.

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
- Settings reads now go through Jira's cluster-aware cached property set instead of
  issuing uncached database queries on every anonymous/service render of the field.
- Warnings are now logged when watcher mutations are silently skipped (permission or
  editability gates), since Jira may still record a changelog entry for the field.
- `isUserPermitted` checks for a logged-in user before reading settings, so the settings
  lookup no longer runs on every field render.
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
