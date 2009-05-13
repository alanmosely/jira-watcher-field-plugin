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
 
package com.atlassian.jira.issue.customfields.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.UserComparator;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.converters.StringConverter;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.opensymphony.user.User;

/**
 * This class is a custom field type that allows users with
 * "Manage Watcher List" permissions to modify users when creating/updating issues.
 * 
 * @author Ray Barham
 * @see MultiUserCFType
 */
public class WatcherFieldType extends MultiUserCFType {

	private final JiraAuthenticationContext _AuthenticationContext;
	private final WatcherManager _WatcherManager;
	
	/**
	 * Overriden, calls super constructor.
	 */
	public WatcherFieldType(CustomFieldValuePersister customFieldValuePersister,
			StringConverter stringConverter,
			GenericConfigManager genericConfigManager,
			MultiUserConverter multiUserConverter,
			ApplicationProperties applicationProperties,
			JiraAuthenticationContext authenticationContext,
			UserPickerSearchService searchService) {
		super(customFieldValuePersister, stringConverter, genericConfigManager,
				multiUserConverter, applicationProperties,
				authenticationContext, searchService);
		_AuthenticationContext = authenticationContext;
		_WatcherManager = ComponentManager.getInstance().getWatcherManager();
	}

	/**
	 * Add a list of users as watchers on an issue.
	 * 
	 * @param issue The issue to add watchers to.
	 * @param userList A list of User objects to add as watchers.
	 */
	protected void addWatchers(Issue issue, List userList){
		if(userList != null && isIssueEditable(issue)){
			for(Iterator i = userList.iterator(); i.hasNext();){
				User user = (User)i.next();
				
				if(!_WatcherManager.isWatching(user, issue.getGenericValue())){
					_WatcherManager.startWatching(user, issue.getGenericValue());
				}
			}
		}
	}
	
	/**
	 * Overriden, see AbstractMultiCFType.createValue.  Adds a list of watchers to an issue.
	 * 
	 * @param customField See AbstractMultiCFType.createValue.
	 * @param issue See AbstractMultiCFType.createValue.
	 * @param value List of User objects to add as watchers.
	 */
	public void createValue(CustomField customField, Issue issue, Object value) {
		addWatchers(issue, (List)value);
	}

	/**
	 * Checks to see if the issue can be edited.  It checks to see if the issue has been create, if it is
	 * editable, and if the authenticated user has permissions.
	 * 
	 * @param issue The issue being edited.
	 * @return True if able to edit, false otherwise.
	 */
	protected boolean isIssueEditable(Issue issue){
		if(issue.isCreated() && issue.isEditable() && isUserPermitted(issue)){
			return true;
		}
		
		return false;
	}

	/**
	 * Checks if a user the authenticated user has the "Manage Watcher List" permission.
	 * 
	 * @param issue The issue the user is trying to add watchers to.
	 * @return True if has permissions, false otherwise.
	 */
	public boolean isUserPermitted(Issue issue){
		return ComponentManager.getInstance().getPermissionManager().hasPermission(
			Permissions.MANAGE_WATCHER_LIST, 
			issue.getProjectObject(),
			_AuthenticationContext.getUser());
	}

	/**
	 * Overriden, see AbstractMultiCFType.getValueFromIssue.  Returns the a list of watchers
	 * on the passed issue
	 * 
	 * @return List of User objects that are watchers on the passed issue.
	 */
	public Object getValueFromIssue(CustomField field, Issue issue) {
		if(!issue.isCreated()){
			return super.getValueFromIssue(field, issue);
		}
		
        return getWatchers(issue);
	}
	
	/**
	 * Overriden, see AbstractCustomFieldType.getVelocityParameters.  Adds the "hasPermissions" parameter to velocity
	 * with true if the authenticated user has "Manage Watcher List" permissions, false otherwise. 
	 */
	public Map getVelocityParameters(Issue issue, CustomField field,
			FieldLayoutItem fieldLayoutItem) {
		
		Map params = super.getVelocityParameters(issue, field, fieldLayoutItem);

		if(isUserPermitted(issue)){
			params.put("hasPermission", new Boolean(true));
		}else{
			params.put("hasPermission", new Boolean(false));
		}

		return params; 
	}
	
	/**
	 * Get a list of of watchers on an issue.
	 * 
	 * @param issue The issue to get watchers from.
	 * @return A List of User objects that are watchers on the passed issue.
	 */
	protected List getWatchers(Issue issue){
		List currWatchers = (List)_WatcherManager.getCurrentWatchList(issue.getGenericValue());
		Collections.sort(currWatchers, new UserComparator());
		
		return currWatchers;
	}

	/**
	 * Remove a list of users as watchers on an issue.
	 * 
	 * @param issue The issue to add watchers to.
	 * @param userList A list of User objects to remove from being watchers.
	 */
	protected void removeWatchers(Issue issue, List userList){
		if(userList != null && isIssueEditable(issue)){
			for(Iterator i = userList.iterator(); i.hasNext();){
				User user = (User)i.next();
				
				if(_WatcherManager.isWatching(user, issue.getGenericValue())){
					_WatcherManager.stopWatching(user, issue.getGenericValue());
				}
			}
		}
	}

	/**
	 * Overrides AbstractMultiCFType.updateValue.  Updates an issue with a list of watchers.
	 * 
	 * @param customField See AbstractMultiCFType.createValue.
	 * @param issue See AbstractMultiCFType.createValue.
	 * @param value List of User objects to update as watchers.  Note, any user not in this list that was previously
	 * a watcher will be removed.
	 */
	public void updateValue(CustomField customField, Issue issue, Object value) {
		List newWatchers = (List)value;
		List currWatchers = getWatchers(issue);
		
		if(!currWatchers.isEmpty()){
			if(newWatchers != null){
				currWatchers.removeAll(newWatchers);
			}
			removeWatchers(issue, currWatchers);
		}
		
		addWatchers(issue, newWatchers);
	}

}
