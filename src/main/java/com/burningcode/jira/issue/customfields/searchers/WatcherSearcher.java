package com.burningcode.jira.issue.customfields.searchers;

import com.atlassian.jira.issue.customfields.searchers.ExactTextSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class WatcherSearcher extends ExactTextSearcher{

	@Inject
	public WatcherSearcher(@ComponentImport JqlOperandResolver jqlOperandResolver,
			@ComponentImport CustomFieldInputHelper customFieldInputHelper,
			@ComponentImport FieldVisibilityManager fieldVisibilityManager) {
		super(jqlOperandResolver, customFieldInputHelper, fieldVisibilityManager);
	}
}