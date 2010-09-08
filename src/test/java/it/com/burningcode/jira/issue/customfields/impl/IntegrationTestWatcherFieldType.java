package it.com.burningcode.jira.issue.customfields.impl;

import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;

import com.atlassian.jira.webtests.JIRAWebTest;
import com.burningcode.jira.rest.Watcher;
import com.burningcode.jira.rest.Watchers;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Class used for integration testing for the JIRA Watcher Field Plugin.
 * 
 * TODO Get integration testing on bulk change.
 * TODO Check issue security
 * TODO Write test to check for issue JWFP-9
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
        clickButton("update_submit");
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
        clickLink("editIssue");
        setFormElement("customfield_"+FIELD_ID, "admin, bob");
        clickButton("update_submit");
        
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
        clickButton("update_submit");
        
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
        clickButton("update_submit");
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
        clickButtonWithValue("Start Progress");
        
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
        clickButtonWithValue("Start Progress");
        
        // Check that the users were actually added as watchers
        clickLink("view_watchers");
        assertFormElementNotPresent("stopwatch_admin");
        assertFormElementNotPresent("stopwatch_bob");

    }
    
    public void testRestGetWatchers() throws MalformedPatternException{
    	gotoIssue("TST-1");
    	clickLink("view_watchers");
    	clickLinkWithText("Watch this issue");
    	assertFormElementPresent("stopwatch_admin");
    	
    	String issueId = getIssueIdWithIssueKey("TST-1");
    	String atlToken = page.getXsrfToken();
    	
    	log("Issue ID: " + issueId);
    	log("Atl Token: " + atlToken);
    	
    	Client client = Client.create();
    	String url = environmentData.getBaseUrl().toExternalForm() + "/rest/watcherfield/latest/watchers?atl_token=" + atlToken + "&issueId=" + issueId;
    	
    	log("Url Used: " + url);
    	
    	WebResource resource = client.resource(url);
    	Watchers watchers = resource.get(Watchers.class);
    	List<Watcher> watcherList = watchers.getWatchers();
    	
    	// Check that the watcherList returned contains watchers.
    	assert watcherList.size() > 0;

    	boolean isWatcherFound = false;
    	for(Watcher watcher : watcherList){
    		log("Found Watcher: " + watcher.getUsername());
    		if(watcher.getUsername().compareToIgnoreCase("admin") == 0){
    			isWatcherFound = true;
    			break;
    		}
    	}
    	assertTrue(isWatcherFound);
    }
    
    /**
     * Verifies that JWFP-13 is resolved
     * 
     * @throws InterruptedException
     * @throws MessagingException
     */
    /* TODO Create test to test resolve issue JWFP-13
	public void testCreateIssueViaEmail() throws InterruptedException, MessagingException {
        log("Starting greenmail server");
        GreenMail greenMail = new GreenMail();
        greenMail.start();
        
        log("Checking that the SMTP server started.");
        assertTrue(greenMail.getSmtp().isAlive());
        
        String subject = "This is created by email";
        String message = 
        	"project=TST\n" +  
        	"issuetype=1\n" +
        	"This is the subject.  It is a test subject.";

        GreenMailUtil.sendTextEmail("jira-reply@localhost.com", ADMIN_EMAIL, subject, message, greenMail.getSmtp().getServerSetup());

        boolean isFound = false;
        if(greenMail.waitForIncomingEmail(120000, 1)) {
            for(MimeMessage mimeMessage : greenMail.getReceivedMessages()){
            	if(mimeMessage.getSubject().compareToIgnoreCase(subject) == 0){
            		isFound = true;
            		break;
            	}
            }
        }else{
            log("No messages found");
        }
        
        assertTrue(isFound);
        
        String key = getIssueKeyWithSummary(subject, "TST");
        log(key);
      
        greenMail.stop();
    }*/
    
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
