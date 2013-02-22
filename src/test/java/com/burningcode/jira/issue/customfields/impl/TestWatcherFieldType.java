package com.burningcode.jira.issue.customfields.impl;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.opensymphony.module.propertyset.map.MapPropertySet;

import junit.framework.TestCase;

import webwork.action.ActionContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertySetManager.class, ActionContext.class, ComponentAccessor.class})
public class TestWatcherFieldType extends TestCase {
	private Project project;
	private PermissionManager permissionManager;
	private JiraAuthenticationContext authenticationContext;
	private WatcherManager watcherManager;
	private UserUtil userUtil;

	private JiraAuthenticationContext getAuthenticationContext(){
		JiraAuthenticationContext context = mock(JiraAuthenticationContext.class);
		return context;
	}
	
	private Issue getIssue(){
		return getIssue(true, true, this.project);
	}

	private Issue getIssue(boolean isCreated, boolean isEditable, Project project){
		Issue issue = mock(Issue.class);
		when(issue.isCreated()).thenReturn(isCreated);
		when(issue.isEditable()).thenReturn(isEditable);
		when(issue.getProjectObject()).thenReturn(project);
		
		return issue;
	}

	private PermissionManager getPermissionManager(){
		return getPermissionManager(true);
	}

	private PermissionManager getPermissionManager(boolean hasPermission){
		PermissionManager permissionManager = mock(PermissionManager.class);
		when(permissionManager.hasPermission(anyInt(), (User)anyObject())).thenReturn(hasPermission);
		when(permissionManager.hasPermission(anyInt(), (Project)anyObject(), (User)anyObject())).thenReturn(hasPermission);

		return permissionManager;
	}
	
	private Project getProject(){
		Project project = mock(Project.class);
		
		return project;
	}
	
	private void setupPropertySetManager(){
		PowerMockito.mockStatic(PropertySetManager.class);
		MapPropertySet propertySet = new MapPropertySet();
		propertySet.setMap(new HashMap<String, Object>());
		when(PropertySetManager.getInstance(eq("ofbiz"), anyMap())).thenReturn(propertySet);
	}
	
	/**
	 * Gets a list of 10 User objects for testing with {@code TestWatcherFieldType#watcherManager}.
	 * 
	 * @return List of 10 User objects.
	 */
	private ArrayList<User> getUserList(){
		return getUserList(10, this.watcherManager);
	}
	
	/**
	 * Get a list of User objects for testing.  The first and last objects in the list are
	 * set to already be watching when the {@code WatcherManager#isWatching(User, GenericValue)} is called.
	 * 
	 * @param count The number of Users to have in the list
	 * @param watcherManager The WatcherManager to use to call isWatching.
	 * @param issueGV The GenericValue of the issue to use.
	 * @return List of User objects
	 */
	private ArrayList<User> getUserList(int count, WatcherManager watcherManager){
		ArrayList<User> users = new ArrayList<User>();
		for(int i = 0; i < count; i++){
			User user = mock(User.class);
			
			// Make the first and last User objects watching the issue
			if(i == 0 || i == (count - 1)){
				when(watcherManager.isWatching(eq(user), (Issue)anyObject())).thenReturn(true);
			}else{
				when(watcherManager.isWatching(eq(user), (Issue)anyObject())).thenReturn(false);
			}
			users.add(user);
		}
		return users;
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
		return getWatcherFieldType(
			authenticationContext,
			permissionManager,
			watcherManager,
			userUtil);
	}
	private WatcherFieldType getWatcherFieldType(
			JiraAuthenticationContext authenticationContext, 
			PermissionManager permissionManager,
			WatcherManager watcherManager,
			UserUtil userUtil){

		WatcherFieldType watcherFieldType = new WatcherFieldType(
			mock(CustomFieldValuePersister.class),
			mock(GenericConfigManager.class),
			mock(MultiUserConverter.class),
			mock(ApplicationProperties.class),
			mock(JiraAuthenticationContext.class),
			mock(UserPickerSearchService.class),
			mock(FieldVisibilityManager.class),
			mock(JiraBaseUrls.class),
			permissionManager,
			watcherManager,
			userUtil,
			mock(WebResourceManager.class)
		);
		
		return watcherFieldType;
	}
	
	private WatcherManager getWatcherManager(){
		WatcherManager manager = mock(WatcherManager.class);
		when(manager.isWatchingEnabled()).thenReturn(true);
		return manager;
	}

	@Override
	protected void setUp() throws Exception {
		project = getProject();
		permissionManager = getPermissionManager();
		authenticationContext = getAuthenticationContext();
		watcherManager = getWatcherManager();
		userUtil = getUserUtil();
		
		setupPropertySetManager();
		
		super.setUp();
	}

	/**
	 * Tests {@link WatcherFieldType#addWatchers(Issue, List)} passing a list of usernames (Strings)
	 */
	public void testAddWatchersWithUsernames(){
		ArrayList<String> usernames = new ArrayList<String>();
		final ArrayList<String> expectedWatchedUsernames = new ArrayList<String>();

		when(watcherManager.isWatching((User)anyObject(), (Issue)anyObject())).thenAnswer(new Answer<Object>() {
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
		whenStartWatching(watcherManager, watchedUsers);

		WatcherFieldType fieldType = getWatcherFieldType();
		Issue issue = getIssue();
		fieldType.addWatchers(issue, usernames);
		assertEquals("Incorrect number of users watching issue", 8, watchedUsers.size());

		assertEquals("Expected watched users is different than actual", expectedWatchedUsernames.size(), watchedUsers.size());

		for(User watchedUser : watchedUsers){
			for(int i = 0; i < usernames.size(); i++){
				assertTrue("User not set as watcher that should", expectedWatchedUsernames.contains(watchedUser.getName()));
			}
		}
	}
	
	/**
	 * Tests {@link WatcherFieldType#addWatchers(Issue, List)} passing a list of User objects
	 */
	public void testAddWatchersWithUserObjects(){
		ArrayList<User> users = getUserList();
		
		// Get the users added as watchers for checking
		final ArrayList<User> watchedUsers = new ArrayList<User>();
		whenStartWatching(watcherManager, watchedUsers);

		WatcherFieldType fieldType = getWatcherFieldType();
		fieldType.addWatchers(getIssue(), users);
		assertEquals("Incorrect number of users watching issue", 8, watchedUsers.size());
		
		for(int i = 0; i < users.size(); i++){
			if(i == 0 || i == 9){
				assertFalse("User set as watcher that shouldn't", watchedUsers.contains(users.get(i)));
			}else{
				assertTrue("User not set as watcher that should", watchedUsers.contains(users.get(i)));
			}
		}
	}
	
	/**
	 * Tests {@link WatcherFieldType#createValue(CustomField, Issue, Object)}
	 */
	public void testCreateValue(){
		ArrayList<User> users = getUserList();
		
		// Get the users added as watchers for checking
		final ArrayList<User> watchedUsers = new ArrayList<User>();
		whenStartWatching(watcherManager, watchedUsers);

		WatcherFieldType fieldType = getWatcherFieldType();
		fieldType.createValue(mock(CustomField.class), getIssue(), users);
		
		assertEquals("Incorrect number of users watching issue", 8, watchedUsers.size());
	}
	
	/**
	 * Tests that {@link WatcherFieldType#getChangelogValue(CustomField, java.util.Collection)} works correctly.
	 * 
	 * Added test for JWFP-28 fix
	 */
	public void testGetChangelogValue() {
		User watcher01 = mock(User.class);
		when(watcher01.getDisplayName()).thenReturn("User Watcher 01");
		User watcher02 = mock(User.class);
		when(watcher02.getDisplayName()).thenReturn(null);
		when(watcher02.getName()).thenReturn("Watcher02");
		User watcher03 = mock(User.class);
		when(watcher03.getDisplayName()).thenReturn("User Watcher 03");

		ArrayList<User> watchers = new ArrayList<User>();
		watchers.add(watcher01);
		watchers.add(watcher02);
		watchers.add(null);
		watchers.add(watcher03);

		WatcherFieldType fieldType = getWatcherFieldType();
		String expected = "User Watcher 01, Watcher02, User Watcher 03";
		String actual = fieldType.getChangelogValue(mock(CustomField.class), watchers);
		assertEquals(expected, actual);
	}
	
	public void testIsIssueEditable(){
		WatcherFieldType fieldType = getWatcherFieldType();
		
		// Verify returns true when issue is fully editable
		assertTrue("Issue is not editable when it should be.", fieldType.isIssueEditable(getIssue()));
		
		// Verify returns false when issue is not created
		assertFalse("Issue is editable when it shouldn't be.", fieldType.isIssueEditable(getIssue(false, true, project)));

		// Verify returns false when issue is not editable
		assertFalse("Issue is editable when it shouldn't be.", fieldType.isIssueEditable(getIssue(true, false, project)));
	
		// Create a WatcherFieldType where the user does not have permission to edit an issue
		fieldType = getWatcherFieldType(
			authenticationContext, 
			getPermissionManager(false),
			watcherManager,
			userUtil);
		
		// Verify returns false when the user doesn't have permission to edit issue.
		assertFalse("Issue is editable when it shouldn't be.", fieldType.isIssueEditable(getIssue()));
	}

	/**
	 * TODO: Make unit test work
	 */
//	public void testValidateFromParams(){
//		User watcher01 = mock(User.class);
//		when(watcher01.getName()).thenReturn("user01");
//		when(watcher01.toString()).thenReturn("user01");
//		User watcher02 = mock(User.class);
//		when(watcher02.getName()).thenReturn("user02");
//		when(watcher02.toString()).thenReturn("user02");
//		
//		final ArrayList<User> watchers = new ArrayList<User>();
//		watchers.add(watcher01);
//		watchers.add(watcher02);
//
//		WatcherFieldType fieldType = getWatcherFieldType();
//		
//		CustomFieldParams relevantParams = mock(CustomFieldParams.class);
//		when(fieldType.getValueFromCustomFieldParams(relevantParams)).thenReturn(watchers);
//		
//		final ArrayList<HashMap> errors = new ArrayList<HashMap>();
//		ErrorCollection errorCollectionToAddTo = mock(ErrorCollection.class);
//
//		doAnswer(new DoesNothing() {
//			public Object answer(InvocationOnMock invocation) {
//	            Object[] args = invocation.getArguments();
//	            HashMap<String, Object> error = new HashMap<String, Object>();
//	            error.put("field", (String)args[0]);
//	            error.put("error", (String)args[1]);
//	            error.put("reason", (ErrorCollection.Reason)args[2]);
//	            errors.add(error);
//	            return null;
//	        }
//		}).when(errorCollectionToAddTo).addError(anyString(), anyString(), any(ErrorCollection.Reason.class));
//		
//		FieldConfig config = mock(FieldConfig.class);
//		when(config.getFieldId()).thenReturn("customField_10000");
//		
//		PowerMockito.mockStatic(ActionContext.class);
//		HashMap<String, String[]> params = new HashMap<String, String[]>();
//		params.put("pid", new String[]{"10000"});
//		params.put("id", null);
//		when(ActionContext.getParameters()).thenReturn(params);
//		
//		PowerMockito.mockStatic(ComponentManager.class);
//		ProjectManager projectManager = getProjectManager();
//		when(ComponentManager.getComponentInstanceOfType(ProjectManager.class)).thenReturn(projectManager);
//		when(projectManager.getProjectObj(Long.valueOf(params.get("pid")[0]))).thenReturn(getProject());
//		
//		fieldType.validateFromParams(relevantParams, errorCollectionToAddTo, config);
//		
//		
//		//validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config)
//	}
	
	/**
	 * Sets {@code WatcherManager#startWatching(User, GenericValue)} to add users to the
	 * watchedUsers list.
	 * 
	 * @param watcherManager The WatcherManager with {@code WatcherManager#startWatching(User, GenericValue)} method being called 
	 * @param watchedUsers The list that will store the watching User objects.
	 */
	private void whenStartWatching(WatcherManager watcherManager, final ArrayList<User> watchedUsers){
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				watchedUsers.add((User)args[0]);
				return null;
			}
		}).when(watcherManager).startWatching((User)anyObject(), (Issue)anyObject());
	}
}
