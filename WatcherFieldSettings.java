package com.burningcode.jira.plugin;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.module.propertyset.InvalidPropertyTypeException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.ActionContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.security.xsrf.DoesNotRequireXsrfCheck;

@Scanned
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class WatcherFieldSettings extends JiraWebActionSupport {

  private static PropertySet propertySet;
  
  private static final long serialVersionUID = -8378909066515942570L;
  
  private static final Logger log = LoggerFactory.getLogger(WatcherFieldSettings.class);
  
  public static final String ignoreUserPermissions = "ignorePermissions";
  
  public static final String ignoreWatcherPermissions = "ignoreWatcherPermissions";
  
  public static final String ignoreDeactivatedWatchers = "ignoreDeactivatedWatchers";
  
  public static final String hideDefaultWatcherList = "hideDefaultWatcherList";
  
  public static final String automaticallyRemoveDeactivatedWatchers = "automaticallyRemoveDeactivatedWatchers";
  
  private final JiraAuthenticationContext authenticationContext;
  
  private final GlobalPermissionManager globalPermissionManager;
  
  public WatcherFieldSettings(@ComponentImport GlobalPermissionManager globalPermissionManager, @ComponentImport JiraAuthenticationContext authenticationContext) {
    this.authenticationContext = authenticationContext;
    this.globalPermissionManager = globalPermissionManager;
  }
  
  public String doDefault() throws Exception {
    if (!hasAdminPermission())
      return "permissionviolation"; 
    return super.doDefault();
  }

  protected String doExecute() throws Exception {
    if (!hasAdminPermission())
      return "permissionviolation"; 
    return super.doExecute();
  }
  
  @DoesNotRequireXsrfCheck
  public String doEdit() {
    if (!hasAdminPermission())
      return "permissionviolation"; 
    PropertySet propertySet = getProperties();
    Map<?, ?> params = ActionContext.getParameters();
    setBooleanSetting(propertySet, params, "ignorePermissions");
    setBooleanSetting(propertySet, params, "ignoreWatcherPermissions");
    setBooleanSetting(propertySet, params, "ignoreDeactivatedWatchers");
    setBooleanSetting(propertySet, params, "hideDefaultWatcherList");
    return getRedirect("WatcherFieldSettings.jspa");
  }
  
  public void setBooleanSetting(PropertySet propertySet, Map<?, ?> params, String setting) {
    if (params.containsKey(setting) && propertySet.isSettable(setting)) {
      Object paramValue = params.get(setting);
      if (paramValue instanceof String[] && ((String[])paramValue).length == 1)
        propertySet.setBoolean(setting, Boolean.parseBoolean(((String[])paramValue)[0])); 
    } 
  }
  
  public static PropertySet getPropertySet() {
    if (propertySet == null) {
      HashMap<String, Object> args = new HashMap<>();
      args.put("delegator.name", "default");
      args.put("entityName", "WatcherFieldSettings");
      args.put("entityId", Long.valueOf(1L));
      propertySet = PropertySetManager.getInstance("ofbiz", args);
      initBooleanSetting("ignorePermissions", false);
      initBooleanSetting("ignoreWatcherPermissions", false);
      initBooleanSetting("ignoreDeactivatedWatchers", false);
      initBooleanSetting("hideDefaultWatcherList", false);
    } 
    return propertySet;
  }
  
  protected static void initBooleanSetting(String setting, boolean defaultValue) {
    try {
      if (!propertySet.exists(setting)) {
        propertySet.setBoolean(setting, defaultValue);
      } else {
        propertySet.getBoolean(setting);
      } 
    } catch (InvalidPropertyTypeException e) {
      log.debug("Property ignorePermissions set to an invalid type.  Setting to default value, " + defaultValue + ".");
      propertySet.setBoolean(setting, defaultValue);
    } catch (Exception e) {
      log.debug("Error with ignorePermissions: " + e.getMessage());
      propertySet.setBoolean(setting, defaultValue);
    } 
  }
  
  public static boolean ignoreBrowseIssuePermissions() {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("ignoreWatcherPermissions") && propertySet.getBoolean("ignoreWatcherPermissions"));
  }
  
  public static boolean isHideDefaultWatcherList() {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("hideDefaultWatcherList") && propertySet
      .getBoolean("hideDefaultWatcherList"));
  }
  
  public static boolean isIgnoreUserPermissions(ApplicationUser user) {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("ignorePermissions") && propertySet
      .getBoolean("ignorePermissions") && user == null);
  }
  
  public static boolean isIgnoreDeactivatedWatcher(@NotNull ApplicationUser user) {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("ignoreDeactivatedWatchers") && propertySet
      .getBoolean("ignoreDeactivatedWatchers") && 
      !user.isActive());
  }
  
  public static boolean isIgnoreDeactivatedWatcher() {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("ignoreDeactivatedWatchers") && propertySet
      .getBoolean("ignoreDeactivatedWatchers"));
  }
  
  @Deprecated
  public static boolean isAutomaticallyRemoveDeactivatedWatcher() {
    PropertySet propertySet = getPropertySet();
    return (propertySet.exists("automaticallyRemoveDeactivatedWatchers") && propertySet
      .getBoolean("automaticallyRemoveDeactivatedWatchers"));
  }
  
  public static void removeAutomaticallyRemoveDeactivatedWatcher() {
    PropertySet propertySet = getPropertySet();
    if (propertySet.exists("automaticallyRemoveDeactivatedWatchers"))
      propertySet.remove("automaticallyRemoveDeactivatedWatchers"); 
  }
  
  public boolean getIgnorePermissions() {
    return getPropertySet().getBoolean("ignorePermissions");
  }
  
  public boolean getIgnoreWatcherPermissions() {
    return getPropertySet().getBoolean("ignoreWatcherPermissions");
  }
  
  public boolean getIgnoreDeactivatedWatchers() {
    return getPropertySet().getBoolean("ignoreDeactivatedWatchers");
  }
  
  public boolean getHideDefaultWatcherList() {
    return getPropertySet().getBoolean("hideDefaultWatcherList");
  }
  
  @Deprecated
  public boolean getAutomaticallyRemoveDeactivatedWatchers() {
    return getPropertySet().getBoolean("automaticallyRemoveDeactivatedWatchers");
  }
  
  private PropertySet getProperties() {
    return getPropertySet();
  }
  
  public ApplicationUser getLoggedInUser() {
    return this.authenticationContext.getLoggedInUser();
  }
  
  private boolean hasAdminPermission() {
    return this.globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, getLoggedInUser());
  }
}
