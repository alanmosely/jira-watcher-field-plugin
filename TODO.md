# TODO

Open findings from the post-rc2 code review (2026-09-02). Items marked **verified** were
confirmed against Jira 11.3.10 bytecode. File references are to the current source.

## Bugs to fix before 3.0.0 final

- [ ] **Check Manage Watchers at issue level, not project level** *(verified)* —
  `WatcherFieldType.isUserPermitted` (`WatcherFieldType.java:203`) uses
  `hasPermission(MANAGE_WATCHERS, issue.getProjectObject(), user)`. Jira's own
  `DefaultWatcherService` uses the `Issue` overload, which also enforces issue security
  levels and evaluates issue-scoped grants (Current Assignee / Reporter / user-CF security
  types pass for *everyone* on a project-level check). Fix: `hasPermission(MANAGE_WATCHERS,
  issue, user)`. Safe on create screens: a null issue id falls back to a project check with
  create-time semantics.

- [ ] **Check the added watcher's Browse at issue level** *(verified)* —
  `WatcherFieldType.addWatchers` (`WatcherFieldType.java:138`) checks
  `BROWSE_PROJECTS` against the project, so users can be added as watchers on
  security-levelled issues they cannot see — a state core Jira refuses to create
  (`watcher.error.user.cant.see.issue`). Fix: `hasPermission(BROWSE_PROJECTS, issue, watcher)`;
  `isIssueEditable` already guarantees `issue.isCreated()` here.

- [ ] **No-permission edit branch submits no value → false changelog + bulk-edit data loss**
  *(verified)* — the `#else` branch of
  `templates/plugins/fields/edit/edit-watcherfield.vm` renders no input named
  `$customField.id`. The full Edit page disables retain-existing-values, so every edit by a
  user without Manage Watchers writes a false `Watchers: A, B → None` history entry; in bulk
  edit (form rendered from the first selected issue only) ticking the field genuinely clears
  watchers on issues where the user *does* have permission. Fix: round-trip the current value
  as hidden inputs in the `#else` branch. Note `$value` in the edit context is a
  `Collection<String>` of usernames — use `value="$singleValue"`, not `$singleValue.name`.

- [ ] **Issue move out of the field's context strips the whole watcher list** *(verified)* —
  single/bulk move to a project/issue-type without the field calls
  `removeValueFromIssueObject` → `updateValue(field, issue, null)`
  (`WatcherFieldType.java:335`), which removes every current watcher (or writes a false
  `→ None` changelog when the gate blocks). Fix: treat `value == null` in `updateValue` as a
  system-initiated clear and no-op; override `getValueFromCustomFieldParams` to return an
  empty collection instead of null so a genuinely emptied picker still clears.

- [ ] **No-permission watcher list renders escaped HTML** *(verified)* —
  `edit-watcherfield.vm:43`: `$userformat.formatUser($singleValue, ...)` output is
  entity-escaped (template lacks `#disable_html_escaping()`, `UserFormatManager` is not
  `@HtmlSafe`), so users see literal `<a href=...>` text; the String `formatUser` variant is
  also the deprecated key-based one being passed usernames. Fix: render plain `$singleValue`,
  or add the directive and use `formatUsername(...)` (then re-check the whole template, the
  directive is template-wide).

- [ ] **Replace the raw OFBiz property set with Jira's caching one** *(verified)* —
  `WatcherFieldSettings.getPropertySet` (`WatcherFieldSettings.java:109`) does 3 uncached SQL
  queries per anonymous/service permission check (an anonymous navigator page with the
  watcher column over 50 issues ≈ 150 queries). Fix: inject `JiraPropertySetFactory` and use
  `buildCachingPropertySet("WatcherFieldSettings", 1L, true)` (cluster-aware), or minimally
  switch `"ofbiz"` → `"ofbiz-cached"`. While there:
  - [ ] Remove default-seeding from the read path (first-touch race on a multi-node cluster
    can create duplicate `OSPropertyEntry` rows; the reader already tolerates absence).
  - [ ] Narrow `catch (Exception)` (`WatcherFieldSettings.java:122`): a transient DB error
    currently overwrites a configured `ignorePermissions=true` with `false`. Catch only
    `InvalidPropertyTypeException` for the repair-write and log at WARN, not DEBUG.

## Should fix

- [ ] **Field renders editable while watching is disabled** *(verified)* —
  `getVelocityParameters` (`WatcherFieldType.java:255`) ignores
  `_WatcherManager.isWatchingEnabled()`, so with watching off the picker is editable, the
  update no-ops, and history records phantom changes. Include `isWatchingEnabled()` in the
  `hasPermission` velocity decision.
- [ ] **Changelog can misstate partial applications** *(verified)* — the change item is built
  from the submitted collection after `updateValue` returns and the SPI offers no hook, so
  BROWSE-skipped users still appear as "added" in history. Mitigate by rejecting
  non-addable users in validation; the residual gap is documented in the README.
- [ ] **Scope `ignorePermissions` to additions** — the null-user bypass
  (`WatcherFieldType.java:197`) also authorizes removals via `updateValue`, wider than the
  documented add-default-watchers purpose (README now warns about this). Consider an
  operation flag so removals still require a real permitted user. Related known item: the
  bypass also matches anonymous HTTP requests — could be narrowed with
  `ExecutingHttpRequest.get() == null`.
- [ ] **Stop minting stub users** — `getUserByNameEvenWhenUnknown`
  (`WatcherFieldType.java:132`, `:308`) returns a stub for unknown names, which
  `startWatching` persists as a ghost watcher row (a typo in a scripted update becomes a
  phantom watcher). Use `getUserByName` and skip-with-warn on null. Also normalize incoming
  `Collection<String>` values to `ApplicationUser` once at the top of `updateValue` —
  otherwise `currWatchers.removeAll(value)` never matches and every scripted update does a
  full remove/re-add cycle.
- [ ] **Return null from `getValueFromIssue` when nobody watches**
  (`WatcherFieldType.java:245`) — the contract distinguishes null (no value) from empty;
  returning an empty list makes `hasValue()` true for every in-scope issue (move flows list
  the field among "values to be removed" even when unwatched).
- [ ] **Fix the settings warning alert encoding** (`templates/settings/edit-watcherfield.vm:24`)
  — i18n text inside `onClick="alert('…')"` with no JS-string encoding breaks on any
  apostrophe (e.g. French translations) and is a wrong-encoding XSS-shaped pattern. Render
  the warning statically (`aui-message`) or via an HTML-escaped `data-` attribute.
- [ ] **Localize the changelog "None"** (`WatcherFieldType.java:214`) — use
  `getI18nBean().getText("common.words.none")`.
- [ ] **Remove HTML from i18n values** (`WatcherFieldType.properties:15`) — Jira 11
  HTML-escapes `$i18n.getText(...)` here, so `<br/><b>Warning:</b>` shows as literal text.
  Move structure into the templates.
- [ ] **Document/mitigate the concurrent-watch lost update** — `updateValue` is
  read-modify-write over live shared state: someone who clicks Watch while an editor has the
  form open gets silently removed on save, attributed to the editor. Consider an additive
  mode (never remove) as a field config option — the safe default for the mail-handler use
  case.

## Nice to have

- [ ] Accessibility of the settings form (`templates/settings/edit-watcherfield.vm`): radios
  have no `id`/`<label>`/fieldset; caption is a bare `<td>`.
- [ ] Replace hardcoded English tooltips `title="Press Ctrl+s to submit form"` and
  ``Cancel (Ctrl + `)`` with Jira's `common.forms.submit.tooltip`/`.accesskey` keys (the Ctrl
  claim is wrong in every mainstream browser); rename the Cancel button's
  `name="WatcherFieldSettings.jspa"`.
- [ ] Drop hardcoded colors in the settings templates (`bgcolor="#f0f0f0"`, `#ffffff`,
  `color:#00bb00/#bb0000`): they ignore the dark theme and the green fails WCAG contrast on
  white. The legacy `jiraform`/`tableBorder` classes still ship theme-tokenized CSS in Jira
  11, so a full AUI rewrite is optional future-proofing, not a breakage fix.
- [ ] `getWatchers` (`WatcherFieldType.java:277`): copy defensively instead of sorting
  `WatcherManager`'s list in place, and consider keeping Jira's locale-aware order instead
  of re-sorting by username.
- [ ] Log user identifiers that cannot carry attacker text (keys, or strip CR/LF) in the
  permission-audit warnings (`WatcherFieldType.java:121/139/297`); moot if the String branch
  is removed.
- [ ] Comment the double role of `@Named` on the three classes (spring-scanner needs it to
  index `@ComponentImport`s, while Jira instantiates actions/modules through its own
  container) so nobody "cleans it up" and breaks wiring.
- [ ] Unify the `ignorePermissions` boolean idioms across the settings templates
  (`.equals(true)` vs `== true`) with `#if($ignorePermissions)`/`#else`, which also
  guarantees one radio is always checked.
- [ ] Fix "JIRA" → "Jira", "This settings allows" and the permission name ("Manage
  Watchers" is the Jira DC 11.3 display name) in `WatcherFieldType.properties`; update the
  README's quoted setting label at the same time.
- [ ] `templates/settings/watcherfield.vm:29`: change the edit link to
  `$baseurl/secure/EditWatcherFieldSettings!default.jspa` (drop the meaningless legacy
  `/secure/project/` segment).
- [ ] Publish GitHub Releases for the 3.0.0 tags with changelog entries and the built jar;
  delete the stray `jira-watcher-field-2.6.0-SNAPSHOT` tag.
