package com.burningcode.jira.issue.customfields.impl;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.converters.StringConverter;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.webresource.WebResourceManager;

import junit.framework.TestCase;

public class TestWatcherFieldType extends TestCase {
	private Project project;
	private PermissionManager permissionManager;
	private JiraAuthenticationContext authenticationContext;
	private WatcherManager watcherManager;
	private UserUtil userUtil;

	@Override
	protected void setUp() throws Exception {
		project = getProject();
		permissionManager = getPermissionManager();
		authenticationContext = getAuthenticationContext();
		watcherManager = getWatcherManager();
		userUtil = getUserUtil();
		
		super.setUp();
	}
	
	private JiraAuthenticationContext getAuthenticationContext(){
		JiraAuthenticationContext context = mock(JiraAuthenticationContext.class);
		return context;
	}

	private Project getProject(){
		Project project = mock(Project.class);
		
		return project;
	}
	
	private PermissionManager getPermissionManager(){
		PermissionManager permissionManager = mock(PermissionManager.class);
		when(permissionManager.hasPermission(anyInt(), (User)anyObject())).thenReturn(true);
		when(permissionManager.hasPermission(anyInt(), (Project)anyObject(), (User)anyObject())).thenReturn(true);

		return permissionManager;
	}
	
	private WatcherManager getWatcherManager(){
		WatcherManager manager = mock(WatcherManager.class);
		return manager;
	}

	private Issue getIssue(){
		Issue issue = mock(Issue.class);
		when(issue.isCreated()).thenReturn(true);
		when(issue.isEditable()).thenReturn(true);
		when(issue.getProjectObject()).thenReturn(project);
		
		return issue;
	}
	
	private UserUtil getUserUtil(){
		UserUtil userUtil = mock(UserUtil.class);
		when(userUtil.getUserObject(anyString())).thenAnswer(new Answer<User>() {

			public User answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				User user = mock(User.class);
				when(user.getName()).thenReturn((String)args[0]);

				return user;
			}
		});

		return userUtil;
	}
	
	private WatcherFieldType getWatcherFieldType(){
		WatcherFieldType watcherFieldType = new WatcherFieldType(
			mock(CustomFieldValuePersister.class), 
			mock(StringConverter.class), 
			mock(GenericConfigManager.class), 
			mock(MultiUserConverter.class), 
			mock(ApplicationProperties.class), 
			authenticationContext,
			mock(UserPickerSearchService.class), 
			mock(FieldVisibilityManager.class),
			permissionManager,
			watcherManager,
			userUtil,
			mock(WebResourceManager.class)
		);
		
		return watcherFieldType;
	}

	/**
	 * Tests {@link WatcherFieldType#addWatchers(Issue, List)} passing a list of User objects
	 */
	public void testAddWatchersWithUserObjects(){
		ArrayList<User> users = new ArrayList<User>();
		for(int i = 0; i < 10; i++){
			User user = mock(User.class);
			
			// Make the first and last User objects watching the issue
			if(i == 0 || i == 9){
				when(watcherManager.isWatching(eq(user), (GenericValue)anyObject())).thenReturn(true);
			}else{
				when(watcherManager.isWatching(eq(user), (GenericValue)anyObject())).thenReturn(false);
			}
			users.add(user);
		}
		
		// Get the users added as watchers for checking
		final ArrayList<User> watchedUsers = new ArrayList<User>();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				watchedUsers.add((User)args[0]);
				return null;
			}
		}).when(watcherManager).startWatching((User)anyObject(), (GenericValue)anyObject());

		WatcherFieldType fieldType = getWatcherFieldType();
		fieldType.addWatchers(getIssue(), users);
		
		for(int i = 0; i < users.size(); i++){
			if(i == 0 || i == 9){
				assertFalse("User set as watcher that shouldn't", watchedUsers.contains(users.get(i)));
			}else{
				assertTrue("User not set as watcher that should", watchedUsers.contains(users.get(i)));
			}
		}
	}
	
	/**
	 * Tests {@link WatcherFieldType#addWatchers(Issue, List)} passing a list of usernames (Strings)
	 */
	public void testAddWatchersWithUsernames(){
		ArrayList<String> usernames = new ArrayList<String>();
		final ArrayList<String> expectedWatchedUsernames = new ArrayList<String>();

		when(watcherManager.isWatching((User)anyObject(), (GenericValue)anyObject())).thenAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				User user = (User)args[0];
				boolean isWatching = !expectedWatchedUsernames.contains((String)user.getName());
				return isWatching;
			}
		});

		for(int i = 0; i < 10; i++){
			String username = String.valueOf(i);
			
			// Make the first and last User objects watching the issue
			if(i != 0 && i != 9){
				expectedWatchedUsernames.add(username);
			}

			usernames.add(username);
		}
		
		// Get the users added as watchers for checking
		final ArrayList<User> watchedUsers = new ArrayList<User>();
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				watchedUsers.add((User)args[0]);
				return null;
			}
		}).when(watcherManager).startWatching((User)anyObject(), (GenericValue)anyObject());

		WatcherFieldType fieldType = getWatcherFieldType();
		fieldType.addWatchers(getIssue(), usernames);
		
		assertEquals("Expected watched users is different than actual", expectedWatchedUsernames.size(), watchedUsers.size());

		for(User watchedUser : watchedUsers){
			for(int i = 0; i < usernames.size(); i++){
				assertTrue("User not set as watcher that should", expectedWatchedUsernames.contains(watchedUser.getName()));
			}
		}
	}
}
