package com.burningcode.jira.rest;

import java.util.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
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
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getWatchers(@QueryParam("issueKey") String issueKey){
		MutableIssue issue = ComponentManager.getInstance().getIssueManager().getIssueObject(issueKey);
		return getWatcherResponse(issue);
	}
	
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getWatchers(@QueryParam("issueid") Long issueId){
		MutableIssue issue = ComponentManager.getInstance().getIssueManager().getIssueObject(issueId);
		return getWatcherResponse(issue);
	}
	
	protected Response getWatcherResponse(MutableIssue issue){
		if(issue == null){
			return Response.noContent().build();
		}
		
		JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
		Collection<User> watcherList = ComponentManager.getInstance().getWatcherManager().getCurrentWatchList(authenticationContext.getLocale(), issue.getGenericValue());
		return Response.ok(new Watchers(issue, watcherList)).build();
	}
}
