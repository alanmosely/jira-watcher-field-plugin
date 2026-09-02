# TODO

Open findings from the post-rc2 code review (2026-09-02). Items marked **verified** were
confirmed against Jira 11.3.10 bytecode. File references are to the current source.
The six verified bugs originally listed here were fixed in 3.0.0-rc3 - see CHANGELOG.md.

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
- [ ] Bulk edit renders this field from the first selected issue only, so with the
  round-trip fix a no-permission render that is deliberately ticked in bulk edit submits
  the first issue's watcher list to every selected issue (the field's replace semantics,
  but surprising). Consider suppressing the field in bulk edit
  (`availableForBulkEdit`) or documenting it.
- [ ] Consider replacing the static `WatcherFieldSettings.getPropertySet()` accessor with
  a small injected component built on `JiraPropertySetFactory.buildCachingPropertySet`
  (drops the static state and class-level synchronization; `"ofbiz-cached"` already gives
  the same caching behavior).
