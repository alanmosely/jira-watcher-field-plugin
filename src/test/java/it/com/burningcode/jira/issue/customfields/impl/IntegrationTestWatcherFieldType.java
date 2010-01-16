package it.com.burningcode.jira.issue.customfields.impl;

import com.atlassian.jira.webtests.JIRAWebTest;

/**
 * Class used for integration testing for the JIRA Watcher Field Plugin.
 * 
 * TODO Get integration testing on bulk change.
 * TODO Check issue security
 * @author Ray Barham
 */
public class IntegrationTestWatcherFieldType extends JIRAWebTest {
    private static String FIELD_TYPE_KEY = "com.burningcode.jira.issue.customfields.impl.jira-watcher-field:watcherfieldtype";
	//private static String FIELD_TYPE_SEARCH_KEY = "com.burningcode.jira.issue.customfields.impl.jira-watcher-field:watcherfieldsearcher";
    private static String FIELD_ID = "10000";
    
    /**
     * {@inheritDoc}
     */
    public IntegrationTestWatcherFieldType(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setUp() {
        super.setUp();
        restoreData("JWF_FieldCreated.xml");
    }
    
    /**
     * Test adding a watcher field to JIRA.
     */
    public void testAddWatcherField() {
        restoreData("JWF_NoFieldCreated.xml");
        
        gotoPage("CreateCustomField!default.jspa");
        selectMultiOptionByValue("fieldType", FIELD_TYPE_KEY);
        clickButton("nextButton");
        setFormElement("fieldName", "Watchers");
        clickButton("nextButton");
        clickButton("Update");
        assertEquals(true, customFieldExists(FIELD_ID));
    }
    
    /**
     * Test deleting a watcher field from JIRA.
     */
    public void testDeleteWatcherField() {
    	assertEquals(true, customFieldExists("My Watchers"));
        deleteCustomField(FIELD_ID);
        assertEquals(false, customFieldExists("My Watchers")); 
    }
    
    /**
     * Test adding watchers via the watcher field on issue create.
     */
    public void testAddWatcherOnIssueCreate() {
        // Create a new issue with the new watchers
        createIssueStep1("Test", "Bug");
        setFormElement("summary", "Test Issue");
        setFormElement("customfield_"+FIELD_ID, "admin");
        submit();

        // Check that the watchers field is shown on view
        assertTextPresent("My Watchers:");

        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
    }
    
    /**
     * Test adding watchers via the watcher field on issue edit.
     */
    public void testAddWatcherOnIssueEdit() {
        // Add new watchers via edit.
        gotoIssue("TST-1");
        clickLink("edit_issue");
        setFormElement("customfield_"+FIELD_ID, "admin, bob");
        clickButton("Update");
        
        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");
    }
    
    /**
     * Test modifying watchers via the watcher field.
     */
    public void testModifyingWatcherOnIssueEdit() {
        gotoIssue("TST-2");
        
        // Check that the watchers are present for the issue
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_bob");
        assertFormElementNotPresent("stopwatch_admin");
        
        // Modify the watchers
        gotoPage("EditIssue!default.jspa?id=10010");
        assertTextInElement("customfield_"+FIELD_ID, "bob");
        setFormElement("customfield_"+FIELD_ID, "admin");
        clickButton("Update");
        
        // Check for the updated watchers
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementNotPresent("stopwatch_bob");
    }
    
    /**
     * Test configuring the watcher field.
     */
    public void testConfigureWatcherField() {
        gotoPage("ConfigureCustomField!default.jspa?customFieldId="+FIELD_ID);
        
        // Check adding a default watcher
        configureDefaultCustomFieldValue(FIELD_ID, "bob");
        assertLinkWithTextExists("Bob");
        
        // Create a new issue with the default watchers
        createIssueStep1("Test", "Bug");
        setFormElement("summary", "Test Issue");
        submit();

        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_bob");
    }
    
    /**
     * Checks filters/searching using the watcher field.  Also checks that issues are being re-indexed on adding watchers (otherwise, searches would not work).
     */
    public void testFiltersByWatcher() {
        // Check that existing filters are correct
        gotoFilter("Watchers - Admin");
        assertTextPresent("No matching issues found.");
        gotoFilter("Watchers - Bob");
        assertTextPresent("TST-2");
        
        // Edit watchers via watcher field.
        testModifyingWatcherOnIssueEdit();
        
        // Check filters for change in watchers
        gotoFilter("Watchers - Admin");
        assertTextPresent("TST-2");
        gotoFilter("Watchers - Bob");
        assertTextPresent("No matching issues found.");
    }
    
    /**
     * Checks that change history is effected properly.  See issue JWF-5.
     */
    @SuppressWarnings("deprecation")
	public void testChangeHistory() {
        // Check change history when editing watchers
        testAddWatcherOnIssueEdit();
        assertLastChangeHistoryIs("TST-1", "Watchers", "None", "Admin, Bob");
        
        // Check change history doesn't change when not editing watchers
        gotoPage("EditIssue!default.jspa?id=10000");
        setFormElement("description", "HAZZAH!");
        clickButton("Update");
        assertTextPresent("HAZZAH!");
        assertLastChangeNotMadeToField("TST-1", "Watchers");
    }
    
    /**
     * Checks that watchers are edited properly on issue transition.  See issue JWF-4.
     */
    public void testEditWatcherOnIssueTransition() {
        gotoIssue("TST-1");
        
        // Execute the transition.
        clickLinkWithText("Start Progress");
        setFormElement("customfield_"+FIELD_ID, "admin, bob");
        clickButton("Start Progress");
        
        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementPresent("stopwatch_admin");
        assertFormElementPresent("stopwatch_bob");
        
        // Return back to the issue
        gotoIssue("TST-1");
        clickLinkWithText("Stop Progress");
        
        // Execute the transition.
        clickLinkWithText("Start Progress");
        setFormElement("customfield_"+FIELD_ID, "");
        clickButton("Start Progress");
        
        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementNotPresent("stopwatch_admin");
        assertFormElementNotPresent("stopwatch_bob");

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
