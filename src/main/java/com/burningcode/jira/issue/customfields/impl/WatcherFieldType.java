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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import webwork.action.ActionContext;

import com.atlassian.jira.bc.user.search.UserPickerSearchService;
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
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.crowd.embedded.api.User;
import com.burningcode.jira.plugin.WatcherFieldSettings;
import com.opensymphony.module.propertyset.PropertySet;

/**
 * This class is a custom field type that allows users with
 * "Manage Watcher List" permissions to modify users when creating/updating issues.
 * 
 * @author Ray Barham
 * @see com.atlassian.jira.issue.customfields.impl.MultiUserCFType
 */
public class WatcherFieldType extends MultiUserCFType {

	private static final Logger log = Logger.getLogger(WatcherFieldType.class);

    private final JiraAuthenticationContext _AuthenticationContext;
    private final PermissionManager _PermissionManager;
    private final WatcherManager _WatcherManager;
    private final UserUtil _UserUtil;

    /**
     * Overridden, calls super constructor.
     * @param customFieldValuePersister 
     * @param stringConverter 
     * @param genericConfigManager 
     * @param multiUserConverter 
     * @param applicationProperties 
     * @param authenticationContext 
     * @param searchService 
     * 
     * @see com.atlassian.jira.issue.customfields.impl.MultiUserCFType#MultiUserCFType
     */
    public WatcherFieldType(
			CustomFieldValuePersister customFieldValuePersister,
			GenericConfigManager genericConfigManager,
			MultiUserConverter multiUserConverter,
			ApplicationProperties applicationProperties,
			JiraAuthenticationContext authenticationContext,
			UserPickerSearchService searchService,
			FieldVisibilityManager fieldVisibilityManager,
			JiraBaseUrls jiraBaseUrls,
            PermissionManager permissionManager,
            WatcherManager watcherManager,
            UserUtil userUtil,
            WebResourceManager webResourceManager) {
		super(customFieldValuePersister, genericConfigManager, multiUserConverter,
				applicationProperties, authenticationContext, searchService,
				fieldVisibilityManager, jiraBaseUrls);
        _AuthenticationContext = authenticationContext;
        _PermissionManager = permissionManager;
        _WatcherManager = watcherManager;
        _UserUtil = userUtil;
	}

    /**
     * Add a list of users as watchers on an issue.
     * 
     * @param issue The issue to add watchers to.
     * @param userList A list of User objects to add as watchers.
     */
    protected void addWatchers(Issue issue, Collection<?> userList){
        if(userList != null && isIssueEditable(issue)){
            for(Iterator<?> i = userList.iterator(); i.hasNext();){
            	Object next = i.next();
            	User watcher = null;

            	if(next instanceof User){
            		watcher = (User)next;
            	}else if(next instanceof String){
            		watcher = _UserUtil.getUserObject((String)next);
            	}

            	// JWFP-22: Added check for watcher's permission to browse project
                if(watcher != null && _PermissionManager.hasPermission(Permissions.BROWSE, issue.getProjectObject(), watcher) && !_WatcherManager.isWatching(watcher, issue)){
                    _WatcherManager.startWatching(watcher, issue);
                }
            }
        }
    }

    /**
     * Overridden, adds a list of watchers to an issue.
     * 
     * @param customField See AbstractMultiCFType.createValue.
     * @param issue See AbstractMultiCFType.createValue.
     * @param value List of User objects to add as watchers.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#createValue(CustomField, Issue, Object)
     */
    @Override
    public void createValue(CustomField customField, Issue issue, Collection<User> value) {
    	addWatchers(issue, value);
    }

    /**
     * Checks to see if the issue can be edited.  It checks to see if the issue has been create, if it is
     * editable, and if the authenticated user has permissions.
     * 
     * @param issue The issue being edited.
     * @return True if able to edit, false otherwise.
     */
    protected boolean isIssueEditable(Issue issue){
        if(issue.isCreated() && issue.isEditable() && _WatcherManager.isWatchingEnabled() && isUserPermitted(issue)){
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
    public boolean isJiraAdmin(User user){
        return _PermissionManager.hasPermission(Permissions.ADMINISTER, user);
    }

    /**
     * Checks if a user the authenticated user has the "Manage Watcher List" permission.
     * 
     * @param issue The issue the user is trying to add watchers to.
     * @return True if has permissions, false otherwise.
     */
    public boolean isUserPermitted(Issue issue){
    	PropertySet propertySet = WatcherFieldSettings.getPropertySet();
    	User user = _AuthenticationContext.getLoggedInUser();

    	// Allow JIRA service to set the watcher field, if enabled to do so.
    	if(propertySet.exists("ignorePermissions") && propertySet.getBoolean("ignorePermissions") && user == null)
    		return true;

        return _PermissionManager.hasPermission(
                Permissions.MANAGE_WATCHER_LIST, 
                issue.getProjectObject(),
                user);
    }

    /**
     * Overridden, returns the value reported in the changelog
     * 
     * @return The full names of watching users in a comma separated list.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#getChangelogValue(CustomField, Object)
     */
	public String getChangelogValue(CustomField field, Collection<User> value) {
   		List<User> watcherList = (List<User>)value;
   		
        if(watcherList == null || watcherList.isEmpty())
            return "None";

        String output = "";
        for(Iterator<User> i = watcherList.iterator(); i.hasNext();){
            User user = (User)i.next();

        	// Fix for JWFP-28
        	if(user == null)
        		continue;

            String displayName = user.getDisplayName();

            // Add fix for issue JWFP-25
            if(displayName == null)
            	displayName = user.getName();

            output += displayName + (i.hasNext()? ", " : "");
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
    public Collection<User> getValueFromIssue(CustomField field, Issue issue) {
        if(!issue.isCreated()){
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
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("hasPermission", new Boolean(false));

        if(issue == null || issue.getProjectObject() == null){
        	if(isJiraAdmin(_AuthenticationContext.getLoggedInUser())){
        		params.put("hasPermission", new Boolean(true));
        	}
        }else if(isUserPermitted(issue)){
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
	protected List<User> getWatchers(Issue issue){
   		List<User> currWatchers = (List<User>)_WatcherManager.getCurrentWatchList(issue, _AuthenticationContext.getLocale());
        Collections.sort(currWatchers, (Comparator<? super User>)(new UserComparator()));

        return currWatchers;
    }

    /**
     * Remove a list of users as watchers on an issue.
     * 
     * @param issue The issue to add watchers to.
     * @param userList A list of User objects to remove from being watchers.
     */
    protected void removeWatchers(Issue issue, List<?> userList){
        if(userList != null && isIssueEditable(issue)){
            for(Iterator<?> i = userList.iterator(); i.hasNext();){
            	Object next = i.next();
            	User user = null;

            	if(next instanceof User){
            		user = (User)next;
            	}else if(next instanceof String){
            		user = _UserUtil.getUserObject((String)next);
            	}

                if(user != null && _WatcherManager.isWatching(user, issue)){
                    _WatcherManager.stopWatching(user, issue);
                }
            }
        }
    }

    /**
     * Overridden, updates an issue with a list of watchers.
     * 
     * @param customField See AbstractMultiCFType.createValue.
     * @param issue See AbstractMultiCFType.createValue.
     * @param value List of User objects to update as watchers.  Note, any user not in this list that was previously
     * a watcher will be removed.
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#updateValue(CustomField, Issue, Object)
     */
	public void updateValue(CustomField customField, Issue issue, Collection<User> value) {
        List<User> newWatchers = (List<User>)value;
        List<User> currWatchers = getWatchers(issue);

        if(!currWatchers.isEmpty()){
            if(newWatchers != null){
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
		if(pid != null)
			project = ComponentAccessor.getProjectManager().getProjectObj(Long.valueOf(pid[0]));

		Issue issue = null;
		if(id != null)
			issue = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(id[0]));

		ArrayList<String> invalidUsers = new ArrayList<String>();
		Collection<User> watchers = getValueFromCustomFieldParams(relevantParams);
		if(watchers != null && watchers.size() > 0){
			for(User user : watchers){
				if((project != null && !_PermissionManager.hasPermission(Permissions.BROWSE, project, user)) || (issue != null && !_PermissionManager.hasPermission(Permissions.BROWSE, issue, user))){
					invalidUsers.add(user.getName());
				}
			}
		}

		if(invalidUsers.size() > 0)
			errorCollectionToAddTo.addError(config.getFieldId(), "Users do not have permission to browse issue: "+StringUtils.join(invalidUsers, ", "), ErrorCollection.Reason.FORBIDDEN);
		
		super.validateFromParams(relevantParams, errorCollectionToAddTo, config);
	}

    /**
     * Overridden, returns true if the current watcher list is equal to the new ones provided.
     * 
     * @see com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType#valuesEqual(Object, Object)
     */
    @Override
    public boolean valuesEqual(Collection<User> v1, Collection<User> v2) {
    	ArrayList<User> watcherList1 = (v1 != null? (ArrayList<User>)v1 : new ArrayList<User>());
    	ArrayList<User> watcherList2 = (v2 != null? (ArrayList<User>)v2 : new ArrayList<User>());
    	Collections.sort(watcherList1, (Comparator<? super User>)(new UserComparator()));
    	Collections.sort(watcherList2, (Comparator<? super User>)(new UserComparator()));
   		
        if(watcherList1.equals(watcherList2)){
            return true;
        }

        return false;
    }
}
