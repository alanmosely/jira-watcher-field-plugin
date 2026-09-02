package com.burningcode.jira.issue.customfields.searchers;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.customfields.converters.UserConverter;
import com.atlassian.jira.issue.customfields.searchers.UserPickerGroupSearcher;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.UserResolver;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import com.atlassian.jira.user.UserFilterManager;
import com.atlassian.jira.user.UserHistoryManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.EmailFormatter;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Searcher for the watcher field. Extends the same searcher Jira uses for its own
 * multi-user picker fields so watcher values are indexed per user via the UserConverter.
 * (The previous ExactTextSearcher base passed the whole user collection to
 * getStringFromSingularObject, which threw ClassCastException during indexing.)
 */
@Named
public class WatcherSearcher extends UserPickerGroupSearcher {

	@Inject
	public WatcherSearcher(JiraAuthenticationContext authenticationContext,
			@ComponentImport VelocityRequestContextFactory velocityRequestContextFactory,
			@ComponentImport VelocityTemplatingEngine templatingEngine,
			@ComponentImport ApplicationProperties applicationProperties,
			@ComponentImport UserSearchService userSearchService,
			@ComponentImport FieldVisibilityManager fieldVisibilityManager,
			@ComponentImport JqlOperandResolver jqlOperandResolver,
			@ComponentImport UserManager userManager,
			@ComponentImport CustomFieldInputHelper customFieldInputHelper,
			@ComponentImport GroupManager groupManager,
			PermissionManager permissionManager,
			@ComponentImport UserHistoryManager userHistoryManager,
			@ComponentImport UserFilterManager userFilterManager,
			@ComponentImport EmailFormatter emailFormatter) {
		// UserConverter and UserResolver are not reliably exported as OSGi services,
		// so they are resolved from Jira's component container instead of injected.
		super(ComponentAccessor.getComponent(UserConverter.class),
				authenticationContext,
				velocityRequestContextFactory,
				templatingEngine,
				applicationProperties,
				userSearchService,
				fieldVisibilityManager,
				jqlOperandResolver,
				ComponentAccessor.getComponent(UserResolver.class),
				userManager,
				customFieldInputHelper,
				groupManager,
				permissionManager,
				userHistoryManager,
				userFilterManager,
				emailFormatter);
	}
}
