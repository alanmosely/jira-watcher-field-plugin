/* Copyright (c) 2008, 2009, Ray Barham
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Ray Barham ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Ray Barham BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.burningcode.jira.issue.customfields.impl;

import java.util.*;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.rest.json.UserBeanFactory;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserFilterManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.UserComparator;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.burningcode.jira.plugin.WatcherFieldSettings;
import com.opensymphony.module.propertyset.PropertySet;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This class is a custom field type that allows users with
 * "Manage Watcher List" permissions to modify users when creating/updating issues.
 *
 * @author Ray Barham
 * @see com.atlassian.jira.issue.customfields.impl.MultiUserCFType
 */
@Named
public class WatcherFieldType extends MultiUserCFType {

    private static final Logger log = LoggerFactory.getLogger(WatcherFieldType.class);

    @ComponentImport
    private final JiraAuthenticationContext _AuthenticationContext;
    @ComponentImport
    private final PermissionManager _PermissionManager;
    @ComponentImport
    private final WatcherManager _WatcherManager;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final GlobalPermissionManager globalPermissionManager;

    /**
     * Overridden, calls super constructor.
     *
     * @see com.atlassian.jira.issue.customfields.impl.MultiUserCFType#MultiUserCFType
     */
    @Inject
    public WatcherFieldType(@ComponentImport CustomFieldValuePersister customFieldValuePersister, @ComponentImport GenericConfigManager genericConfigManager, @ComponentImport MultiUserConverter multiUserConverter, @ComponentImport ApplicationProperties applicationProperties, JiraAuthenticationContext authenticationContext, @ComponentImport UserSearchService searchService, @ComponentImport FieldVisibilityManager fieldVisibilityManager, @ComponentImport JiraBaseUrls jiraBaseUrls, PermissionManager permissionManager, WatcherManager watcherManager, @ComponentImport UserBeanFactory userBeanFactory, @ComponentImport GroupManager groupManager, @ComponentImport ProjectRoleManager projectRoleManager, @ComponentImport SoyTemplateRendererProvider soyTemplateRendererProvider, @ComponentImport UserFilterManager userFilterManager, @ComponentImport FieldConfigSchemeManager fieldConfigSchemeManager, @ComponentImport ProjectManager projectManager, @ComponentImport FeatureManager featureManager, UserManager userManager, GlobalPermissionManager globalPermissionManager) {
        super(customFieldValuePersister, genericConfigManager, multiUserConverter, applicationProperties, authenticationContext, searchService, fieldVisibilityManager, jiraBaseUrls, userBeanFactory, groupManager, projectRoleManager, soyTemplateRendererProvider, userFilterManager, fieldConfigSchemeManager, projectManager, featureManager);
        _AuthenticationContext = authenticationContext;
        _PermissionManager = permissionManager;
        _WatcherManager = watcherManager;
        this.userManager = userManager;
        this.globalPermissionManager = globalPermissionManager;
    }

    /**
     * Add a list of users as watchers on an issue.
     *
     * @param issue    The issue to add watchers to.
     * @param userList A list of User objects to add as watchers.
     */
    protected void addWatchers(Issue issue, Collection<?> userList) {
        if (userList == null || userList.isEmpty()) {
            return;
        }

        // Jira records a changelog entry for the field regardless of whether the
        // watchers were actually changed, so a skipped update must not be silent.
        if (!isIssueEditable(issue)) {
            log.warn("Skipping requested watcher additions on {} by {}: issue is not editable or the user lacks the Manage Watchers permission; the issue history may still record a watcher change", issue.getKey(), actingUserName());
            return;
        }

        for (Iterator<?> i = userList.iterator(); i.hasNext(); ) {
            Object next = i.next();
            ApplicationUser watcher = null;

            if (next instanceof ApplicationUser) {
                watcher = (ApplicationUser) next;
            } else if (next instanceof String) {
                watcher = userManager.getUserByNameEvenWhenUnknown((String) next);
            }

            // JWFP-22: Added check for watcher's permission to browse project
            if (watcher == null) continue;

            if (!_PermissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue.getProjectObject(), watcher)) {
                log.warn("Not adding watcher {} to {}: no browse permission on the project; the issue history may still record the addition", watcher.getName(), issue.getKey());
            } else if (!_WatcherManager.isWatching(watcher, issue)) {
                _WatcherManager.startWatching(watcher, issue);
            }
        }
    }


    /**
     * Overridden, adds a list of watchers to an issue.
     *
     * @param customField See AbstractMultiCFType.createValue.
     * @param issue       See AbstractMultiCFType.createValue.
     * @param value       List of User objects to add as watchers.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#createValue(CustomField, Issue, Collection)
     */
    @Override
    public void createValue(CustomField customField, Issue issue, Collection<ApplicationUser> value) {
        addWatchers(issue, value);
    }

    /**
     * Checks to see if the issue can be edited.  It checks to see if the issue has been created, if it is
     * editable, and if the authenticated user has permissions.
     *
     * @param issue The issue being edited.
     * @return True if able to edit, false otherwise.
     */
    protected boolean isIssueEditable(Issue issue) {
        if (issue.isCreated() && issue.isEditable() && _WatcherManager.isWatchingEnabled() && isUserPermitted(issue)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a user is a JIRA administrator.
     *
     * @param user The user the check
     * @return True if has permissions, false otherwise.
     */
    public boolean isJiraAdmin(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    /**
     * Checks if the authenticated user has the "Manage Watcher List" permission.
     *
     * @param issue The issue the user is trying to add watchers to.
     * @return True if has permissions, false otherwise.
     */
    public boolean isUserPermitted(Issue issue) {
        ApplicationUser user = _AuthenticationContext.getLoggedInUser();

        // Allow JIRA service to set the watcher field, if enabled to do so.
        // The user == null check comes first: the property reads are uncached
        // database queries and would otherwise run on every field render.
        if (user == null) {
            PropertySet propertySet = WatcherFieldSettings.getPropertySet();
            if (propertySet.exists("ignorePermissions") && propertySet.getBoolean("ignorePermissions"))
                return true;
        }

        return _PermissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, issue.getProjectObject(), user);
    }

    /**
     * Overridden, returns the value reported in the changelog
     *
     * @return The full names of watching users in a comma separated list.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#getChangelogValue(CustomField, Collection)
     */
    @Override
    public String getChangelogValue(CustomField field, Collection<ApplicationUser> value) {
        if (value == null || value.isEmpty()) return "None";

        StringJoiner output = new StringJoiner(", ");
        for (ApplicationUser user : value) {
            // Fix for JWFP-28
            if (user == null) continue;

            String displayName = user.getDisplayName();

            // Add fix for issue JWFP-25
            if (displayName == null) displayName = user.getName();

            output.add(displayName);
        }

        return output.toString();
    }

    /**
     * Overridden, returns the a list of watchers
     * on the passed issue
     *
     * @return List of User objects that are watchers on the passed issue.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#getValueFromIssue(CustomField, Issue)
     */
    @Override
    public Collection<ApplicationUser> getValueFromIssue(CustomField field, Issue issue) {
        if (!issue.isCreated()) {
            return super.getValueFromIssue(field, issue);
        }

        return getWatchers(issue);
    }

    /**
     * Overridden, adds the "hasPermissions" parameter to velocity
     * with true if the authenticated user has "Manage Watcher List" permissions, false otherwise.
     *
     * @see com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType#getVelocityParameters(Issue, CustomField, FieldLayoutItem)
     */
    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("hasPermission", Boolean.FALSE);

        if (issue == null || issue.getProjectObject() == null) {
            if (isJiraAdmin(_AuthenticationContext.getLoggedInUser())) {
                params.put("hasPermission", Boolean.TRUE);
            }
        } else if (isUserPermitted(issue)) {
            params.put("hasPermission", Boolean.TRUE);
        }

        return params;
    }

    /**
     * Get a list of of watchers on an issue.
     *
     * @param issue The issue to get watchers from.
     * @return A List of User objects that are watchers on the passed issue.
     */
    protected List<ApplicationUser> getWatchers(Issue issue) {
        List<ApplicationUser> currWatchers = _WatcherManager.getWatchers(issue, _AuthenticationContext.getLocale());
        Collections.sort(currWatchers, new UserComparator());

        return currWatchers;
    }

    /**
     * Remove a list of users as watchers on an issue.
     *
     * @param issue    The issue to add watchers to.
     * @param userList A list of User objects to remove from being watchers.
     */
    protected void removeWatchers(Issue issue, List<?> userList) {
        if (userList == null || userList.isEmpty()) {
            return;
        }

        // Jira records a changelog entry for the field regardless of whether the
        // watchers were actually changed, so a skipped update must not be silent.
        if (!isIssueEditable(issue)) {
            log.warn("Skipping requested watcher removals on {} by {}: issue is not editable or the user lacks the Manage Watchers permission; the issue history may still record a watcher change", issue.getKey(), actingUserName());
            return;
        }

        for (Iterator<?> i = userList.iterator(); i.hasNext(); ) {
            Object next = i.next();
            ApplicationUser user = null;

            if (next instanceof ApplicationUser) {
                user = (ApplicationUser) next;
            } else if (next instanceof String) {
                user = userManager.getUserByNameEvenWhenUnknown((String) next);
            }

            if (user != null && _WatcherManager.isWatching(user, issue)) {
                _WatcherManager.stopWatching(user, issue);
            }
        }
    }

    /**
     * Name of the acting user for log messages.
     */
    private String actingUserName() {
        ApplicationUser user = _AuthenticationContext.getLoggedInUser();
        return user != null ? user.getName() : "anonymous/service";
    }

    /**
     * Overridden, updates an issue with a list of watchers.
     *
     * @param customField See AbstractMultiCFType.createValue.
     * @param issue       See AbstractMultiCFType.createValue.
     * @param value       List of User objects to update as watchers.  Note, any user not in this list that was previously
     *                    a watcher will be removed.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#updateValue(CustomField, Issue, Collection)
     */
    @Override
    public void updateValue(CustomField customField, Issue issue, Collection<ApplicationUser> value) {
        List<ApplicationUser> currWatchers = getWatchers(issue);

        if (!currWatchers.isEmpty()) {
            if (value != null) {
                currWatchers.removeAll(value);
            }
            removeWatchers(issue, currWatchers);
        }

        addWatchers(issue, value);
    }

    /**
     * Overridden, returns true if the current watcher list is equal to the new ones provided.
     *
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#valuesEqual(Collection, Collection)
     */
    @Override
    public boolean valuesEqual(Collection<ApplicationUser> v1, Collection<ApplicationUser> v2) {
        // Compare order-insensitively on defensive copies; the passed-in collections
        // may be unmodifiable and must not be mutated by sorting.
        List<ApplicationUser> watcherList1 = (v1 != null ? new ArrayList<>(v1) : new ArrayList<>());
        List<ApplicationUser> watcherList2 = (v2 != null ? new ArrayList<>(v2) : new ArrayList<>());
        watcherList1.sort(new UserComparator());
        watcherList2.sort(new UserComparator());

        return watcherList1.equals(watcherList2);
    }
}
