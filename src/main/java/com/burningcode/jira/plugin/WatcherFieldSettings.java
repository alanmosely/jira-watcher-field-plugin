package com.burningcode.jira.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import webwork.action.ActionContext;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class WatcherFieldSettings extends JiraWebActionSupport {
	private static PropertySet propertySet;
	private static final long serialVersionUID = -8378909066515942570L;
	
	private PermissionManager permissionManager;
	private JiraAuthenticationContext authenticationContext;

	public WatcherFieldSettings(PermissionManager permissionManager, JiraAuthenticationContext authenticationContext) {
		this.permissionManager = permissionManager;
		this.authenticationContext = authenticationContext;
	}
	
	@Override
	public String doDefault() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		return super.doDefault();
	}

	@Override
	protected String doExecute() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		return super.doExecute();
	}

	public String doEdit() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		PropertySet propertySet = getProperties();

		Map<?, ?> params = ActionContext.getParameters();
		if(params.containsKey("ignorePermissions") && propertySet.isSettable("ignorePermissions")) {
			Object value = params.get("ignorePermissions");
			if(value instanceof String[] && ((String[])value).length == 1) {
				propertySet.setBoolean("ignorePermissions", Boolean.parseBoolean(((String[])value)[0]));
			}
		}
		return getRedirect("WatcherFieldSettings.jspa");
	}

	@Override
	public void setErrorMessages(Collection arg0) {
		super.setErrorMessages(arg0);

	}

	public static PropertySet getPropertySet() {
		if(propertySet == null) {
			HashMap<String, Object> args = new HashMap<String, Object>();
	        args.put("delegator.name", "default");
	        args.put("entityName", "WatcherFieldSettings");
	        args.put("entityId", new Long(1));

	        propertySet = PropertySetManager.getInstance("ofbiz", args);
	        
	        // Set default settings
			if(!propertySet.exists("ignorePermissions") || !(propertySet.getObject("ignorePermissions") instanceof Boolean)) {
				propertySet.setBoolean("ignorePermissions", false);
			}
		}
		return propertySet;
	}
	
	public PropertySet getProperties() {
		return WatcherFieldSettings.getPropertySet();
	}
	
    protected boolean hasAdminPermission() {
    	return permissionManager.hasPermission(
    			Permissions.ADMINISTER,
    			authenticationContext.getLoggedInUser());
    }
}
