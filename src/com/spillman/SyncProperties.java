package com.spillman;

import java.util.HashMap;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.axis.encoding.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class SyncProperties extends Properties {
	private static final Logger logger = LogManager.getLogger();
	
	// Private constants
	private static final String PROPERTIES_FILE_NAME = "sync.properties";
	private static final String DEFAULT_TIME_TO_SLEEP 			= "10000";

	private static final String PROP_CRM_JDBC_CONNECTION_STRING	= "CRM.JDBC_ConnectionString";

	private static final String PROP_DEFAULT_VERSION			= "DefaultVersion";

	private static final String PROP_JIRA_BROWSE_URL			= "Jira.BrowseURL";
	private static final String PROP_JIRA_CREATE_PROJECT_URL	= "Jira.CreateProjectUrl";
	private static final String PROP_JIRA_JDBC_CONNECTION_STRING= "Jira.JDBC_ConnectionString";
	private static final String PROP_JIRA_PASSWORD				= "Jira.Password";
	private static final String PROP_JIRA_TASK_TEMPLATE			= "Workfront.JiraTaskTemplate";
	private static final String PROP_JIRA_USERNAME				= "Jira.Username";

	private static final String PROP_TIME_TO_SLEEP 				= "TimeToSleep";
	
	private static final String PROP_WORKFRONT_ACCOUNT_NAME_PARAM		= "Workfront.AccountNameParam";
	private static final String PROP_WORKFRONT_APIKEY					= "Workfront.ApiKey";
	private static final String PROP_WORKFRONT_DEV_PORTFOLIO			= "Workfront.DevPortfolio";
	private static final String PROP_NEW_PROJECT_REQUEST_PROJECT_ID		= "Workfront.NewProjectRequestProjectID";
	private static final String PROP_WORKFRONT_OPPORTUNITY_NAME_PARAM	= "Workfront.OpportunityNameParam";
	private static final String PROP_WORKFRONT_PROGRAM_DEV_TEAM 		= "Workfront.ProgramDevTeamMap";
	private static final String PROP_WORKFRONT_PROGRAM_PREFIXES 		= "Workfront.ProgramPrefixes";
	private static final String PROP_WORKFRONT_URL						= "Workfront.Url";
	private static final String PROP_WORKFRONT_USERNAME					= "Workfront.Username";
	
	private HashMap<String,String> programToDevTeam = null;
	
	// Public methods
	public SyncProperties() throws IOException {
		super();

		logger.debug("Loading properties file '{}{}{}'", System.getProperty("user.dir"), File.separator, PROPERTIES_FILE_NAME);
		FileInputStream in = new FileInputStream(PROPERTIES_FILE_NAME);
		this.load(in);
		in.close();
	}
	
	public long getTimeToSleep() {
		return Long.parseLong(this.getProperty(PROP_TIME_TO_SLEEP, DEFAULT_TIME_TO_SLEEP));
	}
	
	public String getWorkfrontUsername() {
		return this.getProperty(PROP_WORKFRONT_USERNAME);
	}

	public String getWorkfrontAccountNameParam() {
		return this.getProperty(PROP_WORKFRONT_ACCOUNT_NAME_PARAM); 
	}

	public String getWorkfrontOpportunityNameParam() {
		return this.getProperty(PROP_WORKFRONT_OPPORTUNITY_NAME_PARAM); 
	}

	public String getWorkfrontApiKey() {
		return this.getProperty(PROP_WORKFRONT_APIKEY); 
	}

	public String getWorkfrontUrl() {
		return this.getProperty(PROP_WORKFRONT_URL);
	}

	public String getWorkfrontDevPortfolioName() {
		return this.getProperty(PROP_WORKFRONT_DEV_PORTFOLIO);
	}
	
	public String getWorkfrontNewProjectRequestProejctID() {
		return this.getProperty(PROP_NEW_PROJECT_REQUEST_PROJECT_ID);
	}

	public String getWorkfrontJiraTaskTemplateName() {
		return this.getProperty(PROP_JIRA_TASK_TEMPLATE);
	}

	public String getJiraUsername() {
		return this.getProperty(PROP_JIRA_USERNAME);
	}

	public String getJiraPassword() {
		String base64pass = this.getProperty(PROP_JIRA_PASSWORD); 
		return new String(Base64.decode(base64pass));
	}

	public String getJiraJDBCConnectionString() {
		return this.getProperty(PROP_JIRA_JDBC_CONNECTION_STRING); 
	}
	
	public String getJiraCreateProjectUrl() {
		return this.getProperty(PROP_JIRA_CREATE_PROJECT_URL);
	}
	
	public String getDefaultVersion() {
		return this.getProperty(PROP_DEFAULT_VERSION);
	}
	
	public String getJiraBrowseUrl() {
		return this.getProperty(PROP_JIRA_BROWSE_URL);
	}
	
	public String getCRMJDBCConnectionString() {
		return this.getProperty(PROP_CRM_JDBC_CONNECTION_STRING); 
	}

	public HashMap<String,String> getProgramPrefixMap() {
		return parseMappings(PROP_WORKFRONT_PROGRAM_PREFIXES);
	}
	
	public String lookupDevTeam(String program) {
		if (program == null) {
			return null;
		}
		
		if (programToDevTeam == null) {
			programToDevTeam = parseMappings(PROP_WORKFRONT_PROGRAM_DEV_TEAM);
		}
		
		if (programToDevTeam.containsKey(program)) {
			return programToDevTeam.get(program);
		}
		else {
			return program;
		}
	}

	private HashMap<String, String> parseMappings(String property) {
		HashMap<String, String> map = new HashMap<String,String>();
		
		String items = this.getProperty(property);
		if (items == null) {
			return map;
		}
		
		for (String item : items.split(";")) {
			String[] fields = item.split(":");
			if (fields.length != 2) {
				logger.error("Error parsing {} parameter. Unrecognized format. Value={}", property, item);
				continue;
			}
			map.put(fields[0],fields[1]);
		}
		
		return map;
	}
}