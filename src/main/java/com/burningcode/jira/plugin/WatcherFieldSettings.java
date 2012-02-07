package com.burningcode.jira.plugin;

import java.util.Collection;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class WatcherFieldSettings extends JiraWebActionSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8378909066515942570L;

	public WatcherFieldSettings() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String doDefault() throws Exception {
		// TODO Auto-generated method stub
		return super.doDefault();
	}
	
	@Override
	protected String doExecute() throws Exception {
		// TODO Auto-generated method stub
		return super.doExecute();
	}

	@Override
	public void setErrorMessages(Collection arg0) {
		// TODO Auto-generated method stub

	}
	
	public Boolean getIsAddOnEmailCreate() {
		return true;
	}

}
