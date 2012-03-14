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

package com.burningcode.jira.web.action.issue;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.jira.bc.issue.watcher.WatcherService;
import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.web.action.issue.ManageWatchers;
import com.atlassian.velocity.VelocityManager;

/**
 * This class is used to reindex an issue when watchers are added via the "Manage Watch List" control.
 *
 * Thanks to the developers at Voters and Watchers Custom Field (http://confluence.atlassian.com/display/JIRAEXT/Voters+and+Watchers+Custom+Field) for
 * their work on ManageWatcherAlias.  Your code is awesome!  I can not take credit for this code as it is essentially the same as ManageWatcherAlias.
 * 
 * @author Ray Barham
 * @see com.atlassian.jira.web.action.issue.ManageWatchers
 * @see com.atlassian.jira.plugin.votersAndWatchers.ManageWatcherAlias
 */
public class ManageWatchersReindex extends ManageWatchers {
	private static final long serialVersionUID = 7850186457142561095L;
    private static final Logger log = Logger.getLogger(ManageWatchersReindex.class);
    private final IssueIndexManager issueIndexManager;

    /**
     * Default constructor
     * 
     * @param watcherManager
     * @param velocityManager
     * @param searchService
     */
	public ManageWatchersReindex(WatcherManager watcherManager,
			VelocityManager velocityManager,
			UserPickerSearchService searchService,
			WatcherService watcherService, PermissionManager permissionManager,
			CrowdService crowdService,
			IssueIndexManager issueIndexManager) {
		super(watcherManager, velocityManager, searchService, watcherService,
				permissionManager, crowdService);
		this.issueIndexManager = issueIndexManager;
	}

    /**
     * Reindexes the current issue
     */
    protected void reindexIssue(){
        try {
            if(issueIndexManager.isIndexingEnabled()){
                issueIndexManager.reIndex(getIssueObject());
            }
        } catch (IndexException e) {
            log.error("Error reindexing " + getIssueObject().getKey() + ": ", e);
        }
    }
    /**
     * Causes the current issue to be reindexed when watchers are added.
     * 
     * @throws com.opensymphony.user.EntityNotFoundException, org.ofbiz.core.entity.GenericEntityException
     */
    public String doStartWatchers() throws GenericEntityException {
        String result = super.doStartWatchers();
        reindexIssue();

        return result;
    }

    /**
     * Causes the current issue to be reindexed when the current users starts watching the issue.
     * 
     * @throws org.ofbiz.core.entity.GenericEntityException
     */
    public String doStartWatching() throws GenericEntityException {
        String result = super.doStartWatching();
        reindexIssue();

        return result;
    }

    /**
     * Causes the current issue to be reindexed when watchers are removed.
     * 
     * @throws com.opensymphony.user.EntityNotFoundException, org.ofbiz.core.entity.GenericEntityException
     */
    public String doStopWatchers() throws GenericEntityException {
        String result = super.doStopWatchers();
        reindexIssue();

        return result;
    }

    /**
     * Causes the current issue to be reindexed when the current users stops watching the issue.
     * 
     * @throws org.ofbiz.core.entity.GenericEntityException
     */
    public String doStopWatching() throws GenericEntityException {
        String result = super.doStopWatching();
        reindexIssue();

        return result;
    }
}
