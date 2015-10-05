package com.spillman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.spillman.common.Account;
import com.spillman.common.Opportunity;
import com.spillman.common.OpportunityHolder;
import com.spillman.common.Project;
import com.spillman.common.Request;
import com.spillman.common.Task;
import com.spillman.common.WorkLog;
import com.spillman.crm.CRMClient;
import com.spillman.crm.CRMException;
import com.spillman.jira.JiraClient;
import com.spillman.jira.JiraException;
import com.spillman.jira.JiraIssueNotFoundException;
import com.spillman.jira.MissingDevTeamException;
import com.spillman.workfront.Workfront;
import com.spillman.workfront.WorkfrontClient;
import com.spillman.workfront.WorkfrontException;
import com.spillman.workfront.WorkfrontObjectNotFoundException;
import com.spillman.workfront.api.StreamClientException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ProjectSynchronizer {
	private static final Logger logger = LogManager.getLogger();
	
	private static String FATAL_ERROR_MESSAGE = "FATAL ERROR, EMAILING ADMIN";
	private static SyncProperties properties = null;
	private static LastSyncProperty lastSync = null;
	private static WorkfrontClient workfrontClient = null;
	private static JiraClient jiraClient = null;
	private static CRMClient crmClient = null;
	private static HashMap<String, Project> activeProjects = null;
	private static HashMap<String, Request> activeRequests = null;
	private static HashMap<String,String> wfPilotAgencies = null;
	
	public static void main(String[] args) {
		logger.info("Starting Project Synchronizer");
		
		try {
			setup();
			synchronize();
		} catch (Exception e) {
			
			// Obviously something unexpected happened if we got to this point
			// Log the error and then exit
			logger.fatal(FATAL_ERROR_MESSAGE, e);
		}
	}


	private static void setup() {
		// Read properties file
		try {
			properties = new SyncProperties();
			lastSync = new LastSyncProperty();
		} catch (IOException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
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
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1); // There is no point in continuing if we couldn't login to Workfront
		}
		
		// Initialize the Jira client
		logger.debug("Initializing Jira...");
		try {
			jiraClient = new JiraClient(properties);
		} catch (JiraException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1);
		}
		
		// Initialize the CRM client
		logger.debug("Initializing CRM...");
		try {
			crmClient = new CRMClient(properties.getCRMJDBCConnectionString());
		} catch (CRMException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1);
		}
	}

	
	private static void synchronize() {
		Date currentTimestamp;
		Date lastSyncTimestamp = lastSync.getLastSyncDate();
		
		do {
			logger.debug(">>>>> Start Sync Cycle <<<<<");

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

			logger.debug("<<<<< End Sync Cycle >>>>>");
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
			syncPilotAgencies();
		} catch (WorkfrontException | CRMException | JiraException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1);
		}
		
	}

	private static void synchronizeProjects(Date lastSyncTimestamp, Date currentSyncTimestamp) {
		try {
			// TODO: Now that we are syncing opportunities on projects, we need to sync all projects
			// that have an opportunity.
			activeProjects = workfrontClient.updateProjectList(activeProjects, lastSyncTimestamp, currentSyncTimestamp);
			
			for (Project project : activeProjects.values()) {
				
				// If we are syncing this project with Jira...
				if (project.isSyncWithJira()) {

					// Add new projects to Jira
					if (!project.hasJiraProjectID()) {
						createJiraProject(project);
					}

					// Sync Jira issues and worklog with Workfront
					syncTasks(project, lastSyncTimestamp, currentSyncTimestamp);
					syncWorkLog(project, currentSyncTimestamp);
				}

				// Sync the opportunities
				syncOpportunity(project);
			}
		}
		catch (WorkfrontException | JiraException | CRMException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1);
		}
	}
	
	private static void syncrhonizeRequests(Date lastSyncTimestamp, Date currentTimestamp) {
		List<String> requestsToDelete = new ArrayList<String>();
		
		// Update the list of active requests
		try {
			activeRequests = workfrontClient.getActiveRequests(activeRequests, lastSyncTimestamp, currentTimestamp);
		} catch (WorkfrontException e) {
			logger.fatal(FATAL_ERROR_MESSAGE, e);
			System.exit(-1);
		}
		
		// Sync each request
		for (Request request : activeRequests.values()) {
			try {
				syncOpportunity(request);
			} catch (WorkfrontException e) {
				
				// We may find that the request doesn't exist in Workfront anymore
				// (this happens if the request was converted to a project)
				if (e.getMessageKey() != null && e.getMessageKey().equals(Workfront.RECORD_NOT_FOUND)) {
					
					// Keep track of the deleted request so we can remove it from the list
					requestsToDelete.add(request.getWorkfrontRequestID());
				} else {
					logger.fatal(FATAL_ERROR_MESSAGE, e);
					System.exit(-1);
				}
			} catch (CRMException e) {
				logger.fatal(FATAL_ERROR_MESSAGE, e);
				System.exit(-1);
			}
		}

		// If we ran across any requests that have been deleted
		// remove them from our list.
		for (String id : requestsToDelete) {
			activeRequests.remove(id);
		}
	}

	private static void syncPilotAgencies() throws JiraException, WorkfrontException {
		// Initialize the in memory copy of the list of pilot agencies
		if (wfPilotAgencies == null) {
			wfPilotAgencies = workfrontClient.getPilotAgencies(); 
		}
		
		// Compare the list of pilot agencies in Jira to the list of
		// pilot agencies in Workfront. Keep track of those that
		// are missing in Workfront.
		List<Account> newPilotAgencies = new ArrayList<Account>();
		for (Account account : jiraClient.getPilotAgencies()) {
			if (wfPilotAgencies.get(account.agencyCode) == null) {
				newPilotAgencies.add(account);
				wfPilotAgencies.put(account.agencyCode, account.agencyCode);
			}
		}
		
		// Now add the missing pilot agencies to Workfront.
		workfrontClient.addPilotAgencies(newPilotAgencies);
	}


	private static void createJiraProject(Project project) throws JiraException,	WorkfrontException {
		logger.debug("Creating project '{}' in Jira...", project.getName());
		project.setJiraDevTeam(properties.lookupDevTeam(project.getWorkfrontProgram()));
		if (project.getVersions() == null) 
		{ 
			project.addVersion(properties.getDefaultVersion()); 
		}
		
		try {
			jiraClient.addProjectToJira(project);
			workfrontClient.updateJiraProjectID(project);
		} catch (MissingDevTeamException e) {
			logger.catching(e);
		}
	}

	private static void syncOpportunity(OpportunityHolder wfObject) throws CRMException, WorkfrontException {
		logger.debug("Syncing opportunites for Workfront object {}", wfObject);
		List<String> oppIDs = wfObject.getAllOpportunityIDs();
		
		if (oppIDs.size() > 0) {
			Opportunity curleadopp = wfObject.getOpportunity();
			Opportunity newleadopp = crmClient.getLeadingOpportunity(oppIDs);
			Integer curProbability = wfObject.getCombinedProbability();
			Integer newProbability = crmClient.getCombinedProbability(oppIDs);
			
			if (curleadopp == null || curleadopp.getCrmOpportunityID() == null
					|| !curleadopp.getCrmOpportunityID().equals(newleadopp.getCrmOpportunityID())
					|| curProbability == null
					|| !curProbability.equals(newProbability)) {
				wfObject.setCombinedProbability(newProbability);
				workfrontClient.updateOpportunityStatus(wfObject, newleadopp);
			}
		}
	}

	private static void syncWorkLog(Project project, Date currentSyncTimestamp) throws JiraException, WorkfrontException {
		logger.debug("Syncing worklogs for project {}", project.getName());
		ArrayList<WorkLog> worklog = jiraClient.getWorkLogEntries(project, project.getLastJiraSync(), currentSyncTimestamp);
		
		for (WorkLog wl : worklog) {
			logger.debug("Adding worklog entry for task '{}'...", wl.getJiraIssuenum());
			try {
				workfrontClient.addWorkLogEntry(project, wl);
			}
			catch (WorkfrontObjectNotFoundException e) {
				logger.catching(e);
			}
		}
		
		// If we processed some worklog entries, update the "Last Jira Sync"
		// custom field in the Workfront project
		if (worklog.size() > 0) {
			project.setLastJiraSync(currentSyncTimestamp);
			workfrontClient.updateLastJiraSync(project);
		}
	}
	
	private static void syncTasks(Project project, Date lastSyncTimestamp, Date currentSyncTimestamp) throws WorkfrontException, JiraException {
		logger.debug("Syncing tasks for project {}", project.getName());
		
		// First, go through all the tasks we found in Workfront and make sure they
		// are up to date with Jira.
		for (Task task : project.getWorkfrontDevTasks().values()) {

			// Skip any tasks that aren't marked to sync with Jira
			if (!task.isSyncWithJira()) {
				continue;
			}
			
			if (task.getJiraIssueID() == null || task.getJiraIssueID().isEmpty()) {
				try {
					// This is a new task. Add it to Jira.
					jiraClient.createIssue(project, task);
	
					// Then update the Workfront task with the Jira issue ID and URL
					workfrontClient.updateTask(project, task);
					project.addDevTask(task);
				}
				catch (JiraException | WorkfrontException e) {
					logger.catching(e);
				}
			}
			else {
				// This task is already in Jira. Query Jira to see if we need to update the task.
				Task jiraIssue;
				try {
					jiraIssue = jiraClient.getIssue(task);
					if (!jiraIssue.equals(task)) {
						logger.debug("Updating task '{}'...", task.getJiraIssueID());
						jiraIssue.setWorkfrontTaskID(task.getWorkfrontTaskID());
						workfrontClient.updateTask(project, jiraIssue);
						// replace the current task with the new updated task.
						project.addDevTask(jiraIssue);
					}
					else {
						logger.debug("Task {} hasn't changed", task.getJiraIssueID());
					}
				} catch (JiraIssueNotFoundException e) {
					logger.catching(e);
				}
			}
		}
		
		// Then, look for new epics in Jira and add them to Workfront.
		ArrayList<Task> epics = jiraClient.getEpics(project.getJiraProjectID());
		for (Task task : epics) {
			if (!project.hasJiraTask(task.getJiraIssueID()) // if the epic isn't already a task in the project 
					&& !project.hasSpecialEpic(task.getName()) // and the name of the epic is not on the list of "speical" epics
				) {
				// Add the epic to Workfront
				logger.debug("Adding epic '{}'...", task.getJiraIssueID());
				workfrontClient.addImplementationSubtask(project, task);
				project.addDevTask(task);
			}	
		}
	}
}
