package com.burningcode.jira.issue.customfields.searchers;

import com.atlassian.jira.issue.customfields.searchers.ExactTextSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.web.FieldVisibilityManager;

public class WatcherSearcher extends ExactTextSearcher{

	public WatcherSearcher(JqlOperandResolver jqlOperandResolver,
			CustomFieldInputHelper customFieldInputHelper,
			FieldVisibilityManager fieldVisibilityManager) {
		super(jqlOperandResolver, customFieldInputHelper, fieldVisibilityManager);
	}
}