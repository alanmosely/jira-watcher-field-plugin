package com.burningcode.jira.issue.customfields.searchers;

import com.atlassian.jira.issue.customfields.searchers.ExactTextSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;

public class WatcherSearcher extends ExactTextSearcher{

	public WatcherSearcher(JqlOperandResolver jqlOperandResolver, CustomFieldInputHelper customFieldInputHelper) {
		super(jqlOperandResolver, customFieldInputHelper);
	}
	
}