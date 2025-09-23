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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import webwork.action.ActionContext;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.UserComparator;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.burningcode.jira.plugin.WatcherFieldSettings;
import com.opensymphony.module.propertyset.PropertySet;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class is a custom field type that allows users with
 * "Manage Watcher List" permissions to modify users when creating/updating issues.
 *
 * @author Ray Barham
 * @see com.atlassian.jira.issue.customfields.impl.MultiUserCFType
 */
@Named
public class WatcherFieldType extends MultiUserCFType {

    private static final Logger log = Logger.getLogger(WatcherFieldType.class);

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
        if (userList != null && isIssueEditable(issue)) {
            for (Iterator<?> i = userList.iterator(); i.hasNext(); ) {
                Object next = i.next();
                ApplicationUser watcher = null;

                if (next instanceof ApplicationUser) {
                    watcher = (ApplicationUser) next;
                } else if (next instanceof String) {
                    watcher = userManager.getUserByNameEvenWhenUnknown((String) next);
                }

                // JWFP-22: Added check for watcher's permission to browse project
                if (watcher != null && _PermissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue.getProjectObject(), watcher) && !_WatcherManager.isWatching(watcher, issue)) {
                    _WatcherManager.startWatching(watcher, issue);
                }
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
    public void createValue(CustomField customField, Issue issue, @Nonnull Collection<ApplicationUser> value) {
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
     * Checks if a user the authenticated user has the "Manage Watcher List" permission.
     *
     * @param issue The issue the user is trying to add watchers to.
     * @return True if has permissions, false otherwise.
     */
    public boolean isUserPermitted(Issue issue) {
        PropertySet propertySet = WatcherFieldSettings.getPropertySet();
        ApplicationUser user = _AuthenticationContext.getLoggedInUser();

        // Allow JIRA service to set the watcher field, if enabled to do so.
        if (propertySet.exists("ignorePermissions") && propertySet.getBoolean("ignorePermissions") && user == null)
            return true;

        return _PermissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, issue.getProjectObject(), user);
    }

    /**
     * Overridden, returns the value reported in the changelog
     *
     * @return The full names of watching users in a comma separated list.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#getChangelogValue(CustomField, Collection)
     */
    public String getChangelogValue(CustomField field, Collection<ApplicationUser> value) {
        List<ApplicationUser> watcherList = (List<ApplicationUser>) value;

        if (watcherList == null || watcherList.isEmpty()) return "None";

        String output = "";
        for (Iterator<ApplicationUser> i = watcherList.iterator(); i.hasNext(); ) {
            ApplicationUser user = (ApplicationUser) i.next();

            // Fix for JWFP-28
            if (user == null) continue;

            String displayName = user.getDisplayName();

            // Add fix for issue JWFP-25
            if (displayName == null) displayName = user.getName();

            output += displayName + (i.hasNext() ? ", " : "");
        }

        return output;
    }

    /**
     * Overridden, returns the a list of watchers
     * on the passed issue
     *
     * @return List of User objects that are watchers on the passed issue.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#getValueFromIssue(CustomField, Issue)
     */
    public Collection<ApplicationUser> getValueFromIssue(@Nonnull CustomField field, Issue issue) {
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
    @Nonnull
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("hasPermission", new Boolean(false));

        if (issue == null || issue.getProjectObject() == null) {
            if (isJiraAdmin(_AuthenticationContext.getLoggedInUser())) {
                params.put("hasPermission", new Boolean(true));
            }
        } else if (isUserPermitted(issue)) {
            params.put("hasPermission", new Boolean(true));
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
        if (userList != null && isIssueEditable(issue)) {
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
    public void updateValue(CustomField customField, Issue issue, Collection<ApplicationUser> value) {
        List<ApplicationUser> newWatchers = (List<ApplicationUser>) value;
        List<ApplicationUser> currWatchers = getWatchers(issue);

        if (!currWatchers.isEmpty()) {
            if (newWatchers != null) {
                currWatchers.removeAll(newWatchers);
            }
            removeWatchers(issue, currWatchers);
        }

        addWatchers(issue, newWatchers);
    }

    @Override
    public void validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config) {
        String[] pid = (String[]) ActionContext.getParameters().get("pid");
        String[] id = (String[]) ActionContext.getParameters().get("id");

        Project project = null;
        if (pid != null) project = ComponentAccessor.getProjectManager().getProjectObj(Long.valueOf(pid[0]));

        Issue issue = null;
        if (id != null) issue = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(id[0]));

        ArrayList<String> invalidUsers = new ArrayList<String>();
        Collection<ApplicationUser> watchers = getValueFromCustomFieldParams(relevantParams);
        if (watchers != null && watchers.size() > 0) {
            for (ApplicationUser user : watchers) {
                if ((project != null && !_PermissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)) || (issue != null && !_PermissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user))) {
                    invalidUsers.add(user.getName());
                }
            }
        }

        // Validation only runs on Edit screen, not on View screen, so this check is inconsistent
        
        // if (invalidUsers.size() > 0)
        //     errorCollectionToAddTo.addError(config.getFieldId(), "Users do not have permission to browse issue: " + StringUtils.join(invalidUsers, ", "), ErrorCollection.Reason.FORBIDDEN);

        super.validateFromParams(relevantParams, errorCollectionToAddTo, config);
    }

    /**
     * Overridden, returns true if the current watcher list is equal to the new ones provided.
     *
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#valuesEqual(Collection, Collection)
     */
    @Override
    public boolean valuesEqual(Collection<ApplicationUser> v1, Collection<ApplicationUser> v2) {
        ArrayList<ApplicationUser> watcherList1 = (v1 != null ? (ArrayList<ApplicationUser>) v1 : new ArrayList<>());
        ArrayList<ApplicationUser> watcherList2 = (v2 != null ? (ArrayList<ApplicationUser>) v2 : new ArrayList<>());
        Collections.sort(watcherList1, new UserComparator());
        Collections.sort(watcherList2, new UserComparator());

        if (watcherList1.equals(watcherList2)) {
            return true;
        }

        return false;
    }
}
