package com.spillman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import javax.swing.event.ListSelectionEvent;

import com.attask.api.StreamClientException;
import com.jira.api.Jira;
import com.spillman.common.Opportunity;
import com.spillman.common.Project;
import com.spillman.common.Request;
import com.spillman.common.Task;
import com.spillman.common.WorkLog;
import com.spillman.crm.CRMClient;
import com.spillman.crm.CRMException;
import com.spillman.jira.JiraClient;
import com.spillman.jira.JiraException;
import com.spillman.workfront.WorkfrontClient;
import com.spillman.workfront.WorkfrontException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ProjectSynchronizer {
	private static final Logger logger = LogManager.getLogger();
	
	private static SyncProperties properties = null;
	private static LastSyncProperty lastSync = null;
	private static WorkfrontClient workfrontClient = null;
	private static JiraClient jiraClient = null;
	private static CRMClient crmClient = null;
	private static HashMap<String, Project> activeProjects = null;
	private static HashMap<String, Request> activeRequests = null;
	
	public static void main(String[] args) {
		logger.info("Starting Project Synchronizer");
		try {
			setup();
			synchronize();
		} catch (Exception e) {
			logger.catching(e);
		}
	}


	private static void setup() {
		// Read properties file
		try {
			properties = new SyncProperties();
			lastSync = new LastSyncProperty();
		} catch (IOException e) {
			logger.fatal("Error reading properties file", e);
			System.exit(-1); // There is no point in continuing if we can't read the properties file.
		}

		// Ensure the last sync property file is saved when the application exits
		Thread t = new Thread() {
			public void run() {
				try {
					lastSync.save();
					workfrontClient.logout();
				} catch (StreamClientException e) {
					logger.catching(e);
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(t);
		
		// Initialize the Workfront client
		logger.debug("Initializing Workfront...");
		try {
			workfrontClient = new WorkfrontClient(properties);
			workfrontClient.login(properties.getWorkfrontUsername(), properties.getWorkfrontApiKey());
		} catch (WorkfrontException e) {
			logger.fatal("Error trying to initialize Workfront", e);
			System.exit(-1); // There is no point in continuing if we couldn't login to Workfront
		}
		
		// Initialize the Jira client
		logger.debug("Initializing Jira...");
		try {
			jiraClient = new JiraClient(properties);
		} catch (JiraException e) {
			logger.fatal("Error tyring to initialize Jira", e);
			System.exit(-1);
		}
		
		// Initialize the CRM client
		logger.debug("Initializing CRM...");
		try {
			crmClient = new CRMClient(properties.getCRMJDBCConnectionString());
		} catch (CRMException e) {
			logger.fatal("Error tyring to initialize CRM", e);
			System.exit(-1);
		}
	}

	
	private static void synchronize() {
		Date currentTimestamp;
		Date lastSyncTimestamp = lastSync.getLastSyncDate();
		
		do {
			logger.debug(">>>>> Starting Sync Cycle <<<<<");

			currentTimestamp = new Date();
			
			synchronizeCustomFields(lastSyncTimestamp, currentTimestamp);
			synchronizeProjects(lastSyncTimestamp, currentTimestamp);
			syncrhonizeRequests(lastSyncTimestamp, currentTimestamp);
			
			lastSyncTimestamp = currentTimestamp;
			lastSync.setLastSyncDate(lastSyncTimestamp);
			
			try {
				lastSync.save();
			} catch (IOException e) {
				logger.catching(e);
			}

			logger.debug("<<<<< End of Sync Cycle >>>>>");
			try { 
				logger.info("Sleeping for {} seconds...", properties.getTimeToSleep()/1000);
				Thread.sleep(properties.getTimeToSleep()); 
			}
			catch (InterruptedException e) { 
				logger.catching(e);
			}
		} while(true);
	}

	
	private static void synchronizeCustomFields(Date lastSyncTimestamp,	Date currentTimestamp) {
		try {
			workfrontClient.removeOpportunities(crmClient.getClosedOpportunities(lastSyncTimestamp));
			workfrontClient.addOpportunities(crmClient.getNewOpportunities(lastSyncTimestamp));
			workfrontClient.addAccounts(crmClient.getNewAccounts(lastSyncTimestamp));
			workfrontClient.addPilotAgencies(jiraClient.getPilotAgencies());
		} catch (WorkfrontException | CRMException | JiraException e) {
			logger.fatal("Experienced an error", e);
			System.exit(-1);
		}
		
	}


	private static void syncrhonizeRequests(Date lastSyncTimestamp, Date currentTimestamp) {
		try {
			activeRequests = workfrontClient.getActiveRequests(activeRequests, lastSyncTimestamp, currentTimestamp);
			for (Request request : activeRequests.values()) {
				Opportunity opp = request.getOpportunity();
				if (opp != null && opp.getCrmOpportunityID() != null && !opp.getCrmOpportunityID().isEmpty()) {
					Opportunity curopp = crmClient.getOpportunity(opp);
					if (curopp != null && !opp.hasSameStatus(curopp)) {
						workfrontClient.updateOpportunityStatus(request, curopp);
					}
				}
			}
		} catch (WorkfrontException | CRMException e) {
			logger.fatal("Experienced an error", e);
			System.exit(-1);
		}
	}

	private static void synchronizeProjects(Date lastSyncTimestamp, Date currentSyncTimestamp) {
		try {
			activeProjects = workfrontClient.updateProjectList(activeProjects, lastSyncTimestamp, currentSyncTimestamp);
			
			for (Project project : activeProjects.values()) {
				if (project.hasJiraProjectID()) {
					logger.debug("Syncing project '{}'...", project.getName());
					syncTasks(project);
//					syncTasks(project, lastSyncTimestamp, currentSyncTimestamp);
					syncWorkLog(project, currentSyncTimestamp);
				}
				else {
					logger.debug("Creating project '{}' in Jira...", project.getName());
					project.setJiraDevTeam(properties.lookupDevTeam(project.getWorkfrontProgram()));
					if (project.getVersions() == null) 
					{ 
						project.addVersion(properties.getDefaultVersion()); 
					}
					
					jiraClient.addProjectToJira(project);
					workfrontClient.updateJiraProjectID(project);
				}
			}
		}
		catch (WorkfrontException e) {
			logger.fatal("Experienced a Workfront error", e);
			System.exit(-1);
		} catch (JiraException e) {
			logger.fatal("Experienced a Jira error", e);
			System.exit(-1);
		}
	}

	private static void syncWorkLog(Project project, Date currentSyncTimestamp) throws JiraException, WorkfrontException {
		ArrayList<WorkLog> worklog = jiraClient.getWorkLogEntries(project, project.getLastJiraSync(), currentSyncTimestamp);
		
		for (WorkLog wl : worklog) {
			logger.debug("Adding worklog entry for task '{}'...", wl.getJiraIssuenum());
			workfrontClient.addWorkLogEntry(project, wl);
		}
		
		// If we processed some worklog entries, update the Last Jira Sync custom field
		// on the Workfront project
		if (worklog.size() > 0) {
			project.setLastJiraSync(currentSyncTimestamp);
			workfrontClient.updateLastJiraSync(project);
		}
	}
	
	private static void syncTasks(Project project) throws WorkfrontException, JiraException {
		// First, go through all the tasks we found in Workfront and make sure they
		// are up to date with Jira.
		for (Task task : project.getDevTasks().values()) {
			if (task.getJiraIssueID() == null || task.getJiraIssueID().isEmpty()) {
				
				// This is a new task. Add it to Jira.
				Task newTask = jiraClient.createIssue(project, task);
				
				// Then update the Workfront task with the Jira issue ID and URL
				workfrontClient.updateTask(project, newTask);
			}
			else {
				// This task is already in Jira. Query Jira to see if we need to update the task.
			}
		}
		
		// Then, look for new epics in Jira and add them to Workfront.
		
	}
	
	private static void syncTasks(Project project, Date lastSyncTimestamp, Date currentSyncTimestamp) throws JiraException, WorkfrontException {
		ArrayList<Task> tasks = jiraClient.getEpics(project.getJiraProjectID());
		
		for (Task task : tasks) {
			if (project.hasTask(task.getJiraIssueID())) {
				if (task.equals(project.getDevTask(task.getJiraIssueID()))) {
					logger.debug("Updating task '{}'...", task.getJiraIssueID());
					Task t = workfrontClient.updateTask(project, task);
					// replace the current task with the new updated task.
					project.addDevTask(t);
				}
				else {
					logger.debug("Task {} hasn't changed", task.getJiraIssueID());
				}
			}
			else {
				logger.debug("Adding task '{}'...", task.getJiraIssueID());
				Task newTask = workfrontClient.addImplementationSubtask(project, task);
				project.addDevTask(newTask);
			}	
		}
	}
}
