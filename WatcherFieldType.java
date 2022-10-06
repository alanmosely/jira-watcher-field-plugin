package com.burningcode.jira.issue.customfields.impl;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.ApplicationUserBestNameComparator;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.json.UserBeanFactory;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
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
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.burningcode.jira.plugin.WatcherFieldContext;
import com.burningcode.jira.plugin.WatcherFieldSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.ActionContext;

@Scanned
public class WatcherFieldType
extends MultiUserCFType {
    private static final Logger log = LoggerFactory.getLogger(WatcherFieldType.class);


    private final GlobalPermissionManager globalPermissionManager;


    private final IssueSecurityLevelManager issueSecurityLevelManager;

    private final JiraAuthenticationContext authenticationContext;

    private final PermissionManager permissionManager;

    private final WatcherManager watcherManager;
    private final UserFilterManager userFilterManager;


    public WatcherFieldType(@ComponentImport CustomFieldValuePersister customFieldValuePersister, @ComponentImport GenericConfigManager genericConfigManager, @ComponentImport MultiUserConverter multiUserConverter, @ComponentImport ApplicationProperties applicationProperties, @ComponentImport JiraAuthenticationContext authenticationContext, @ComponentImport UserSearchService searchService, @ComponentImport FieldVisibilityManager fieldVisibilityManager, @ComponentImport JiraBaseUrls jiraBaseUrls, @ComponentImport UserBeanFactory userBeanFactory, @ComponentImport GroupManager groupManager, @ComponentImport ProjectRoleManager projectRoleManager, @ComponentImport SoyTemplateRendererProvider soyTemplateRendererProvider, @ComponentImport UserFilterManager userFilterManager, @ComponentImport FieldConfigSchemeManager fieldConfigSchemeManager, @ComponentImport ProjectManager projectManager, @ComponentImport GlobalPermissionManager globalPermissionManager, @ComponentImport PermissionManager permissionManager, @ComponentImport WatcherManager watcherManager, @ComponentImport IssueSecurityLevelManager issueSecurityLevelManager) {
        super(customFieldValuePersister, genericConfigManager, multiUserConverter, applicationProperties, authenticationContext, searchService, fieldVisibilityManager, jiraBaseUrls, userBeanFactory, groupManager, projectRoleManager, soyTemplateRendererProvider, userFilterManager, fieldConfigSchemeManager, projectManager);


        this.userFilterManager = userFilterManager;
        this.authenticationContext = authenticationContext;
        this.globalPermissionManager = globalPermissionManager;
        this.permissionManager = permissionManager;
        this.watcherManager = watcherManager;
        this.issueSecurityLevelManager = issueSecurityLevelManager;
    }



    protected void addWatchers(Issue issue, Collection < ApplicationUser > userList) {
        if (userList != null && isIssueEditable(issue)) {
            for (ApplicationUser next: userList) {
                ApplicationUser watcher = next;

                if (!this.watcherManager.isWatching(watcher, issue)) {
                    issue = this.watcherManager.startWatching(watcher, issue);
                }
            }
        }
    }



    public void createValue(CustomField customField, Issue issue, @Nonnull Collection < ApplicationUser > value) {
        addWatchers(issue, value);
    }


    protected boolean isIssueEditable(Issue issue) {
        return (issue != null && issue.isCreated() && this.watcherManager.isWatchingEnabled() && isUserPermitted(issue));
    }



    public boolean isJiraAdmin(ApplicationUser user) {
        return this.globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }



    public boolean isUserPermitted(Issue issue) {
        ApplicationUser user = getLoggedInUser();

        if (WatcherFieldSettings.isIgnoreUserPermissions(user)) {
            return true;
        }

        if (issue == null) {
            log.warn("WatcherFieldType#isUserPermitted was called with a null issue object.  Should this have happened?");
            return false;
        }

        Project project = issue.getProjectObject();
        if (project == null) {
            log.warn("WatcherFieldType#isUserPermitted was called with an issue object with a null project.  Should this have happened?");
            return false;
        }

        return this.permissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, project, user);
    }


    public boolean isUserPermittedAsWatcher(ApplicationUser user, Project project) {
        return (project != null && this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user));
    }


    public boolean isUserPermittedAsWatcher(ApplicationUser user, Issue issue) {
        return (issue == null || this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user));
    }

    public boolean hasIssueSecurityLevelPermissions(ApplicationUser user, WatcherFieldContext context) {
        IssueSecurityLevel issueSecurityLevel = context.getIssueSecurityLevel();
        if (issueSecurityLevel != null) {
            Issue issue = context.getIssue();

            if (issue != null) {
                return this.issueSecurityLevelManager.getUsersSecurityLevels(issue, user).contains(issueSecurityLevel);
            }

            return this.issueSecurityLevelManager.getUsersSecurityLevels(context.getProject(), user).contains(issueSecurityLevel);
        }
        return true;
    }


    public String getChangelogValue(CustomField field, Collection < ApplicationUser > value) {
        List < ApplicationUser > watcherList = (List < ApplicationUser > ) value;

        if (watcherList == null || watcherList.isEmpty()) {
            return "None";
        }
        String output = "";

        for (Iterator < ApplicationUser > i = watcherList.iterator(); i.hasNext();) {
            ApplicationUser user = i.next();
            if (user == null) {
                continue;
            }
            String displayName = user.getDisplayName();
            if (displayName == null) {
                displayName = user.getName();
            }
            output = output + displayName + (i.hasNext() ? ", " : "");
        }

        return output;
    }

    public ApplicationUser getLoggedInUser() {
        return this.authenticationContext.getLoggedInUser();
    }


    public Collection < ApplicationUser > getValueFromIssue(CustomField field, Issue issue) {
        if (!issue.isCreated()) {
            return super.getValueFromIssue(field, issue);
        }

        return getWatchers(issue);
    }



    @Nonnull
    public Map < String, Object > getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map < String, Object > params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("hasPermission", Boolean.valueOf(false));

        if (issue == null || issue.getProjectObject() == null) {
            if (isJiraAdmin(getLoggedInUser())) {
                params.put("hasPermission", Boolean.valueOf(true));
            }
        } else if (isUserPermitted(issue)) {
            params.put("hasPermission", Boolean.valueOf(true));
        }

        if (WatcherFieldSettings.isHideDefaultWatcherList()) {
            WebResourceManager webResourceManager = ComponentAccessor.getWebResourceManager();
            webResourceManager.requireResource("com.burningcode.jira.issue.customfields.impl.jira-watcher-field:hide-resource");
        }
        return params;
    }


    protected List < ApplicationUser > getWatchers(Issue issue) {
        List < ApplicationUser > currWatchers = new ArrayList < > (this.watcherManager.getWatchers(issue, this.authenticationContext.getLocale()));

        currWatchers.sort((Comparator << ? super ApplicationUser > ) new ApplicationUserBestNameComparator());

        return currWatchers;
    }



    protected void removeWatchers(Issue issue, List < ApplicationUser > userList) {
        if (userList != null && isIssueEditable(issue)) {
            for (ApplicationUser next: userList) {
                ApplicationUser user = next;

                if (this.watcherManager.isWatching(user, issue)) {
                    issue = this.watcherManager.stopWatching(user, issue);
                }
            }
        }
    }



    public void updateValue(CustomField customField, Issue issue, Collection < ApplicationUser > value) {
        List < ApplicationUser > newWatchers = (List < ApplicationUser > ) value;
        List < ApplicationUser > currWatchers = getWatchers(issue);
        if (!currWatchers.isEmpty()) {
            if (newWatchers != null) {
                currWatchers.removeAll(newWatchers);
            }
            removeWatchers(issue, currWatchers);
        }

        addWatchers(issue, newWatchers);
    }
    public void validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config) {
        Collection < ApplicationUser > watchers = getValueFromCustomFieldParams(relevantParams);
        if (watchers != null && watchers.size() > 0) {
            if (!WatcherFieldSettings.ignoreBrowseIssuePermissions()) {

                WatcherFieldContext context = new WatcherFieldContext(this.issueSecurityLevelManager, relevantParams, ActionContext.getParameters());

                Issue issue = context.getIssue();
                Project project = context.getProject();
                IssueSecurityLevel issueSecurityLevel = context.getIssueSecurityLevel();

                if (project != null) {
                    boolean hasSecurityLevel = false;
                    if (issueSecurityLevel != null) {
                        hasSecurityLevel = true;
                    }

                    ArrayList < String > notPermittedUsers = new ArrayList < > ();
                    String uuid = UUID.randomUUID().toString();
                    boolean ignoreInactive = WatcherFieldSettings.isIgnoreDeactivatedWatcher();
                    for (ApplicationUser user: watchers) {
                        if (ignoreInactive && !user.isActive()) {
                            log.warn("[" + uuid + "] Ignore deactivated watchers enabled.  Allowing deactivated user '" + user.getUsername() + "' to be watcher.");
                            continue;
                        }
                        if (WatcherFieldSettings.isIgnoreDeactivatedWatcher(user)) {
                            log.warn("[" + uuid + "] Ignore deactivated watchers enabled.  Allowing deactivated user '" + user.getUsername() + "' to be watcher.");

                            continue;
                        }
                        boolean projectPerm = isUserPermittedAsWatcher(user, project);
                        boolean securityPerm = hasIssueSecurityLevelPermissions(user, context);
                        boolean issuePerm = isUserPermittedAsWatcher(user, issue);

                        if (!projectPerm) {
                            notPermittedUsers.add(user.getName());
                            continue;
                        }
                        if (hasSecurityLevel) {
                            if (!securityPerm) {
                                notPermittedUsers.add(user.getName());
                            }

                            continue;
                        }
                        if (issue != null && project.getId().equals(issue.getProjectId()) &&
                            !issuePerm) {
                            notPermittedUsers.add(user.getName());
                        }
                    }


                    if (notPermittedUsers.size() > 0)
                        errorCollectionToAddTo.addError(config.getFieldId(), "Users do not have permission to view this issue: " + StringUtils.join(notPermittedUsers, ", "), ErrorCollection.Reason.FORBIDDEN);
                }
            } else {
                log.debug("'Ignore browse issue permissions' enabled");
            }
        }
    }


    public boolean valuesEqual(Collection < ApplicationUser > v1, Collection < ApplicationUser > v2) {
        ArrayList < ApplicationUser > watcherList1 = (v1 != null) ? (ArrayList < ApplicationUser > ) v1 : new ArrayList < > ();
        ArrayList < ApplicationUser > watcherList2 = (v2 != null) ? (ArrayList < ApplicationUser > ) v2 : new ArrayList < > ();
        watcherList1.sort((Comparator << ? super ApplicationUser > ) new ApplicationUserBestNameComparator());
        watcherList2.sort((Comparator << ? super ApplicationUser > ) new ApplicationUserBestNameComparator());

        return watcherList1.equals(watcherList2);
    }
}