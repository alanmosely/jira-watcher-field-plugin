package it.com.burningcode.jira.rest;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.oro.text.regex.MalformedPatternException;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.atlassian.jira.functest.framework.util.url.URLUtil;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.RestFuncTest;
import com.meterware.httpunit.WebResponse;

import static it.com.burningcode.jira.IntegrationTestHelper.*;

public class IntegrationTestWatcherResource extends RestFuncTest {
    @Override
    public void setUpTest() {
    	String jiraVersion = (new BuildUtilsInfoImpl()).getVersion();
        administration.restoreData(EXPORT_WITH_FIELD(jiraVersion));
        super.setUpTest();
    }

    @Test
    public void testRestGetWatchers() throws MalformedPatternException, IOException, SAXException, JSONException{
    	log.log("### Test get watchers rest call ###");
    	
    	String issueKey = navigation.issue().createIssue("Test", ISSUE_TYPE_BUG, "Test get watchers REST call.");

    	// Check that the REST call sets the watcher field when toggling watcher.
    	tester.assertTextNotInElement(FIELD_ID + "-val", ADMIN_FULLNAME);
    	tester.clickLink("watching-toggle");
    	tester.assertTextInElement(FIELD_ID + "-val", ADMIN_FULLNAME);

    	String issueId = navigation.issue().getId(issueKey);
    	String url = URLUtil.addXsrfToken(page.getFreshXsrfToken(), "/rest/watcherfield/latest/watchers");
    	url += "&issueId=" + issueId;
    	
    	log.log("Using issue id: " + issueId);
    	log.log("Using atl_token: " + page.getXsrfToken());
    	log.log("Generated URL " + url);
    	
    	WebResponse response = GET(url);
    	
    	// Verify the request was successful
    	assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());
    	
    	JSONObject jsonResponse = new JSONObject(response.getText());

    	// Verify the response contains the fieldId's
    	assertTrue(jsonResponse.has("fieldIds"));
    	assertEquals(FIELD_ID, jsonResponse.getJSONArray("fieldIds").get(0));
    	
    	// Verify the response contains the issueId
    	assertTrue(jsonResponse.has("issueId"));
    	assertEquals(issueId, jsonResponse.getString("issueId"));
    	
    	// Verify the response contains the watchers.
    	assertTrue(jsonResponse.has("watchers"));
    	JSONObject actualWatchers = (JSONObject)jsonResponse.getJSONArray("watchers").get(0);
    	assertEquals(ADMIN_USERNAME, actualWatchers.get("username"));
    	assertEquals(ADMIN_FULLNAME, actualWatchers.get("displayName"));
    }
}
