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
2. Users who hold **Manage Watchers** can set watchers. On existing issues, the field
   shows the list to users who can browse the issue and have either **View Voters and
   Watchers** or **Manage Watchers**. Users with view permission can read the list.
3. Watchers added through the field must have the **Browse Projects** permission; anyone else
   in the submitted list is skipped with a logged warning, though the issue history may still
   record them as added.

On update the field applies *replace* semantics: watchers missing from the submitted list are
removed, new entries are added.

Use Jira's built-in `watcher` JQL field for searches.

## Upgrading to 3.0.0-rc4

This is a release candidate for Jira Data Center 11.x (tested on 11.3.10), with
permission fixes for watcher identities. Validate the upgrade in a test instance
before rolling it out.

1. Upload `jira-watcher-field-3.0.0-rc4.jar` through **Administration → Manage apps →
   Upload app**.
2. In **Administration → Issues → Custom fields**, edit each existing **Watcher
   Field** and set its search template to **None**. The plugin no longer supplies a
   custom-field searcher or indexer.
3. Replace saved filters that query the custom field with Jira's built-in `watcher`
   JQL clause, which applies Jira's watcher permissions. For example, replace a
   custom-field equality query with `watcher = username` for the same user.
4. Run a **full reindex** to remove the old custom-field index values. Schedule this
   using the normal reindex procedure for your Jira installation.
5. Check the field with an issue viewer who has neither watcher permission, a user
   with **View Voters and Watchers**, and a user with **Manage Watchers**. Only the
   latter two should see identities; unrelated edits by a read-only user must
   preserve the watcher list.

New custom-field history records watcher counts instead of names. This upgrade
does not rewrite existing Jira history: names recorded by older plugin versions
may remain visible there. The watcher list itself is preserved by the upgrade.

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
