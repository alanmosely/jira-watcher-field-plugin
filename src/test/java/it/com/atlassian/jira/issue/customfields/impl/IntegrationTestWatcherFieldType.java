package it.com.atlassian.jira.issue.customfields.impl;

import com.atlassian.jira.webtests.JIRAWebTest;

/**
 * @author Ray
 *
 */
public class IntegrationTestWatcherFieldType extends JIRAWebTest {
    /**
     * @param name
     */
    public IntegrationTestWatcherFieldType(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();
        restoreData("JWF_NoFieldCreated.xml");
        //restoreBlankInstance();
    }
    
    /**
     * 
     */
    public void testDisableAndEnable() {
        disablePlugin("JIRA Watcher Field","");
        enablePlugin("JIRA Watcher Field", "");
    }

    /**
     * 
     */
    public void testAddCustomField() {
        //gotoPage(url)
    }
}
