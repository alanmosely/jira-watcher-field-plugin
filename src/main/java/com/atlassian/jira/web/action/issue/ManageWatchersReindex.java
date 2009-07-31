/**
 * Thanks to the developers at Voters and Watchers Custom Field (http://confluence.atlassian.com/display/JIRAEXT/Voters+and+Watchers+Custom+Field) for
 * their work on ManageWatcherAlias.  Your code is awesome!  I can not take credit for this code as it is essentially the same that you made.
 */
package com.atlassian.jira.web.action.issue;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.velocity.VelocityManager;
import com.opensymphony.user.EntityNotFoundException;

public class ManageWatchersReindex extends ManageWatchers {
	private static final long serialVersionUID = 7850186457142561095L;
	private static final Logger log = Logger.getLogger(ManageWatchersReindex.class);
	private final IssueIndexManager issueIndexManager;

	public ManageWatchersReindex(WatcherManager watcherManager,
			VelocityManager velocityManager,
			UserPickerSearchService searchService) {
		super(watcherManager, velocityManager, searchService);
		issueIndexManager = ComponentManager.getInstance().getIndexManager();
	}
	
	protected void reIndexIssue(){
		try {
			if(issueIndexManager.isIndexingEnabled()){
				issueIndexManager.reIndex(getIssueObject());
			}
		} catch (IndexException e) {
			log.error("Error reindexing " + getIssueObject().getKey() + ": ", e);
		}
	}
	
	public String doStartWatchers() throws EntityNotFoundException,
			GenericEntityException {
		String result = super.doStartWatchers();
		reIndexIssue();
		
		return result;
	}
	
	public String doStartWatching() throws GenericEntityException {
		String result = super.doStartWatching();
		reIndexIssue();
		
		return result;
	}
	
	public String doStopWatchers() throws EntityNotFoundException,
			GenericEntityException {
		String result = super.doStopWatchers();
		reIndexIssue();
		
		return result;
	}
	
	public String doStopWatching() throws GenericEntityException {
		String result = super.doStopWatching();
		reIndexIssue();
		
		return result;
	}
}
