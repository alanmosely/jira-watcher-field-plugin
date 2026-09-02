# Cutting 3.0.0 final

3.0.0-rc3 (tagged `jira-watcher-field-3.0.0-rc3`, released on GitHub, merged to `master`)
is the candidate for final. It fixed the six verified defects from the post-rc2 code
review and was smoke-tested end to end on Jira DC 11.3.10. This file is the checklist
for promoting it.

## 1. Soak rc3 first

Install the rc3 jar on a real or staging Jira DC 11.x instance (**Administration →
Manage apps → Upload app**) and exercise, over a reasonable soak period:

- Field on create and edit screens: add and remove watchers as a user **with** Manage
  Watchers, and confirm a user **without** it sees the read-only list and can still save
  edits (including on an issue that has a **deactivated** watcher — rc3 grandfathers
  those in validation).
- A project with **issue security levels**: users must not be able to manage watchers on
  (or be added to) issues their security level hides (the rc3 issue-level permission fix).
- **Move an issue** to a project/issue type where the field is not configured: the
  watcher list must survive and no "Watchers → None" history entry may appear.
- JQL through the field searcher (`"<field name>" = user`).
- The settings screen toggle (`ignorePermissions`) and, if used, a null-user flow such as
  an incoming-mail handler creating issues with default watchers.
- Watch `atlassian-jira.log` for the plugin's WARN lines (they flag skipped watcher
  mutations and settings read failures).

## 2. Decide what rides along

Nothing left in [TODO.md](TODO.md) blocks final — they are minors. If any are folded in
(the cheapest with real value is including `isWatchingEnabled()` in the
`getVelocityParameters` permission decision), they need the same build/test/review cycle
before tagging.

## 3. Cut

On branch `jira-11-compat`, clean tree, up to date with origin:

1. Bump `<version>` in [pom.xml](pom.xml) from `3.0.0-rc3` to `3.0.0`.
2. Add a `## [3.0.0] - <date>` section to [CHANGELOG.md](CHANGELOG.md). If nothing
   changed since rc3, roll up: copy the rc3 section's content under 3.0.0 with a note
   "identical to 3.0.0-rc3", or write "No changes since 3.0.0-rc3" and let the rc
   sections carry the detail.
3. Build and test — either works now that the Atlassian repositories are in the pom:
   - SDK: set `JAVA_HOME` to a JDK 21 (this machine: `C:\dev\jdk-21`), then
     `atlas-mvn clean package`
   - Plain Maven 3.9+: `mvn clean package`

   Expect all unit tests green and `target/jira-watcher-field-3.0.0.jar` produced.
   If dependencies changed at all, re-check the built `MANIFEST.MF`: `Import-Package`
   must not contain `javax.inject`, `javax.annotation` or `org.apache.log4j`.
4. Optional local smoke (`atlas-run`): note that QuickReload does **not** reliably
   register the plugin even though the jar lands in `installed-plugins` — install it
   explicitly (UI upload as admin/admin, or UPM REST: grab the `upm-token` response
   header from `GET /rest/plugins/1.0/?os_authType=basic`, then multipart-POST the jar
   to `/rest/plugins/1.0/?token=<token>`). Confirm the plugin and all five modules show
   enabled.
5. Commit (`Release 3.0.0`), tag annotated `jira-watcher-field-3.0.0`, push branch and
   tag. Wait for the GitHub Actions "Build" run to go green.
6. Create the GitHub release for the tag: body from the CHANGELOG 3.0.0 section, attach
   `target/jira-watcher-field-3.0.0.jar`, **not** marked as a pre-release (the GitHub
   release is the distribution channel — the plugin is not on Marketplace).
7. Merge `jira-11-compat` → `master` (`--no-ff`) and push.

## 4. After

- Any commit after the tag bumps the pom version before the next build leaves the
  machine (two different artifacts must never share a version string — this bit rc2).
- Update [TODO.md](TODO.md) if anything shipped, and keep the README compatibility
  table current if a new Jira LTS line is targeted.
