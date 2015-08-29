package com.spillman.jira;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jira.api.Jira;
import com.jira.api.JiraRestAPIException;
import com.jira.api.JiraRestClient;
import com.jira.api.JiraSQLClient;
import com.spillman.SyncProperties;
import com.spillman.common.Account;
import com.spillman.common.Project;
import com.spillman.common.Task;
import com.spillman.common.WorkLog;

public class JiraClient {
	private static final Logger logger = LogManager.getLogger();
	
	private static final int MAX_KEY_LENGTH = 10;
	private static final int MAX_PROJECT_NAME_LENGTH = 80;
	
	private JiraSQLClient sqlClient;
	private JiraRestClient restClient;
	private String jiraBrowseUrl;
	private HashMap<String,String> programPrefix;
	
	public JiraClient(SyncProperties props) throws JiraException {
		logger.entry(props);
		
		this.restClient = new JiraRestClient(props);

		String jdbcConnectionString = props.getJiraJDBCConnectionString();
		this.sqlClient = new JiraSQLClient(jdbcConnectionString);

		this.jiraBrowseUrl = props.getJiraBrowseUrl();
		this.programPrefix = props.getProgramPrefixMap();
		
		logger.exit();
	}
	
	public List<Account> getPilotAgencies() throws JiraException {
		return sqlClient.getPilotAgencies();
	}
	
	public Task createIssue(Project project, Task task) throws JiraException {
		// Create the Jira issue
		try {
			task = restClient.createIssue(project.getJiraProjectID(), task); 
		} catch (JiraRestAPIException e) {
			throw new JiraException(e);
		}
		
		// If an epic name was specified in Workfront, then link the new issue
		// to the specified epic.
		if (task.getJiraEpicName() != null && !task.getJiraEpicName().isEmpty() 
			&& task.getJiraIssueKey() != null && !task.getJiraIssueKey().isEmpty()) {
			
			// Search Jira for an epic with the specified name
			String epicKey = sqlClient.getEpicKey(task.getJiraEpicName(), project.getJiraProjectID());
			
			// If it's not there, add the epic
			try {
				if (epicKey == null) {
					Task t = restClient.createEpic(project.getJiraProjectID(), task.getJiraEpicName());
					epicKey = t.getJiraIssueKey();
				}
				
				// Link the new issue to the epic
				restClient.linkIssueToEpic(task.getJiraIssueKey(), epicKey);
			} catch (JiraRestAPIException e) {
				throw new JiraException(e);
			}
		}
		
		return task;
	}

	public Task getIssue(String issueID) {
		return null;
	}
	
	public ArrayList<WorkLog> getWorkLogEntries(Project project, Date startTime, Date endTime) throws JiraException {
		logger.entry(project, startTime, endTime);
		
		ArrayList<WorkLog> worklog;
		
		try {
			logger.debug("Getting work log entries between {} and {}", startTime, endTime);
			
			ResultSet rs;
			if (startTime != null) {
				rs = sqlClient.getWorkLog(project.getJiraProjectID(), new java.sql.Timestamp(startTime.getTime()), new java.sql.Timestamp(endTime.getTime()));
			} else {
				rs = sqlClient.getWorkLog(project.getJiraProjectID(), new java.sql.Timestamp(endTime.getTime()));
			}
			worklog = processWorkLogEntries(rs);
			rs.close();
			
			logger.debug("Found {} work log entries", worklog.size());
		}
		catch (SQLException e) {
			throw new JiraException(e);
		}
		
		return logger.exit(worklog);
	}
	
	public ArrayList<Task> getEpics(String projectID) throws JiraException {
		logger.entry(projectID);
		ArrayList<Task> tasks = sqlClient.getEpics(projectID);
		for (Task t : tasks) {
			t.setJiraIssueUrl(jiraBrowseUrl + t.getJiraIssueKey());
		}
		logger.debug("Found {} epics", tasks.size());
		return logger.exit(tasks);
	}
	
	public void addProjectToJira(Project project) throws JiraException {
		logger.entry(project);
		
		if (project.getJiraDevTeam() == null) {
			logger.error("Unable to create Jira project because the Workfront project has not been assigned to a program");
			return;
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Jira.PROJECT_KEY, generateShortProjectKey(project));
		params.put(Jira.PROJECT_DESCRIPTION, project.getDescription());
		params.put(Jira.PROJECT_NAME, generateProjectName(project));
		params.put(Jira.URL, project.getURL());
		params.put(Jira.DEVELOPMENT_TEAM, project.getJiraDevTeam());
		params.put(Jira.VERSIONS, project.getVersions());
		
		try {
			String projectID = restClient.createProject(params);
			project.setJiraProjectID(projectID);
			project.setJiraProjectKey((String)params.get(Jira.PROJECT_KEY));
		} catch (JiraRestAPIException e) {
			logger.catching(e);
		}
		
		logger.exit();
	}
	
	private String generateProjectName(Project project) throws JiraException {
		logger.entry(project);
		
		String projectName = project.getName();
		
		// Make sure the project name doesn't exceed the max length allowed.
		if (projectName.length() > MAX_PROJECT_NAME_LENGTH) {
			projectName = projectName.substring(0, MAX_PROJECT_NAME_LENGTH);
		}
		
		int nextSequence = 1;
		String suffix = null;
		
		logger.debug("Trying project name '{}'...", projectName);
		while (sqlClient.projectNameExists(projectName)) {
			// If we get to here then the project name already exists.
			
			// If this isn't our first time through the loop, the project name already 
			// has a suffix. Remove the old suffix before adding the new suffix.
			if (suffix != null && projectName.endsWith(suffix)) {
				projectName = projectName.substring(0, projectName.length() - suffix.length());
			}
			
			// Generate the new suffix.
			suffix = "(" + nextSequence + ")";
			
			// Make sure the new project name won't exceed the max length allowed.
			if (projectName.length() > (MAX_PROJECT_NAME_LENGTH - suffix.length())) {
				// The new project name would be too long. Trim the project name so the
				// new suffix will fit.
				projectName = projectName.substring(0, MAX_PROJECT_NAME_LENGTH - suffix.length());
			}
			
			// Generate the new project name.
			projectName = projectName + suffix;
			
			// In case we come back through the loop again, increment the sequence number.
			nextSequence++;
			logger.debug("Trying project name '{}'...", projectName);
		}
		
		return logger.exit(projectName);
	}
	
	private String generateShortProjectKey(Project project) throws JiraException {
		logger.entry(project);
		
		// The project key may have already been defined in Workfront
		String projectKey = project.getJiraProjectKey();
		if (projectKey == null || projectKey.isEmpty()) {
			// If the project key wasn't defined in Workfront, create one
			projectKey = initializeProjectKey(project);
		}
		else {
			// If it was defined, make sure it meets the requirements for a project key
			projectKey = standardizeProjectKey(projectKey);
		}

		// Check to see if we have a unique project key
		logger.debug("Trying project key '{}'...", projectKey);
		if (sqlClient.projectKeyExists(projectKey)) {
			// The project key already exists, so we'll need to do some more work to
			// generate a unique project key

			// Resize the project key so there is enough room to add a 2 character suffix
			projectKey = projectKey.substring(0, Math.min(projectKey.length(), MAX_KEY_LENGTH - 2));

			char suffixChar1 = 'A';
			char suffixChar2 = 'A';

			// Generate the initial project key and check to see if it exists
			projectKey = projectKey + suffixChar1 + suffixChar2;
			
			logger.debug("Trying project key '{}'...", projectKey);
			while (sqlClient.projectKeyExists(projectKey)) {
				// Increment the suffix characters
				suffixChar2++;
				if (suffixChar2 > 'Z') {
					suffixChar2 = 'A';
					suffixChar1++;
					if (suffixChar1 > 'Z') {
						throw new JiraException("Cannot create unique project key for project '" + project.getName() + "'");
					}
				}
				
				// Generate the new project key
				projectKey = projectKey.substring(0,projectKey.length() - 2) + suffixChar1 + suffixChar2;
				logger.debug("Trying project key '{}'...", projectKey);
			} 
		}
		
		return logger.exit(projectKey);
	}

	private String initializeProjectKey(Project project) {
		// The first letter of the project key represents the product team
		// Lookup up the prefix for the program (specified in the properties file)
		String program = project.getWorkfrontProgram();
		String prefix = programPrefix.get(program);
		if (prefix == null) prefix = program.substring(0,1);
		
		StringBuffer key = new StringBuffer();
		key.append(prefix);
		
		// The rest of the project key is formed by the first letter of
		// each word in the project name
		String[] words = project.getName().split("\\s+");
		for (String word : words) {
			if (Character.isAlphabetic(word.charAt(0))) key.append(word.substring(0,1));
		}
		
		return standardizeProjectKey(key.toString());
	}

	private String standardizeProjectKey(String key) {
		// Project keys can only be letters
		String projectKey = key.toString().replaceAll("[^A-Za-z]","");
		
		// Project keys must be upper case
		projectKey = projectKey.toUpperCase();
		
		// Trim the project key if it is too long
		if (projectKey.length() > MAX_KEY_LENGTH) {
			projectKey = projectKey.substring(0, MAX_KEY_LENGTH);
		}
		
		return projectKey;
	}

	private ArrayList<WorkLog> processWorkLogEntries(ResultSet rs) throws SQLException {
		logger.entry(rs);
		
		ArrayList<WorkLog> worklog = new ArrayList<WorkLog>();
		
		while (rs.next()) {
			WorkLog wl = new WorkLog();
			wl.setDateWorked(rs.getTimestamp(Jira.DATE_WORKED));
			wl.setDescription(rs.getString(Jira.DESCRIPTION));
			wl.setHoursWorked(rs.getDouble(Jira.HOURS_WORKED));
			wl.setJiraIssuenum(rs.getString(Jira.ISSUENUM));
			wl.setJiraWorker(rs.getString(Jira.WORKER));
			wl.setEpicIssuenum(rs.getString(Jira.EPIC_ID));
			wl.setJiraIssueUrl(jiraBrowseUrl + rs.getString(Jira.ISSUE_KEY));
			worklog.add(wl);
		}
		
		return logger.exit(worklog);
	}
}
