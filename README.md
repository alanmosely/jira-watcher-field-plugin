# Jira Watcher Field Plugin

A Jira Data Center plugin that provides a **Watcher Field** custom field type. The field lets
users with the *Manage Watchers* permission set an issue's watchers directly from the
create and edit screens (and from anything else that writes fields, such as post functions or
scripts). It does not store a value of its own — it mirrors the issue's real watcher list and
applies changes through Jira's watcher service, so edits show up in the normal watcher list,
notifications and issue history.

## Compatibility

| Plugin version | Jira Data Center | Notes                                        |
| -------------- | ---------------- | -------------------------------------------- |
| 3.x            | 11.x (11.3 LTS)  | Platform 8: JDK 21, Spring 6, Jakarta EE 10  |
| 2.6.0, 2.10.x  | 10.x             | 2.6.0 was this fork's first Jira 10 build    |
| ≤ 2.5.x        | ≤ 9.x            | Historical releases, unmaintained            |

## Installation

The plugin is not on the Atlassian Marketplace. Build the jar (see below) or take one from a
[GitHub release](https://github.com/alanmosely/jira-watcher-field-plugin/releases), then upload
it in **Administration → Manage apps → Upload app**.

## Usage

1. Create a custom field of type **Watcher Field** (Administration → Issues → Custom fields)
   and add it to the relevant screens.
2. Users who hold **Manage Watchers** on the project can then set watchers while creating
   or editing an issue. Users without the permission see the current watcher list read-only.
3. Watchers added through the field must have the **Browse Projects** permission; anyone else
   in the submitted list is skipped with a logged warning, though the issue history may still
   record them as added.

On update the field applies *replace* semantics: watchers missing from the submitted list are
removed, new entries are added.

## Settings

**Administration → Manage apps → Watchers Custom Field → Watcher Field Settings** holds one
option:

- **Allow JIRA to add watchers independently** (`ignorePermissions`, default off): lets the
  field change watchers when there is no logged-in user, e.g. default watchers on issues
  created by an incoming mail handler or another service context.

  **Warning:** this bypasses the *Manage Watchers* permission check for every request
  without a logged-in user, and it applies to watcher removals on update as well as
  additions. Leave it off unless you need it.

## Building

Requires JDK 21. With the [Atlassian Plugin SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/):

```bash
atlas-mvn clean package
```

or with plain Maven 3.9+ (the Atlassian repositories are declared in the pom):

```bash
mvn clean package
```

The jar lands in `target/jira-watcher-field-<version>.jar`. Unit tests run as part of the
build; `mvn test` runs them alone.

## History

Originally written by Ray Barham (2008-2012), later maintained at
[sxbehnke/jira-watcher-field-plugin](https://bitbucket.org/sxbehnke/jira-watcher-field-plugin/src/master/).
This fork updated it for Jira 10 (2.6.0 and 2.10.x) and Jira 11 (3.x). See
[CHANGELOG.md](CHANGELOG.md).

## License

BSD 3-Clause — see [LICENSE.TXT](LICENSE.TXT).
