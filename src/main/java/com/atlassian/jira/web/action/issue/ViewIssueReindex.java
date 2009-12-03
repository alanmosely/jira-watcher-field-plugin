package com.atlassian.jira.web.action.issue;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenRendererFactory;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.thumbnail.ThumbnailManager;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculatorFactory;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.trackback.TrackbackManager;
import com.atlassian.plugin.PluginManager;

/**
 * Used to reindex the issue if the user select to watch the issue via that quick link in the issue.
 * 
 * @author Ray Barham
 */
public class ViewIssueReindex extends ViewIssue {

    private static final long serialVersionUID = 6593985225886472089L;
    private final IssueIndexManager issueIndexManager;
    private boolean isReindexIssue;

    /**
     * @see com.atlassian.jira.web.action.issue.ViewIssue#ViewIssue(TrackbackManager, ThumbnailManager, SubTaskManager, IssueLinkManager, VoteManager, WatcherManager, com.atlassian.plugin.PluginAccessor, FieldManager, FieldScreenRendererFactory, FieldLayoutManager, RendererManager, CommentManager, ProjectRoleManager, CommentService, AttachmentService, AggregateTimeTrackingCalculatorFactory)
     * @param trackbackManager
     * @param thumbnailManager
     * @param subTaskManager
     * @param issueLinkManager
     * @param voteManager
     * @param watcherManager
     * @param pluginManager
     * @param fieldManager
     * @param fieldScreenRendererFactory
     * @param fieldLayoutManager
     * @param rendererManager
     * @param commentManager
     * @param projectRoleManager
     * @param commentService
     * @param attachmentService
     * @param aggregateTimeTrackingCalculatorFactory
     */
    public ViewIssueReindex(
            TrackbackManager trackbackManager,
            ThumbnailManager thumbnailManager,
            SubTaskManager subTaskManager,
            IssueLinkManager issueLinkManager,
            VoteManager voteManager,
            WatcherManager watcherManager,
            PluginManager pluginManager,
            FieldManager fieldManager,
            FieldScreenRendererFactory fieldScreenRendererFactory,
            FieldLayoutManager fieldLayoutManager,
            RendererManager rendererManager,
            CommentManager commentManager,
            ProjectRoleManager projectRoleManager,
            CommentService commentService,
            AttachmentService attachmentService,
            AggregateTimeTrackingCalculatorFactory aggregateTimeTrackingCalculatorFactory) {
        super(trackbackManager, thumbnailManager, subTaskManager,
                issueLinkManager, voteManager, watcherManager, pluginManager,
                fieldManager, fieldScreenRendererFactory, fieldLayoutManager,
                rendererManager, commentManager, projectRoleManager,
                commentService, attachmentService,
                aggregateTimeTrackingCalculatorFactory);

        issueIndexManager = ComponentManager.getInstance().getIndexManager();
        isReindexIssue = false;
    }

    /**
     * If the current user is set to watch this issue, reindex the issue after adding.
     */
    protected String doExecute() throws Exception {
        String result = super.doExecute();

        if(isReindexIssue){
            try {
                if(issueIndexManager.isIndexingEnabled()){
                    issueIndexManager.reIndex(getIssueObject());
                }
            } catch (IndexException e) {
                log.error("Error reindexing " + getIssueObject().getKey() + ": ", e);
            }

            isReindexIssue = false;
        }

        return result;
    }

    /**
     * Sets the issue to be reindex on doExecute
     * 
     * @param watch	The username of the user to start watching.
     */
    public void setWatch(String watch) {
        isReindexIssue = true;
        super.setWatch(watch);
    }
}
