package com.atlassian.jira.issue.customfields.searchers;

import com.atlassian.jira.issue.customfields.converters.UserConverter;
import com.atlassian.jira.web.bean.FieldVisibilityBean;

public class WatcherSearcher extends UserPickerSearcher {

	public WatcherSearcher(UserConverter userConverter,
			FieldVisibilityBean fieldVisibilityBean) {
		super(userConverter, fieldVisibilityBean);
		// TODO Auto-generated constructor stub
	}
}
