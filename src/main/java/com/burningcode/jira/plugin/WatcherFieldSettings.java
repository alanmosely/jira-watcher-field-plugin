package com.burningcode.jira.plugin;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.annotations.security.AdminOnly;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import webwork.action.ActionContext;

import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.JiraWebActionSupport;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Used to handle settings for the JIRA Watcher Field.
 * @author Ray
 *
 */
@Named
@AdminOnly
@WebSudoRequired
@SupportedMethods({RequestMethod.GET})
public class WatcherFieldSettings extends JiraWebActionSupport {
	private static PropertySet propertySet;
	private static final long serialVersionUID = -8378909066515942570L;
	private static final Logger log = LoggerFactory.getLogger(WatcherFieldSettings.class);
	@ComponentImport
	private final JiraAuthenticationContext authenticationContext;
	@ComponentImport
	private final GlobalPermissionManager globalPermissionManager;

	/**
	 * Default Constructor
	 */
	@Inject
	public WatcherFieldSettings(JiraAuthenticationContext authenticationContext, GlobalPermissionManager globalPermissionManager) {
		this.authenticationContext = authenticationContext;
		this.globalPermissionManager = globalPermissionManager;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String doDefault() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		return super.doDefault();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String doExecute() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		return super.doExecute();
	}

	/**
	 * Called when editing the settings
	 */
	@RequiresXsrfCheck
	@SupportedMethods({RequestMethod.POST})
	public String doEdit() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		PropertySet propertySet = getProperties();

		Map<?, ?> params = ActionContext.getParameters();
		if(params.containsKey("ignorePermissions") && propertySet.isSettable("ignorePermissions")) {
			Object value = params.get("ignorePermissions");
			if(value instanceof String[] && ((String[])value).length == 1) {
				// Also the repair path for a tampered row: the cached entry store
				// converts a type-conflicting row on set rather than throwing.
				propertySet.setBoolean("ignorePermissions", Boolean.parseBoolean(((String[])value)[0]));
			}
		}
		return getRedirect("WatcherFieldSettings.jspa");
	}

	/**
	 * Static method that returns the PropertySet used to get/store settings in the database
	 * @return The PropertySet to reference the data
	 */
	public static synchronized PropertySet getPropertySet() {
		if(propertySet == null) {
			HashMap<String, Object> args = new HashMap<String, Object>();
	        args.put("delegator.name", "default");
	        args.put("entityName", "WatcherFieldSettings");
	        args.put("entityId", 1L);

	        // "ofbiz-cached" wraps the same OFBiz storage in Jira's cluster-aware
	        // cache (invalidated across Data Center nodes on write), so the
	        // per-render reads in WatcherFieldType don't each hit the database.
	        // No default is seeded here: readers treat an absent property as
	        // false, and writing from a read path would race across nodes.
	        propertySet = PropertySetManager.getInstance("ofbiz-cached", args);
		}

		return propertySet;
	}
	
	/**
	 * Method used to reference the {@link WatcherFieldSettings#getPropertySet()}
	 */
	public PropertySet getProperties() {
		return WatcherFieldSettings.getPropertySet();
	}

	/**
	 * Fail-safe read of the ignorePermissions setting for the velocity views:
	 * a tampered stored type or a storage error must not take the settings
	 * screens down (the edit form is also the only self-service repair path).
	 * @return The configured value, or false when it is absent or unreadable.
	 */
	public boolean getIgnorePermissions() {
		try {
			PropertySet propertySet = getProperties();
			return propertySet != null && propertySet.getBoolean("ignorePermissions");
		} catch (PropertyException e) {
			log.warn("Could not read the ignorePermissions setting; showing it as disabled", e);
			return false;
		}
	}
	
	/**
	 * Does the current logged in user has admin permissions
	 * @return True if has permissions, false otherwise.
	 */
    protected boolean hasAdminPermission() {
    	return globalPermissionManager.hasPermission(
    			GlobalPermissionKey.ADMINISTER,
    			authenticationContext.getLoggedInUser());
    }
}
