package com.burningcode.jira.rest;

import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.opensymphony.user.User;

/**
 * Used for access watcher info for an issue using REST API
 * TODO Replace @AnonymousAllowed
 * TODO Check for user permissions to access watcher info.
 * @author rbarham
 *
 */
@Path("/watchers")
@AnonymousAllowed
public class WatcherResource {
	private JiraAuthenticationContext authenticationContext;
	private PermissionManager permissionManager;
	private WatcherManager watcherManaer;

	public WatcherResource(){
		authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
		permissionManager = ComponentManager.getInstance().getPermissionManager();
		watcherManaer = ComponentManager.getInstance().getWatcherManager();
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getWatchers(@QueryParam("issueKey") String issueKey, @QueryParam("issueId") Long issueId){
	//public Response getWatchers(@Context HttpServletRequest request, @QueryParam("atl_token") String atlToken, @QueryParam("issueKey") String issueKey, @QueryParam("issueId") Long issueId){
		
		// Check that the passed users token is valid
//		XsrfTokenGenerator tokenGenerator = ComponentManager.getComponentInstanceOfType(XsrfTokenGenerator.class);
//		if(!tokenGenerator.validateToken(request, atlToken)){
//			return Response.status(Status.UNAUTHORIZED).build();
//		}

		MutableIssue issue = null;
		try{
			if(issueKey != null){
				issue = ComponentManager.getInstance().getIssueManager().getIssueObject(issueKey);
			}else if(issueId != null){
				issue = ComponentManager.getInstance().getIssueManager().getIssueObject(issueId);
			}
		
			if(issue == null){
				return Response.status(Status.BAD_REQUEST).build();
			}
		}catch(DataAccessException e){
			return Response.status(Status.INTERNAL_SERVER_ERROR).build(); 
		}
		
//		if(!isUserPermitted(issue)){
//			return Response.status(Status.UNAUTHORIZED).build();
//		}

		Collection<User> watcherList = watcherManaer.getCurrentWatchList(authenticationContext.getLocale(), issue.getGenericValue());
		ArrayList<Watcher> watchers = new ArrayList<Watcher>();
		for(User watcher : watcherList){
			watchers.add(new Watcher(watcher.getName(), watcher.getFullName()));
		}
		
		Collection<CustomField> customFields = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjects(issue);
		ArrayList<String> watcherFieldIds = new ArrayList<String>();
		for(CustomField field : customFields){
			String key = field.getCustomFieldType().getKey();
			if(key.equalsIgnoreCase("com.burningcode.jira.issue.customfields.impl.jira-watcher-field:watcherfieldtype")){
				watcherFieldIds.add(field.getId());
			}
		}

		return Response.ok(new Watchers(issue.getId(), watchers, watcherFieldIds)).build();
	}
	
	/**
     * Checks if a user the authenticated user has the "View Voters and Watchers" permission.
     * 
     * @param issue The issue the user is trying to view watchers.
     * @return True if has permissions, false otherwise.
     */
    protected boolean isUserPermitted(Issue issue){
        return permissionManager.hasPermission(
                Permissions.VIEW_VOTERS_AND_WATCHERS, 
                issue.getProjectObject(),
                authenticationContext.getUser());
    }
}