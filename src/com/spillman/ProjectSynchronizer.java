package com.spillman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import javax.swing.event.ListSelectionEvent;

import com.attask.api.StreamClientException;
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
			workfrontClient = new WorkfrontClient(
				properties.getWorkfrontUrl(), 
				properties.getWorkfrontDevPortfolioName(),
				properties.getWorkfrontJiraTaskTemplateName(),
				properties.getWorkfrontAccountNameParam(),
				properties.getWorkfrontOpportunityNameParam(),
				properties.getWorkfrontNewProjectRequestProejctID()
			);
			workfrontClient.login(properties.getWorkfrontUsername(), properties.getWorkfrontApiKey());
		} catch (WorkfrontException e) {
			logger.fatal("Error trying to initialize Workfront", e);
			System.exit(-1); // There is no point in continuing if we couldn't login to Workfront
		}
		
		// Initialize the Jira client
		logger.debug("Initializing Jira...");
		try {
			jiraClient = new JiraClient(properties.getJiraCreateProjectUrl(), 
					properties.getJiraUsername(),
					properties.getJiraPassword(), 
					properties.getJiraJDBCConnectionString(),
					properties.getJiraBrowseUrl(),
					properties.getProgramPrefixMap());
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

	
	private static void syncrhonizeRequests(Date lastSyncTimestamp, Date currentTimestamp) {
		try {
			//Testing Code
//			Opportunity[] testOpps = new Opportunity[] {
//					new Opportunity("Redding Police, CA (4)", "01E73261-6A09-E111-99FE-005056BA0029"), // referenced by a Request
//					new Opportunity("Ramsey County Sheriff's Department", "86AB8421-BF3A-E511-940F-0050569F8035"), // referenced by a Project
//					new Opportunity("Red River Marshall's Office", "575785CD-15D7-E411-940F-0050569F8035") // not referenced
//			};
//			workfrontClient.removeOpportunities(Arrays.asList(testOpps));
			//END Testing Code

			workfrontClient.removeOpportunities(crmClient.getClosedOpportunities(lastSyncTimestamp));
			workfrontClient.addOpportunities(crmClient.getNewOpportunities(lastSyncTimestamp));
			workfrontClient.addAccounts(crmClient.getNewAccounts(lastSyncTimestamp));
			
			activeRequests = workfrontClient.getActiveRequests(activeRequests, lastSyncTimestamp, currentTimestamp);
			for (Request request : activeRequests.values()) {
				Opportunity opp = request.getOpportunity();
				if (opp != null && opp.getCrmOpportunityID() != null && !opp.getCrmOpportunityID().isEmpty()) {
					Opportunity curopp = crmClient.getOpportunity(opp);
					if (!opp.hasSameStatus(curopp)) {
						workfrontClient.updateOpportunityStatus(request, curopp);
					}
				}
			}
		} catch (WorkfrontException e) {
			logger.fatal("Experienced a Workfront error", e);
			System.exit(-1);
		} catch (CRMException e) {
			logger.fatal("Experienced a CRM error", e);
			System.exit(-1);
		}
	}

	/*
	 * Pseudo code for syncing tasks and projects:
	 * 
	 * Update the in memory list of active projects
	 * For each project:
	 *     If it hasn't been added to Jira (i.e. there is no Jira project number)
	 *         Add it to Jira
	 *         Update Workfront with the new project id
	 *     Sync the project
	 *        Sync Tasks
	 *            Update the in memory list of active tasks
	 *            For each task:
	 *                If the task hasn't been added to Jira (i.e. there is no Jira issue number)
	 *                    Add it to Jira
	 *                        Search for the epic (identified by the Jira Epic Name field) in Jira
	 *                        If the epic is not found
	 *                            Add the epic to Jira
	 *                            Add the epic to the in memory list of tasks
	 *                        Add the issue to Jira
	 *                        Update Workfront with the new issue number
	 *                Otherwise
	 *                    Query Jira for updates to the task
	 *                    If the task has changed
	 *                        Update the task in Workfront
	 *            Search Jira for any new epics to be added to Workfront
	 *        Sync Worklog
	 *     
	 */

	private static void synchronizeProjects(Date lastSyncTimestamp, Date currentSyncTimestamp) {
		try {
			activeProjects = workfrontClient.updateProjectList(activeProjects, lastSyncTimestamp, currentSyncTimestamp);
			
			for (Project project : activeProjects.values()) {
				if (project.hasJiraProjectID()) {
					logger.debug("Syncing project '{}'...", project.getName());
					syncTasks(project, lastSyncTimestamp, currentSyncTimestamp);
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
	
	private static void syncTasks(Project project, Date lastSyncTimestamp, Date currentSyncTimestamp) throws JiraException, WorkfrontException {
		ArrayList<Task> tasks = jiraClient.getEpics(project.getJiraProjectID());
		
		for (Task task : tasks) {
			if (project.hasTask(task.getJiraIssuenum())) {
				if (taskHasChanged(task, project.getDevTask(task.getJiraIssuenum()))) {
					logger.debug("Updating task '{}'...", task.getJiraIssuenum());
					Task t = workfrontClient.updateTask(project, task);
					// replace the current task with the new updated task.
					project.addDevTask(t);
				}
				else {
					logger.debug("Task {} hasn't changed", task.getJiraIssuenum());
				}
			}
			else {
				logger.debug("Adding task '{}'...", task.getJiraIssuenum());
				Task newTask = workfrontClient.addTaskToProject(project, task);
				project.addDevTask(newTask);
			}	
		}
	}


	private static boolean taskHasChanged(Task newTask, Task curTask) {
		logger.entry(newTask, curTask);
		return logger.exit(
		// Check the assignee
		!Objects.equals(newTask.getAssigneeName(), curTask.getAssigneeName()) ||
		// Check the parent
		!Objects.equals(newTask.getJiraParentIssuenum(), curTask.getJiraParentIssuenum()) ||
		// Check the duration
		!Objects.equals(newTask.getDuration(), curTask.getDuration()) ||
		// Check the percent complete
		!Objects.equals(newTask.getPercentComplete(), curTask.getPercentComplete()) ||
		// Check the name
		!Objects.equals(newTask.getName(), curTask.getName()) ||
		// Check the description
		!Objects.equals(newTask.getDescription(), curTask.getDescription())
		);		
	}
}
