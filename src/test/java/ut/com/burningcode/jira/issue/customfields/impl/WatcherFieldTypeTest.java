package ut.com.burningcode.jira.issue.customfields.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.rest.json.UserBeanFactory;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserFilterManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.burningcode.jira.issue.customfields.impl.WatcherFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the pure logic in WatcherFieldType: value equality, changelog
 * rendering and the permission filtering applied when adding watchers.
 *
 * The logged-in user is always non-null in these tests so the static
 * WatcherFieldSettings property set (an OFBiz-backed singleton that needs a
 * running Jira) is never touched.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WatcherFieldTypeTest {

    @Mock private CustomFieldValuePersister customFieldValuePersister;
    @Mock private GenericConfigManager genericConfigManager;
    @Mock private MultiUserConverter multiUserConverter;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private JiraAuthenticationContext authenticationContext;
    @Mock private UserSearchService searchService;
    @Mock private FieldVisibilityManager fieldVisibilityManager;
    @Mock private JiraBaseUrls jiraBaseUrls;
    @Mock private PermissionManager permissionManager;
    @Mock private WatcherManager watcherManager;
    @Mock private UserBeanFactory userBeanFactory;
    @Mock private GroupManager groupManager;
    @Mock private ProjectRoleManager projectRoleManager;
    @Mock private SoyTemplateRendererProvider soyTemplateRendererProvider;
    @Mock private UserFilterManager userFilterManager;
    @Mock private FieldConfigSchemeManager fieldConfigSchemeManager;
    @Mock private ProjectManager projectManager;
    @Mock private FeatureManager featureManager;
    @Mock private UserManager userManager;
    @Mock private GlobalPermissionManager globalPermissionManager;

    @Mock private CustomField customField;
    @Mock private Issue issue;
    @Mock private Project project;
    @Mock private ApplicationUser actor;
    @Mock private ApplicationUser alice;
    @Mock private ApplicationUser bob;

    private WatcherFieldType fieldType;

    @BeforeEach
    void setUp() {
        fieldType = new WatcherFieldType(customFieldValuePersister, genericConfigManager,
                multiUserConverter, applicationProperties, authenticationContext, searchService,
                fieldVisibilityManager, jiraBaseUrls, permissionManager, watcherManager,
                userBeanFactory, groupManager, projectRoleManager, soyTemplateRendererProvider,
                userFilterManager, fieldConfigSchemeManager, projectManager, featureManager,
                userManager, globalPermissionManager);

        when(actor.getName()).thenReturn("actor");
        when(alice.getName()).thenReturn("alice");
        when(bob.getName()).thenReturn("bob");
    }

    /** Grants the acting user everything needed for addWatchers to run. */
    private void permitEditing() {
        when(issue.isCreated()).thenReturn(true);
        when(issue.isEditable()).thenReturn(true);
        when(issue.getProjectObject()).thenReturn(project);
        when(issue.getKey()).thenReturn("TEST-1");
        when(watcherManager.isWatchingEnabled()).thenReturn(true);
        when(authenticationContext.getLoggedInUser()).thenReturn(actor);
        // Only the issue-level overload is stubbed: if the implementation regressed
        // to the project-level check it would get Mockito's default false and the
        // positive-path tests would fail.
        when(permissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, issue, actor))
                .thenReturn(true);
    }

    // ---- valuesEqual ----

    @Test
    void valuesEqualIsOrderInsensitive() {
        assertTrue(fieldType.valuesEqual(Arrays.asList(alice, bob), Arrays.asList(bob, alice)));
    }

    @Test
    void valuesEqualDoesNotMutateUnmodifiableInputs() {
        // List.of() is unmodifiable; sorting the inputs in place would throw
        assertTrue(fieldType.valuesEqual(List.of(alice, bob), List.of(bob, alice)));
    }

    @Test
    void valuesEqualTreatsNullAsEmpty() {
        assertTrue(fieldType.valuesEqual(null, Collections.emptyList()));
        assertTrue(fieldType.valuesEqual(Collections.emptyList(), null));
        assertTrue(fieldType.valuesEqual(null, null));
    }

    @Test
    void valuesEqualDetectsDifferentUsers() {
        assertFalse(fieldType.valuesEqual(List.of(alice), List.of(bob)));
        assertFalse(fieldType.valuesEqual(List.of(alice, bob), List.of(alice)));
    }

    // ---- getChangelogValue ----

    @Test
    void changelogValueIsNullForSystemClear() {
        // Null value = Jira clearing the field on move out of context; a null
        // changelog value suppresses the change item, so history stays truthful
        // about the untouched watcher list.
        assertNull(fieldType.getChangelogValue(customField, null));
    }

    @Test
    void changelogValueIsNoneForGenuineEmptying() {
        assertEquals("None", fieldType.getChangelogValue(customField, Collections.emptyList()));
    }

    @Test
    void changelogValueUsesDisplayNames() {
        when(alice.getDisplayName()).thenReturn("Alice A");
        when(bob.getDisplayName()).thenReturn("Bob B");

        assertEquals("Alice A, Bob B", fieldType.getChangelogValue(customField, List.of(alice, bob)));
    }

    @Test
    void changelogValueSkipsNullUsers() {
        // JWFP-28
        when(alice.getDisplayName()).thenReturn("Alice A");

        assertEquals("Alice A", fieldType.getChangelogValue(customField, Arrays.asList(null, alice)));
    }

    @Test
    void changelogValueFallsBackToUsernameWhenDisplayNameMissing() {
        // JWFP-25
        when(alice.getDisplayName()).thenReturn(null);

        assertEquals("alice", fieldType.getChangelogValue(customField, List.of(alice)));
    }

    // ---- createValue -> addWatchers permission filtering ----

    @Test
    void createValueAddsPermittedWatcher() {
        permitEditing();
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, alice))
                .thenReturn(true);
        when(watcherManager.isWatching(alice, issue)).thenReturn(false);

        fieldType.createValue(customField, issue, List.of(alice));

        verify(watcherManager).startWatching(alice, issue);
    }

    @Test
    void createValueSkipsWatcherWithoutBrowsePermission() {
        permitEditing();
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, alice))
                .thenReturn(false);

        fieldType.createValue(customField, issue, List.of(alice));

        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
    }

    @Test
    void createValueSkipsUserAlreadyWatching() {
        permitEditing();
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, alice))
                .thenReturn(true);
        when(watcherManager.isWatching(alice, issue)).thenReturn(true);

        fieldType.createValue(customField, issue, List.of(alice));

        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
    }

    @Test
    void createValueDoesNothingWhenActorLacksManageWatchers() {
        permitEditing();
        when(permissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, issue, actor))
                .thenReturn(false);

        fieldType.createValue(customField, issue, List.of(alice));

        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
    }

    @Test
    void createValueIgnoresNullAndEmptyValues() {
        fieldType.createValue(customField, issue, null);
        fieldType.createValue(customField, issue, Collections.emptyList());

        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
    }

    // ---- updateValue ----

    @Test
    void updateValueWithNullIsSystemClearAndDoesNothing() {
        // Jira passes null when the field is removed from an issue (e.g. moving it
        // out of the field's context); the real watcher list must survive that.
        fieldType.updateValue(customField, issue, null);

        verify(watcherManager, never()).stopWatching(any(ApplicationUser.class), any(Issue.class));
        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
        verify(watcherManager, never()).getWatchers(any(Issue.class), any(Locale.class));
    }

    @Test
    void updateValueRemovesWatchersMissingFromSubmission() {
        permitEditing();
        when(authenticationContext.getLocale()).thenReturn(Locale.ENGLISH);
        when(watcherManager.getWatchers(issue, Locale.ENGLISH))
                .thenReturn(new ArrayList<>(List.of(alice, bob)));
        when(watcherManager.isWatching(alice, issue)).thenReturn(true);
        when(watcherManager.isWatching(bob, issue)).thenReturn(true);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, alice))
                .thenReturn(true);

        fieldType.updateValue(customField, issue, List.of(alice));

        verify(watcherManager).stopWatching(bob, issue);
        verify(watcherManager, never()).stopWatching(alice, issue);
        verify(watcherManager, never()).startWatching(any(ApplicationUser.class), any(Issue.class));
    }

    @Test
    void updateValueWithEmptyCollectionRemovesAllWatchers() {
        permitEditing();
        when(authenticationContext.getLocale()).thenReturn(Locale.ENGLISH);
        when(watcherManager.getWatchers(issue, Locale.ENGLISH))
                .thenReturn(new ArrayList<>(List.of(alice)));
        when(watcherManager.isWatching(alice, issue)).thenReturn(true);

        fieldType.updateValue(customField, issue, Collections.emptyList());

        verify(watcherManager).stopWatching(alice, issue);
    }

    // ---- validateFromParams ----

    @Test
    void validateGrandfathersNamesAlreadyWatching() {
        // The read-only branch round-trips current watchers on every submit; a
        // current watcher who is no longer a valid pickable user (deactivated,
        // deleted) must not fail validation and block the whole edit.
        CustomFieldParams params = mock(CustomFieldParams.class);
        when(params.getValuesForNullKey()).thenReturn(List.of("alice"));
        when(params.getFirstValueForKey(CustomFieldUtils.getParamKeyIssueId())).thenReturn("10001");
        when(multiUserConverter.extractUserStringsFromString("alice")).thenReturn(List.of("alice"));

        IssueManager issueManager = mock(IssueManager.class);
        MutableIssue watchedIssue = mock(MutableIssue.class);
        installComponent(issueManager);
        when(issueManager.getIssueObject(10001L)).thenReturn(watchedIssue);
        when(authenticationContext.getLocale()).thenReturn(Locale.ENGLISH);
        when(watcherManager.getWatchers(watchedIssue, Locale.ENGLISH))
                .thenReturn(new ArrayList<>(List.of(alice)));

        ErrorCollection errors = new SimpleErrorCollection();
        fieldType.validateFromParams(params, errors, mock(FieldConfig.class));

        assertFalse(errors.hasAnyErrors());
        // Validation of already-watching names was skipped entirely: the user
        // search that the inherited validation performs never ran.
        verifyNoInteractions(searchService);
    }

    /** Routes ComponentAccessor lookups (used for IssueManager) to the given mock. */
    private void installComponent(IssueManager issueManager) {
        ComponentAccessor.initialiseWorker(new ComponentAccessor.Worker() {
            @Override
            public <T> Optional<T> getComponentSafely(Class<T> type) {
                return Optional.ofNullable(getComponent(type));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getComponent(Class<T> type) {
                return type == IssueManager.class ? (T) issueManager : null;
            }

            @Override
            public <T> T getComponentOfType(Class<T> type) {
                return getComponent(type);
            }

            @Override
            public <T> T getOSGiComponentInstanceOfType(Class<T> type) {
                return getComponent(type);
            }

            @Override
            public <T> Optional<T> getOSGiComponentInstanceOfTypeSafely(Class<T> type) {
                return getComponentSafely(type);
            }
        });
    }

    // ---- getValueFromCustomFieldParams ----

    @Test
    void emptySubmissionBecomesEmptyCollectionNotNull() {
        // The inherited implementation returns null for an empty submission, which
        // updateValue would treat as a system clear; the override must map it to
        // an empty collection so a deliberately emptied picker still clears.
        CustomFieldParams params = mock(CustomFieldParams.class);
        when(params.getValuesForNullKey()).thenReturn(null);

        Collection<ApplicationUser> value = fieldType.getValueFromCustomFieldParams(params);

        assertNotNull(value);
        assertTrue(value.isEmpty());
    }
}
