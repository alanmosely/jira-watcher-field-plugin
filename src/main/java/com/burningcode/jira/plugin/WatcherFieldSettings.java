package com.burningcode.jira.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import webwork.action.ActionContext;

import com.opensymphony.module.propertyset.InvalidPropertyTypeException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Used to handle settings for the JIRA Watcher Field.
 * @author Ray
 *
 */
public class WatcherFieldSettings extends JiraWebActionSupport {
	private static PropertySet propertySet;
	private static final long serialVersionUID = -8378909066515942570L;
	private static final Logger log = LoggerFactory.getLogger(WatcherFieldSettings.class);
	
	public static final String ignoreUserPermissions = "ignorePermissions";
	public static final String ignoreWatcherPermissions = "ignoreWatcherPermissions";

	private PermissionManager permissionManager;
	private JiraAuthenticationContext authenticationContext;

	/**
	 * Default Constructor
	 * @param permissionManager
	 * @param authenticationContext
	 */
	public WatcherFieldSettings(PermissionManager permissionManager, JiraAuthenticationContext authenticationContext) {
		this.permissionManager = permissionManager;
		this.authenticationContext = authenticationContext;
	}

    public static boolean ignoreBrowseIssuePermissions(){
    	PropertySet propertySet = WatcherFieldSettings.getPropertySet();
    	return propertySet.exists(ignoreWatcherPermissions) && propertySet.getBoolean(ignoreWatcherPermissions);
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
	public String doEdit() throws Exception {
		if(!hasAdminPermission())
			return PERMISSION_VIOLATION_RESULT;
		
		PropertySet propertySet = getProperties();

		Map<?, ?> params = ActionContext.getParameters();
		
		if(params.containsKey(ignoreUserPermissions) && propertySet.isSettable(ignoreUserPermissions)) {
			Object value = params.get(ignoreUserPermissions);
			if(value instanceof String[] && ((String[])value).length == 1) {
				propertySet.setBoolean(ignoreUserPermissions, Boolean.parseBoolean(((String[])value)[0]));
			}
		}
		
		if(params.containsKey(ignoreWatcherPermissions) && propertySet.isSettable(ignoreWatcherPermissions)) {
			Object value = params.get(ignoreWatcherPermissions);
			if(value instanceof String[] && ((String[])value).length == 1) {
				propertySet.setBoolean(ignoreWatcherPermissions, Boolean.parseBoolean(((String[])value)[0]));
			}
		}
		return getRedirect("WatcherFieldSettings.jspa");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setErrorMessages(@SuppressWarnings("rawtypes") Collection arg0) {
		super.setErrorMessages(arg0);

	}

	/**
	 * Static method that returns the PropertySet used to get/store settings in the database
	 * @return The PropertySet to reference the data
	 */
	public static PropertySet getPropertySet() {
		if(propertySet == null) {
			HashMap<String, Object> args = new HashMap<String, Object>();
	        args.put("delegator.name", "default");
	        args.put("entityName", "WatcherFieldSettings");
	        args.put("entityId", new Long(1));

	        propertySet = PropertySetManager.getInstance("ofbiz", args);

	        try{
		        // Set default settings
				if(!propertySet.exists(ignoreUserPermissions)) {
					propertySet.setBoolean(ignoreUserPermissions, false);
				}else{
		        	// Will throw an exception if of invalid type
		        	propertySet.getBoolean(ignoreUserPermissions);
				}
		    }catch (InvalidPropertyTypeException e) {
		    	log.debug("Property ignorePermissions set to an invalid type.  Setting to default value, false.");
		    	propertySet.setBoolean(ignoreUserPermissions, false);
			}catch (Exception e) {
				log.debug("Error with ignorePermissions: "+e.getMessage());
				propertySet.setBoolean(ignoreUserPermissions, false);
			}
	        
	        try{
		        // Set default settings
				if(!propertySet.exists(ignoreWatcherPermissions)) {
					propertySet.setBoolean(ignoreWatcherPermissions, false);
				}else{
		        	// Will throw an exception if of invalid type
		        	propertySet.getBoolean(ignoreWatcherPermissions);
				}
		    }catch (InvalidPropertyTypeException e) {
		    	log.debug("Property ignoreWatcherPermissions set to an invalid type.  Setting to default value, false.");
		    	propertySet.setBoolean(ignoreWatcherPermissions, false);
			}catch (Exception e) {
				log.debug("Error with ignoreWatcherPermissions: "+e.getMessage());
				propertySet.setBoolean(ignoreWatcherPermissions, false);
			}
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
	 * Does the current logged in user has admin permissions
	 * @return True if has permissions, false otherwise.
	 */
    protected boolean hasAdminPermission() {
    	return permissionManager.hasPermission(
    			Permissions.ADMINISTER,
    			authenticationContext.getLoggedInUser());
    }
}
