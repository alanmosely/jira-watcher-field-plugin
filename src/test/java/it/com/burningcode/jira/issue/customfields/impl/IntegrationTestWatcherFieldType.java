package it.com.burningcode.jira.issue.customfields.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.atlassian.jira.functest.framework.admin.ViewServices.UnableToAddServiceException;
import com.atlassian.jira.functest.framework.navigator.ContainsIssueKeysCondition;
import com.atlassian.jira.functest.framework.navigator.GenericQueryCondition;
import com.atlassian.jira.functest.framework.navigator.NavigatorSearch;
import com.atlassian.jira.functest.framework.navigator.SearchResultsCondition;
import com.atlassian.jira.webtests.EmailFuncTestCase;
import com.atlassian.jira.webtests.JIRAServerSetup;
import com.atlassian.jira.webtests.ztests.workflow.ExpectedChangeHistoryItem;
import com.atlassian.jira.webtests.ztests.workflow.ExpectedChangeHistoryRecord;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.meterware.httpunit.WebForm;

import static it.com.burningcode.jira.IntegrationTestHelper.*;

/**
 * Class used for integration testing for the JIRA Watcher Field Plugin.
 * 
 * TODO Get integration testing on bulk change.
 * TODO Check issue security
 * TODO Write test to check for issue JWFP-9
 * @author Ray Barham
 */
public class IntegrationTestWatcherFieldType extends EmailFuncTestCase {
    
    protected void assertWatchersNotPresent(String issueKey, String[] watchers){
    	String currentPage = navigation.getCurrentPage();
    	navigation.issue().gotoIssue(issueKey);
    	
    	tester.clickLink("view-watcher-list");
    	for(String watcher : watchers){
    		tester.assertLinkNotPresent("watcher_link_" + watcher);
    		log.log("Successfully found user " + watcher + " is not a watcher on issue " + issueKey);
    	}
    	navigation.gotoPage(currentPage);
    }
    
    protected void assertWatchersPresent(String issueKey, String[] watchers){
    	String currentPage = navigation.getCurrentPage();
    	navigation.issue().gotoIssue(issueKey);
    	
    	tester.clickLink("view-watcher-list");
    	for(String watcher : watchers){
    		tester.assertLinkPresent("watcher_link_" + watcher);
    		log.log("Successfully found user " + watcher + " is a watcher on issue " + issueKey);
    	}
    	navigation.gotoPage(currentPage);
    }
    
    protected HashMap<String, String[]> getUsernameFieldMap(String[] usernames) {
    	HashMap<String, String[]> params = new HashMap<String, String[]>();
    	params.put(FIELD_ID, usernames);
    	return params;
    }

    @Before
    public void setUpTest() {
        administration.restoreData("JWF_FieldCreated.xml");
    }

    protected WebForm setWatcherFieldForm(WebForm[] forms, String fieldId, String values){
    	return setWatcherFieldForm(forms, fieldId, values, null);
    }
    
    protected WebForm setWatcherFieldForm(WebForm[] forms, String fieldId, String values, String expectedExistingValues){
    	// Loop through the forms till you one w/ the watcher field is found 
    	for(WebForm form : forms){	
    		if(form.hasParameterNamed(fieldId)){
    			if(expectedExistingValues != null){
    				tester.assertFormElementEquals(fieldId, expectedExistingValues);
    			}
    			form.setParameter(fieldId, values);
    			return form;
    		}
    	}
    	fail("No form found with watcher field ID "+ fieldId);

    	return null;
    }
    
    /**
     * Test adding a watcher field to JIRA.
     */
    @Test
    public void testCreateWatcherField() {
    	log.log("### Test creating watcher field ###");
    	
    	administration.restoreData("JWF_NoFieldCreated.xml");
    	
    	navigation.gotoCustomFields();

    	tester.assertTextNotInTable("custom-fields", FIELD_NAME);
    	tester.assertTextNotInTable("custom-fields", FIELD_TYPE);
        administration.customFields().addCustomField(FIELD_TYPE_KEY, FIELD_NAME);
        tester.assertTextInTable("custom-fields", FIELD_NAME);
        tester.assertTextInTable("custom-fields", FIELD_TYPE);
    }
    
    /**
     * Test deleting a watcher field from JIRA.
     */
    public void testDeleteWatcherField() {
    	log.log("### Test delete watcher field ###");
    	
    	navigation.gotoCustomFields();
    	
    	tester.assertTextInTable("custom-fields", FIELD_NAME);
    	tester.assertTextInTable("custom-fields", FIELD_TYPE);
    	administration.customFields().removeCustomField(FIELD_ID);
    	tester.assertTextNotInTable("custom-fields", FIELD_NAME);
    	tester.assertTextNotInTable("custom-fields", FIELD_TYPE);
    }
    
    /**
     * Test adding watchers via the watcher field on issue create.
     */
    public void testAddWatcherOnIssueCreate() {
    	log.log("### Test adding watcher on issue create ###");
    	
    	HashMap<String, String[]> params = getUsernameFieldMap(new String[]{BOB_USERNAME});
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test add watchers on issue create", params);
    	navigation.issue().gotoIssue(issueKey);
    	tester.assertTextPresent(FIELD_NAME);
    	assertWatchersPresent(issueKey, new String[]{BOB_USERNAME});
    }
    
    /**
     * Test adding watchers via the watcher field on issue edit.
     * @throws SAXException 
     * @throws IOException 
     */
    public void testAddWatcherOnIssueEdit() throws IOException, SAXException {
    	log.log("### Test adding watcher on issue edit ###");
    	
    	String[] usernames = new String[]{ADMIN_USERNAME, BOB_USERNAME};

    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test add watchers on issue edit.");
    	assertWatchersNotPresent(issueKey, usernames);

    	
    	navigation.issue().gotoEditIssue(issueKey);
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, ADMIN_USERNAME + ", " + BOB_USERNAME).submit();

    	assertWatchersPresent(issueKey, usernames);
    }
    
    /**
     * Test modifying watchers via the watcher field.
     * @throws SAXException 
     * @throws IOException 
     */
    public void testModifyingWatcherOnIssueEdit() throws IOException, SAXException {
    	log.log("### Test modify watcher on issue edit ###");
    	
    	String[] usernames = new String[]{ADMIN_USERNAME, BOB_USERNAME};

    	HashMap<String, String[]> params = getUsernameFieldMap(usernames);
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test modify watchers on issue edit", params);
    	
    	assertWatchersPresent(issueKey, usernames);
    	
    	navigation.issue().gotoEditIssue(issueKey);
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, usernames[0], usernames[0] + ", " + usernames[1]).submit();
    	
    	assertWatchersPresent(issueKey, new String[]{usernames[0]});
    	assertWatchersNotPresent(issueKey, new String[]{usernames[1]});
    }
    
    /**
     * Test configuring the watcher field.
     */
    public void testConfigureWatcherField() {
    	log.log("### Test configure watcher field ###");
    	
    	String[] usernames = new String[]{ADMIN_USERNAME, BOB_USERNAME};

    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test default watchers without configuration.");
    	assertWatchersNotPresent(issueKey, usernames);
    	
    	// Set the default value for the watcher field
    	administration.customFields().setDefaultValue(NUMERIC_FIELD_ID, usernames[1]);
    	
    	issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test default watchers with configuration.");
    	assertWatchersNotPresent(issueKey, new String[]{usernames[0]});
    	assertWatchersPresent(issueKey, new String[]{usernames[1]});
    }
    
    /**
     * Checks simple filter/searching using the watcher field.  Also checks that issues are being re-indexed on adding watchers (otherwise, searches would not work).
     */
    public void testSimpleFilterByWatcher() {
    	log.log("### Test simple filter by watcher ###");
    	
    	String[] usernames = new String[]{BOB_USERNAME};

    	HashMap<String, String[]> params = getUsernameFieldMap(usernames);
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test simple filter by watcher", params);
    	assertWatchersPresent(issueKey, usernames);
    	
    	GenericQueryCondition watcherCondition = new GenericQueryCondition(FIELD_ID);
    	watcherCondition.setQuery(usernames[0]);
    	NavigatorSearch search = new NavigatorSearch(watcherCondition);
    	
    	navigation.issueNavigator().createSearch(search);
    	tester.submit();
    	
    	ArrayList<SearchResultsCondition> searchResultsConditions = new ArrayList<SearchResultsCondition>();
    	searchResultsConditions.add(new ContainsIssueKeysCondition(text, issueKey));
    	assertions.getIssueNavigatorAssertions().assertSearchResults(searchResultsConditions);
    }
    
    /**
     * Checks sql filter/searching using the watcher field
     */
    public void testJqlFilterByWatcher() {
    	log.log("### Test jql filter by watcher ###");
    	
    	String[] usernames = new String[]{BOB_USERNAME};
    	
    	HashMap<String, String[]> params = getUsernameFieldMap(usernames);
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test jql filter by watcher", params);
    	assertWatchersPresent(issueKey, usernames);
    	
    	navigation.issueNavigator().createSearch("\"My Watchers\" = " + usernames[0]);
    	tester.submit();
    	
    	assertions.getIssueNavigatorAssertions().assertNoJqlErrors();

    	ArrayList<SearchResultsCondition> searchResultsConditions = new ArrayList<SearchResultsCondition>();
    	searchResultsConditions.add(new ContainsIssueKeysCondition(text, issueKey));
    	assertions.getIssueNavigatorAssertions().assertSearchResults(searchResultsConditions);
    }
    
    /**
     * Checks that change history is effected properly.  See issue JWF-5.
     * @throws SAXException 
     * @throws IOException 
     */
	public void testChangeHistory() throws IOException, SAXException {
    	log.log("### Test change history ###");
    	
		String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test change history without watchers specified.");
		navigation.issue().gotoIssueChangeHistory(issueKey);
		
		// Verify no change history for the watcher field is added on issue create.
		tester.assertTextPresent("No changes have yet been made on this issue.");
		
		navigation.issue().gotoEditIssue(issueKey);
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, BOB_USERNAME + ", " + ADMIN_USERNAME).submit();

    	log.log(ADMIN_USERNAME);
    	log.log(ADMIN_FULLNAME);
    	log.log(ADMIN_PASSWORD);
    	log.log(ADMIN_USERNAME);
    	
		ArrayList<ExpectedChangeHistoryItem> expectedChangeItems = new ArrayList<ExpectedChangeHistoryItem>();
		expectedChangeItems.add(new ExpectedChangeHistoryItem(FIELD_NAME, "None", ADMIN_FULLNAME + ", " + BOB_FULLNAME));
		ExpectedChangeHistoryRecord changeHistoryRecord = new ExpectedChangeHistoryRecord(expectedChangeItems);
		
		// Verify change history when adding watchers
		assertions.assertLastChangeHistoryRecords(issueKey, changeHistoryRecord);
		
		navigation.issue().gotoEditIssue(issueKey);
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, ADMIN_USERNAME).submit();
    	
    	// Verify change history when changing watchers
    	expectedChangeItems.set(0, new ExpectedChangeHistoryItem(FIELD_NAME, ADMIN_FULLNAME + ", " + BOB_FULLNAME, ADMIN_FULLNAME));
    	changeHistoryRecord = new ExpectedChangeHistoryRecord(expectedChangeItems);
    	assertions.assertLastChangeHistoryRecords(issueKey, changeHistoryRecord);
    	
		navigation.issue().gotoEditIssue(issueKey);
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, "").submit();
    	
    	// Verify change history when clearing watchers
    	expectedChangeItems.set(0, new ExpectedChangeHistoryItem(FIELD_NAME, ADMIN_FULLNAME, "None"));
    	changeHistoryRecord = new ExpectedChangeHistoryRecord(expectedChangeItems);
    	assertions.assertLastChangeHistoryRecords(issueKey, changeHistoryRecord);
    }
    
    /**
     * Checks that watchers are edited properly on issue transition.  See issue JWF-4.
     * @throws SAXException 
     * @throws IOException 
     */
    public void testEditWatcherOnIssueTransition() throws IOException, SAXException {
    	log.log("### Test edit watcher on issue transition ###");
    	
    	// Add the watcher field to the resolve workflow screen
    	administration.viewFieldScreens().goTo();
    	administration.viewFieldScreens().configureScreen("Workflow Screen");
    	tester.selectOption("fieldId", FIELD_NAME);
    	tester.submit("Add");
    	
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test edit watchers on issue transition.");
    	navigation.issue().closeIssue(issueKey, "Fixed", null);
    	tester.clickLinkWithText("Reopen Issue");
    	setWatcherFieldForm(this.form.getForms(), FIELD_ID, BOB_USERNAME + ", " + ADMIN_USERNAME).submit();
    	
    	// Check that the watchers were successfully added
    	assertWatchersPresent(issueKey, new String[]{BOB_USERNAME, ADMIN_USERNAME});
    }
    
    /**
     * Verifies that JWFP-13 is resolved
     * 
     * @throws InterruptedException
     * @throws MessagingException
     * @throws UnableToAddServiceException 
     */
	public void testCreateIssueViaEmail() throws InterruptedException, MessagingException, UnableToAddServiceException {
    	log.log("### Test add watchers on create issue via email ###");
    	
    	// Set the default user for the watcher field
    	administration.customFields().setDefaultValue(NUMERIC_FIELD_ID, BOB_USERNAME);
    	
		assertSendingMailIsEnabled();

		JIRAServerSetup.POP3.setPort(110);
		GreenMail greenMail = configureAndStartGreenMail(JIRAServerSetup.ALL);
		greenMail.setUser(ADMIN_EMAIL, ADMIN_USERNAME, ADMIN_PASSWORD);
		
		assertTrue(greenMail.getPop3().isAlive());
		assertTrue(greenMail.getSmtp().isAlive());
		assertTrue(greenMail.getImap().isAlive());
		
		// Setup the mail server in JIRA
		setupJiraImapPopServer();
		setupJiraMailServer(ADMIN_EMAIL, DEFAULT_SUBJECT_PREFIX, String.valueOf(greenMail.getSmtp().getPort()));

		// Add service to create issues from POP server
		setupPopService("project=" + PROJECT_KEY + ", issuetype=" + ISSUE_BUG);

        String subject = "This is created by email";
        String message = "This is the subject.  It is a test subject.";
        
        // Send the message
        GreenMailUtil.sendTextEmail(ADMIN_EMAIL, ADMIN_EMAIL, subject, message, greenMail.getSmtp().getServerSetup());
		
        // Keep the mail server up long enough for the JIRA POP service to connect to
        greenMail.waitForIncomingEmail(65000, 10);

        navigation.issue().gotoIssue(PROJECT_KEY + "-1");
//        if(!greenMail.waitForIncomingEmail(1))
//    	  fail("No email messages found");
    }
    
    /*
    public void testBulkEditWatchers() {
        Vector issueList = new Vector();
        issueList.add("TST-1");
        issueList.add("TST-2");
        
        // Bulk edit the watchers on some issues
        displayAllIssues();
        bulkChangeIncludeAllPages();
        bulkChangeSelectIssues(issueList);
        bulkChangeChooseOperationEdit();
        assertFormElementPresent("cbcustomfield_10000");
        selectCheckbox("cbcustomfield_10000");
        //setBulkEditFieldTo("customfield_"+FIELD_ID, "cbcustomfield_"+FIELD_ID);
        //bulkEditOperationDetailsSetAs(easyMapBuild("customfield_"+FIELD_ID, "bob"));
        clickOnNext();
        dumpScreen("screenDump");

        // Check that the users were actually added as watchers
        gotoIssue("TST-1");
        clickLink("view_watchers");
        assertFormElementNotPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");
        gotoIssue("TST-2");
        clickLink("view_watchers");
        assertFormElementNotPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");

        // Bulk edit the watchers on some issues
        displayAllIssues();
        clickLink("bulkedit_all");
        bulkChangeSelectIssues(issueList);
        bulkChangeChooseOperationEdit();
        setFormElement("customfield_"+FIELD_ID, "admin");
        clickButton("Next");
        clickButtonWithValue("Confirm");

        // Check that the users were actually added as watchers
        gotoIssue("TST-1");
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementNotPresent("stopwatch_bob");
        gotoIssue("TST-2");
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementNotPresent("stopwatch_bob");
        
        // Bulk edit the multiple watchers on some issues
        displayAllIssues();
        clickLink("bulkedit_all");
        bulkChangeSelectIssues(issueList);
        bulkChangeChooseOperationEdit();
        setFormElement("customfield_"+FIELD_ID, "admin, bob");
        clickButton("Next");
        clickButtonWithValue("Confirm");
        
        // Check that the users were actually added as watchers
        gotoIssue("TST-1");
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");
        gotoIssue("TST-2");
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");
    }
    */
}
