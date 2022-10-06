/*     */ package com.burningcode.jira.issue.customfields.impl;
/*     */ 
/*     */ import com.atlassian.jira.bc.user.search.UserSearchService;
/*     */ import com.atlassian.jira.component.ComponentAccessor;
/*     */ import com.atlassian.jira.config.properties.ApplicationProperties;
/*     */ import com.atlassian.jira.issue.Issue;
/*     */ import com.atlassian.jira.issue.comparator.ApplicationUserBestNameComparator;
/*     */ import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
/*     */ import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
/*     */ import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
/*     */ import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
/*     */ import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
/*     */ import com.atlassian.jira.issue.fields.CustomField;
/*     */ import com.atlassian.jira.issue.fields.config.FieldConfig;
/*     */ import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
/*     */ import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
/*     */ import com.atlassian.jira.issue.fields.rest.json.UserBeanFactory;
/*     */ import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
/*     */ import com.atlassian.jira.issue.security.IssueSecurityLevel;
/*     */ import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
/*     */ import com.atlassian.jira.issue.watchers.WatcherManager;
/*     */ import com.atlassian.jira.permission.GlobalPermissionKey;
/*     */ import com.atlassian.jira.permission.ProjectPermissions;
/*     */ import com.atlassian.jira.project.Project;
/*     */ import com.atlassian.jira.project.ProjectManager;
/*     */ import com.atlassian.jira.security.GlobalPermissionManager;
/*     */ import com.atlassian.jira.security.JiraAuthenticationContext;
/*     */ import com.atlassian.jira.security.PermissionManager;
/*     */ import com.atlassian.jira.security.groups.GroupManager;
/*     */ import com.atlassian.jira.security.roles.ProjectRoleManager;
/*     */ import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
/*     */ import com.atlassian.jira.user.ApplicationUser;
/*     */ import com.atlassian.jira.user.UserFilterManager;
/*     */ import com.atlassian.jira.util.ErrorCollection;
/*     */ import com.atlassian.jira.web.FieldVisibilityManager;
/*     */ import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
/*     */ import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
/*     */ import com.atlassian.plugin.webresource.WebResourceManager;
/*     */ import com.burningcode.jira.plugin.WatcherFieldContext;
/*     */ import com.burningcode.jira.plugin.WatcherFieldSettings;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.Comparator;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.UUID;
/*     */ import javax.annotation.Nonnull;
/*     */ import org.apache.commons.lang.StringUtils;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ import webwork.action.ActionContext;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ @Scanned
/*     */ public class WatcherFieldType
/*     */   extends MultiUserCFType
/*     */ {
/*  84 */   private static final Logger log = LoggerFactory.getLogger(WatcherFieldType.class);
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private final GlobalPermissionManager globalPermissionManager;
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private final IssueSecurityLevelManager issueSecurityLevelManager;
/*     */ 
/*     */ 
/*     */   
/*     */   private final JiraAuthenticationContext authenticationContext;
/*     */ 
/*     */ 
/*     */   
/*     */   private final PermissionManager permissionManager;
/*     */ 
/*     */ 
/*     */   
/*     */   private final WatcherManager watcherManager;
/*     */ 
/*     */ 
/*     */   
/*     */   private final UserFilterManager userFilterManager;
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public WatcherFieldType(@ComponentImport CustomFieldValuePersister customFieldValuePersister, @ComponentImport GenericConfigManager genericConfigManager, @ComponentImport MultiUserConverter multiUserConverter, @ComponentImport ApplicationProperties applicationProperties, @ComponentImport JiraAuthenticationContext authenticationContext, @ComponentImport UserSearchService searchService, @ComponentImport FieldVisibilityManager fieldVisibilityManager, @ComponentImport JiraBaseUrls jiraBaseUrls, @ComponentImport UserBeanFactory userBeanFactory, @ComponentImport GroupManager groupManager, @ComponentImport ProjectRoleManager projectRoleManager, @ComponentImport SoyTemplateRendererProvider soyTemplateRendererProvider, @ComponentImport UserFilterManager userFilterManager, @ComponentImport FieldConfigSchemeManager fieldConfigSchemeManager, @ComponentImport ProjectManager projectManager, @ComponentImport GlobalPermissionManager globalPermissionManager, @ComponentImport PermissionManager permissionManager, @ComponentImport WatcherManager watcherManager, @ComponentImport IssueSecurityLevelManager issueSecurityLevelManager) {
/* 116 */     super(customFieldValuePersister, genericConfigManager, multiUserConverter, applicationProperties, authenticationContext, searchService, fieldVisibilityManager, jiraBaseUrls, userBeanFactory, groupManager, projectRoleManager, soyTemplateRendererProvider, userFilterManager, fieldConfigSchemeManager, projectManager);
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 121 */     this.userFilterManager = userFilterManager;
/* 122 */     this.authenticationContext = authenticationContext;
/* 123 */     this.globalPermissionManager = globalPermissionManager;
/* 124 */     this.permissionManager = permissionManager;
/* 125 */     this.watcherManager = watcherManager;
/* 126 */     this.issueSecurityLevelManager = issueSecurityLevelManager;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   protected void addWatchers(Issue issue, Collection<ApplicationUser> userList) {
/* 136 */     if (userList != null && isIssueEditable(issue)) {
/* 137 */       for (ApplicationUser next : userList) {
/* 138 */         ApplicationUser watcher = next;
/*     */         
/* 140 */         if (!this.watcherManager.isWatching(watcher, issue)) {
/* 141 */           issue = this.watcherManager.startWatching(watcher, issue);
/*     */         }
/*     */       } 
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void createValue(CustomField customField, Issue issue, @Nonnull Collection<ApplicationUser> value) {
/* 156 */     addWatchers(issue, value);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   protected boolean isIssueEditable(Issue issue) {
/* 167 */     return (issue != null && issue.isCreated() && this.watcherManager.isWatchingEnabled() && isUserPermitted(issue));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isJiraAdmin(ApplicationUser user) {
/* 177 */     return this.globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isUserPermitted(Issue issue) {
/* 187 */     ApplicationUser user = getLoggedInUser();
/*     */     
/* 189 */     if (WatcherFieldSettings.isIgnoreUserPermissions(user)) {
/* 190 */       return true;
/*     */     }
/*     */     
/* 193 */     if (issue == null) {
/* 194 */       log.warn("WatcherFieldType#isUserPermitted was called with a null issue object.  Should this have happened?");
/* 195 */       return false;
/*     */     } 
/*     */     
/* 198 */     Project project = issue.getProjectObject();
/* 199 */     if (project == null) {
/* 200 */       log.warn("WatcherFieldType#isUserPermitted was called with an issue object with a null project.  Should this have happened?");
/* 201 */       return false;
/*     */     } 
/*     */     
/* 204 */     return this.permissionManager.hasPermission(ProjectPermissions.MANAGE_WATCHERS, project, user);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isUserPermittedAsWatcher(ApplicationUser user, Project project) {
/* 215 */     return (project != null && this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isUserPermittedAsWatcher(ApplicationUser user, Issue issue) {
/* 226 */     return (issue == null || this.permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user));
/*     */   }
/*     */   
/*     */   public boolean hasIssueSecurityLevelPermissions(ApplicationUser user, WatcherFieldContext context) {
/* 230 */     IssueSecurityLevel issueSecurityLevel = context.getIssueSecurityLevel();
/* 231 */     if (issueSecurityLevel != null) {
/* 232 */       Issue issue = context.getIssue();
/*     */       
/* 234 */       if (issue != null) {
/* 235 */         return this.issueSecurityLevelManager.getUsersSecurityLevels(issue, user).contains(issueSecurityLevel);
/*     */       }
/*     */       
/* 238 */       return this.issueSecurityLevelManager.getUsersSecurityLevels(context.getProject(), user).contains(issueSecurityLevel);
/*     */     } 
/* 240 */     return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String getChangelogValue(CustomField field, Collection<ApplicationUser> value) {
/* 249 */     List<ApplicationUser> watcherList = (List<ApplicationUser>)value;
/*     */     
/* 251 */     if (watcherList == null || watcherList.isEmpty()) {
/* 252 */       return "None";
/*     */     }
/* 254 */     String output = "";
/*     */     
/* 256 */     for (Iterator<ApplicationUser> i = watcherList.iterator(); i.hasNext(); ) {
/* 257 */       ApplicationUser user = i.next();
/*     */ 
/*     */       
/* 260 */       if (user == null) {
/*     */         continue;
/*     */       }
/* 263 */       String displayName = user.getDisplayName();
/*     */ 
/*     */       
/* 266 */       if (displayName == null) {
/* 267 */         displayName = user.getName();
/*     */       }
/* 269 */       output = output + displayName + (i.hasNext() ? ", " : "");
/*     */     } 
/*     */     
/* 272 */     return output;
/*     */   }
/*     */   
/*     */   public ApplicationUser getLoggedInUser() {
/* 276 */     return this.authenticationContext.getLoggedInUser();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public Collection<ApplicationUser> getValueFromIssue(CustomField field, Issue issue) {
/* 287 */     if (!issue.isCreated()) {
/* 288 */       return super.getValueFromIssue(field, issue);
/*     */     }
/*     */     
/* 291 */     return getWatchers(issue);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Nonnull
/*     */   public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
/* 302 */     Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
/* 303 */     params.put("hasPermission", Boolean.valueOf(false));
/*     */     
/* 305 */     if (issue == null || issue.getProjectObject() == null) {
/* 306 */       if (isJiraAdmin(getLoggedInUser())) {
/* 307 */         params.put("hasPermission", Boolean.valueOf(true));
/*     */       }
/* 309 */     } else if (isUserPermitted(issue)) {
/* 310 */       params.put("hasPermission", Boolean.valueOf(true));
/*     */     } 
/*     */     
/* 313 */     if (WatcherFieldSettings.isHideDefaultWatcherList()) {
/* 314 */       WebResourceManager webResourceManager = ComponentAccessor.getWebResourceManager();
/* 315 */       webResourceManager.requireResource("com.burningcode.jira.issue.customfields.impl.jira-watcher-field:hide-resource");
/*     */     } 
/*     */ 
/*     */     
/* 319 */     return params;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   protected List<ApplicationUser> getWatchers(Issue issue) {
/* 330 */     List<ApplicationUser> currWatchers = new ArrayList<>(this.watcherManager.getWatchers(issue, this.authenticationContext.getLocale()));
/*     */     
/* 332 */     currWatchers.sort((Comparator<? super ApplicationUser>)new ApplicationUserBestNameComparator());
/*     */     
/* 334 */     return currWatchers;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   protected void removeWatchers(Issue issue, List<ApplicationUser> userList) {
/* 344 */     if (userList != null && isIssueEditable(issue)) {
/* 345 */       for (ApplicationUser next : userList) {
/* 346 */         ApplicationUser user = next;
/*     */         
/* 348 */         if (this.watcherManager.isWatching(user, issue)) {
/* 349 */           issue = this.watcherManager.stopWatching(user, issue);
/*     */         }
/*     */       } 
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void updateValue(CustomField customField, Issue issue, Collection<ApplicationUser> value) {
/* 365 */     List<ApplicationUser> newWatchers = (List<ApplicationUser>)value;
/* 366 */     List<ApplicationUser> currWatchers = getWatchers(issue);
/*     */ 
/*     */     
/* 369 */     if (!currWatchers.isEmpty()) {
/* 370 */       if (newWatchers != null) {
/* 371 */         currWatchers.removeAll(newWatchers);
/*     */       }
/* 373 */       removeWatchers(issue, currWatchers);
/*     */     } 
/*     */     
/* 376 */     addWatchers(issue, newWatchers);
/*     */   }
/*     */ 
/*     */   
/*     */   public void validateFromParams(CustomFieldParams relevantParams, ErrorCollection errorCollectionToAddTo, FieldConfig config) {
/* 381 */     Collection<ApplicationUser> watchers = getValueFromCustomFieldParams(relevantParams);
/* 382 */     if (watchers != null && watchers.size() > 0)
/*     */     {
/* 384 */       if (!WatcherFieldSettings.ignoreBrowseIssuePermissions()) {
/*     */ 
/*     */ 
/*     */         
/* 388 */         WatcherFieldContext context = new WatcherFieldContext(this.issueSecurityLevelManager, relevantParams, ActionContext.getParameters());
/*     */         
/* 390 */         Issue issue = context.getIssue();
/* 391 */         Project project = context.getProject();
/* 392 */         IssueSecurityLevel issueSecurityLevel = context.getIssueSecurityLevel();
/*     */         
/* 394 */         if (project != null) {
/* 395 */           boolean hasSecurityLevel = false;
/* 396 */           if (issueSecurityLevel != null) {
/* 397 */             hasSecurityLevel = true;
/*     */           }
/*     */           
/* 400 */           ArrayList<String> notPermittedUsers = new ArrayList<>();
/* 401 */           String uuid = UUID.randomUUID().toString();
/* 402 */           boolean ignoreInactive = WatcherFieldSettings.isIgnoreDeactivatedWatcher();
/* 403 */           for (ApplicationUser user : watchers) {
/* 404 */             if (ignoreInactive && !user.isActive()) {
/* 405 */               log.warn("[" + uuid + "] Ignore deactivated watchers enabled.  Allowing deactivated user '" + user.getUsername() + "' to be watcher.");
/*     */               continue;
/*     */             } 
/* 408 */             if (WatcherFieldSettings.isIgnoreDeactivatedWatcher(user)) {
/* 409 */               log.warn("[" + uuid + "] Ignore deactivated watchers enabled.  Allowing deactivated user '" + user.getUsername() + "' to be watcher.");
/*     */               
/*     */               continue;
/*     */             } 
/* 413 */             boolean projectPerm = isUserPermittedAsWatcher(user, project);
/* 414 */             boolean securityPerm = hasIssueSecurityLevelPermissions(user, context);
/* 415 */             boolean issuePerm = isUserPermittedAsWatcher(user, issue);
/*     */             
/* 417 */             if (!projectPerm) {
/* 418 */               notPermittedUsers.add(user.getName()); continue;
/*     */             } 
/* 420 */             if (hasSecurityLevel) {
/* 421 */               if (!securityPerm) {
/* 422 */                 notPermittedUsers.add(user.getName());
/*     */               }
/*     */               
/*     */               continue;
/*     */             } 
/* 427 */             if (issue != null && project.getId().equals(issue.getProjectId()) && 
/* 428 */               !issuePerm) {
/* 429 */               notPermittedUsers.add(user.getName());
/*     */             }
/*     */           } 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 436 */           if (notPermittedUsers.size() > 0)
/* 437 */             errorCollectionToAddTo.addError(config.getFieldId(), "Users do not have permission to view this issue: " + StringUtils.join(notPermittedUsers, ", "), ErrorCollection.Reason.FORBIDDEN); 
/*     */         } 
/*     */       } else {
/* 440 */         log.debug("'Ignore browse issue permissions' enabled");
/*     */       } 
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean valuesEqual(Collection<ApplicationUser> v1, Collection<ApplicationUser> v2) {
/* 451 */     ArrayList<ApplicationUser> watcherList1 = (v1 != null) ? (ArrayList<ApplicationUser>)v1 : new ArrayList<>();
/* 452 */     ArrayList<ApplicationUser> watcherList2 = (v2 != null) ? (ArrayList<ApplicationUser>)v2 : new ArrayList<>();
/* 453 */     watcherList1.sort((Comparator<? super ApplicationUser>)new ApplicationUserBestNameComparator());
/* 454 */     watcherList2.sort((Comparator<? super ApplicationUser>)new ApplicationUserBestNameComparator());
/*     */     
/* 456 */     return watcherList1.equals(watcherList2);
/*     */   }
/*     */ }


/* Location:              /home/davidyu/jira-watcher-field-2.8.5.jar!/com/burningcode/jira/issue/customfields/impl/WatcherFieldType.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */