package it.com.burningcode.jira;

public class IntegrationTestHelper {
    public static String FIELD_TYPE_KEY = "com.burningcode.jira.issue.customfields.impl.jira-watcher-field:watcherfieldtype";
    
    public static String NUMERIC_FIELD_ID = "10201";
    public static String FIELD_ID = "customfield_" + NUMERIC_FIELD_ID;
    public static String FIELD_NAME = "My Watchers";
    public static String FIELD_TYPE = "Watcher Field";
    
    public static String PROJECT_KEY = "TST";
    
    public static void EXPORT_WITH_FIELD() {
    	EXPORT_WITH_FIELD(null);
    }
    
    public static String EXPORT_WITH_FIELD(String version) {
       	return "JWF_FieldCreated.zip";
    }
    
    public static void EXPORT_WITHOUT_FIELD() {
    	EXPORT_WITHOUT_FIELD(null);
    }
    
    public static String EXPORT_WITHOUT_FIELD(String version) {
   		return "JWF_NoFieldCreated.zip";
    }
}
