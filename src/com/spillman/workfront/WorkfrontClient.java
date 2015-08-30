package com.spillman.workfront;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.attask.api.StreamClient;
import com.attask.api.StreamClientException;
import com.spillman.SyncProperties;
import com.spillman.common.Account;
import com.spillman.common.Opportunity;
import com.spillman.common.Project;
import com.spillman.common.Request;
import com.spillman.common.Task;
import com.spillman.common.WorkLog;

public class WorkfrontClient {
	private static final Logger logger = LogManager.getLogger();
	
	private final static String[] PROJECT_FIELDS = new String[] {
		Workfront.ID,
		Workfront.NAME, 
		Workfront.LAST_UPDATE_DATE, 
		Workfront.JIRA_PROJECT_ID, 
		Workfront.JIRA_PROJECT_KEY,
		Workfront.STATUS, 
		Workfront.OWNER_NAME,
		Workfront.PROGRAM_NAME,
		Workfront.URL,
		Workfront.VERSIONS,
		Workfront.SYNC_WITH_JIRA,
		Workfront.LAST_JIRA_SYNC
	};
	
	private final static String[] REQUEST_FIELDS = new String[] {
		Workfront.ID,
		Workfront.NAME,
		Workfront.STATUS,
		Workfront.OPPORTUNITY_NAME,
		Workfront.OPPORTUNITY_PROBABILITY,
		Workfront.OPPORTUNITY_FLAG,
		Workfront.OPPORTUNITY_PHASE,
		Workfront.OPPORTUNITY_POSITION,
		Workfront.OPPORTUNITY_STATE
	};
	
	private final static String[] TASK_FIELDS = new String[] {
		Workfront.ID,
		Workfront.NAME,
		Workfront.DESCRIPTION,
		Workfront.ASSIGNED_TO_ID,
		Workfront.STATUS,
		Workfront.PERCENT_COMPLETE,
		Workfront.DURATION_MINUTES,
		Workfront.JIRA_ISSUE_ID,
		Workfront.JIRA_ISSUE_EPIC_NAME,
		Workfront.JIRA_ISSUE_TYPE,
		Workfront.JIRA_ISSUE_URL,
		Workfront.PILOT_AGENCY,
		Workfront.JIRA_SYNC_TASK,
		Workfront.PARENT_ID
	};
	
	private StreamClient client = null;
	
	private String portfolioID 				= null;
	private String portfolioName 			= null;
	private String jiraTaskCustomFormID 	= null;
	private String jiraTaskCustomFormName	= null;
	private String paramAccountName			= null;
	private String accountNameFieldID		= null;
	private String paramOpportunityName		= null;
	private String opportunityNameFieldID	= null;
	private String paramPilotAgency			= null;
	private String pilotAgencyFieldID		= null;
	private String newRequestProjectID		= null;
	private HashMap<String, String> users	= null;

	public WorkfrontClient(SyncProperties props) throws WorkfrontException {
			
		logger.entry(props);
		
		String url = props.getWorkfrontUrl();
		if (url == null) {
			throw new WorkfrontException("Workfront URL cannot be null");
		}
		
		String portfolio = props.getWorkfrontDevPortfolioName();
		if (portfolio == null) {
			throw new WorkfrontException("Workfront Development portfolio name cannot be null");
		}
		
		String jiraTask = props.getWorkfrontJiraTaskTemplateName();
		if (jiraTask == null) {
			throw new WorkfrontException("Workfront Jira task template name cannot be null");
		}
		
		String accountName = props.getWorkfrontAccountNameParam();
		if (accountName == null) {
			throw new WorkfrontException("Workfront Account Name cannot be null");
		}
		
		String opportunityName = props.getWorkfrontOpportunityNameParam();
		if (opportunityName == null) {
			throw new WorkfrontException("Workfront Opportunity Name parameter cannot be null");
		}
		
		String pilotAgency = props.getWorkfrontPilotAgencyParam();
		if (pilotAgency == null) {
			throw new WorkfrontException("Workfront Pilot Agency parameter cannot be null");
		}
		
		String newRequestProjectID = props.getWorkfrontNewProjectRequestProejctID();
		if (newRequestProjectID == null) {
			throw new WorkfrontException("Workfront New Project Request Project ID parameter cannot be null");
		}
		
		this.client = new StreamClient(url);
		this.portfolioName = portfolio;
		this.jiraTaskCustomFormName = jiraTask;
		this.paramAccountName = accountName;
		this.paramOpportunityName = opportunityName;
		this.newRequestProjectID = newRequestProjectID;
		this.paramPilotAgency = pilotAgency;
		
		logger.exit();
	}

	public void login(String username, String apikey) throws WorkfrontException {
		logger.entry(username, apikey);
		
		try {
			client.login(username, apikey);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}

		// Now that we are logged in, get/save some information from Workfront
		try {
			portfolioID = getPortfolioID();
			jiraTaskCustomFormID = getJiraTaskCustomFormID();
			users = getWorkfrontUsers();
			accountNameFieldID = getObjectIdByName(Workfront.OBJCODE_PARAM, paramAccountName);
			opportunityNameFieldID = getObjectIdByName(Workfront.OBJCODE_PARAM, paramOpportunityName);
			pilotAgencyFieldID = getObjectIdByName(Workfront.OBJCODE_PARAM, paramPilotAgency);
		} catch (JSONException e) {
			throw new WorkfrontException(e);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		logger.exit();
	}
	
	public void logout() throws StreamClientException {
		logger.entry();
		client.logout();
		logger.exit();
	}

	public void addAccounts(List<Account> accounts) throws WorkfrontException {
		logger.entry(accounts);
		
		for (Account account : accounts) {
			addParameterOption(accountNameFieldID, account.accountGUID, account.accountName);
			logger.trace("Added account {}.", account);
		}

		logger.exit();
	}

	public void addPilotAgencies(List<Account> accounts) throws WorkfrontException {
		logger.entry(accounts);
		
		for (Account account : accounts) {
			addParameterOption(pilotAgencyFieldID, account.agencyCode, account.agencyCode);
			logger.trace("Added pilot agency {}.", account.agencyCode);
		}

		logger.exit();
	}

	public void addOpportunities(List<Opportunity> opportunities) throws WorkfrontException {
		logger.entry(opportunities);
		
		for (Opportunity opportunity : opportunities) {
			addParameterOption(opportunityNameFieldID, opportunity.getCrmOpportunityID(), opportunity.getName());
			logger.trace("Added opportunity {}.", opportunity);
		}

		logger.exit();
	}
	
	private void addParameterOption(String id, String value, String label) throws WorkfrontException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.PARAMETER_ID, id);
		map.put(Workfront.VALUE, value);
		map.put(Workfront.LABEL, label);
		
		try {
			client.post(Workfront.OBJCODE_POPT, map);
		} catch (StreamClientException e) {
			if (e.getMessageKey() != null && e.getMessageKey().equals(Workfront.UNIQUE_KEY_VIOLATION)) {
				logger.warn("Parameter option {}:{} already exists", label, value);
			}
			else {
				throw new WorkfrontException(e);
			}
		}
	}

	public void removeOpportunities(List<Opportunity> opportunities) throws WorkfrontException {
		for (Opportunity opp : opportunities) {
			if (opportunityIsReferencedByProject(opp) || opportunityIsReferencedByRequest(opp)) {
				hideOpportunity(opp);
			}
			else {
				deleteOpportunity(opp);
			}
		}
	}
	
	private boolean opportunityIsReferencedByProject(Opportunity opp) throws WorkfrontException {
		return opportunityIsReferencedBy(Workfront.OBJCODE_PROJ, opp);
	}
	
	private boolean opportunityIsReferencedByRequest(Opportunity opp) throws WorkfrontException {
		return opportunityIsReferencedBy(Workfront.OBJCODE_ISSUE, opp);
	}
	
	private boolean opportunityIsReferencedBy(String objcode, Opportunity opp) throws WorkfrontException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.OPPORTUNITY_NAME, opp.getCrmOpportunityID());
		try {
			JSONArray results = client.search(objcode, map);
			return results.length() > 0;
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
	}

	private void deleteOpportunity(Opportunity opp) throws WorkfrontException {
		try {
			String optionID = getObjectIdByValue(Workfront.OBJCODE_POPT, opp.getCrmOpportunityID());
			boolean success = client.delete(Workfront.OBJCODE_POPT, optionID);
			if (!success) {
				logger.error("Unable to remove parameter {}", opp);
			}
			else {
				logger.debug("Removed opportunity {} from the list", opp.getName());
			}
		} catch (StreamClientException | JSONException e) {
			logger.catching(e);
		}
	}
	
	private void hideOpportunity(Opportunity opp) throws WorkfrontException {
		try {
			String optionID = getObjectIdByValue(Workfront.OBJCODE_POPT, opp.getCrmOpportunityID());
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(Workfront.IS_HIDDEN, true);
			JSONObject result = client.put(Workfront.OBJCODE_POPT, optionID, map, new String[] {Workfront.ID, Workfront.IS_HIDDEN, Workfront.LABEL});
			if (result == null) {
				logger.error("Unable to hide parameter {}", opp);
			}
			else {
				logger.debug("Hid opportunity {} in the list", opp.getName());
			}
		} catch (StreamClientException | JSONException e) {
			throw new WorkfrontException(e);
		}
	}
	
	public void addWorkLogEntry(Project project, WorkLog worklog) throws WorkfrontException {
		logger.entry(project, worklog);
		
		Map<String, Object> fields = new HashMap<String, Object>();
		
		// Get the Workfront task for the Jira epic
		Task t = project.getDevTaskByJiraID(worklog.getEpicIssuenum());

		if (t == null) {
			// If we couldn't find a Workfront task for the epic
			// Look for a Workfront task for the Jira issue
			t = project.getDevTaskByJiraID(worklog.getJiraIssuenum());
		}
		
		String wfTaskID;
		if (t == null) {
			// If we couldn't find a Workfront task for the Jira issue
			// Then log the hours against the Workfront implementation task
			wfTaskID = project.getImplementationTaskID();
		} else {
			// We found an existing Workfront task for the Jira epic or Jira issue
			// so log the hours against this Workfront task
			wfTaskID = t.getWorkfrontTaskID();
		}
		
		fields.put(Workfront.TASK_ID, wfTaskID);
		fields.put(Workfront.HOURS, worklog.getHoursWorked());
		fields.put(Workfront.OWNER_ID, users.get(worklog.getJiraWorker()));
		fields.put(Workfront.ENTRY_DATE, Workfront.dateFormatter.format(worklog.getDateWorked()));
		fields.put(Workfront.DESCRIPTION, worklog.getDescription() + " (" + worklog.getJiraIssueUrl() + " )");
		try {
			logger.debug("Adding worklog entry: {}", fields.toString());
			client.post(Workfront.OBJCODE_HOUR, fields);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		logger.exit();
	}

	public void updateJiraProjectID(Project project) throws WorkfrontException {
		logger.entry(project);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.JIRA_PROJECT_ID, project.getJiraProjectID());
		map.put(Workfront.JIRA_PROJECT_KEY, project.getJiraProjectKey());
		try {
			logger.debug("Updating Workfront project with Jira project ID: {}", map.toString());
			client.put(Workfront.OBJCODE_PROJ, project.getWorkfrontProjectID(), map);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		logger.exit();
	}

	public void updateLastJiraSync(Project project) throws WorkfrontException {
		logger.entry(project);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.LAST_JIRA_SYNC, Workfront.dateFormatter.format(project.getLastJiraSync()));
		try {
			logger.debug("Updating Workfront project with last sync timestamp: {}", map.toString());
			client.put(Workfront.OBJCODE_PROJ, project.getWorkfrontProjectID(), map);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		logger.exit();
	}

	public void updateOpportunityStatus(Request request,	Opportunity curopp) throws WorkfrontException {
		logger.entry(request, curopp);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.OPPORTUNITY_FLAG, curopp.getFlag());
		map.put(Workfront.OPPORTUNITY_PHASE, curopp.getPhase());
		map.put(Workfront.OPPORTUNITY_POSITION, curopp.getPosition());
		map.put(Workfront.OPPORTUNITY_PROBABILITY, curopp.getProbability());
		map.put(Workfront.OPPORTUNITY_STATE, curopp.getState());
		try {
			client.put(Workfront.OBJCODE_ISSUE, request.getWorkfrontRequestID(), map);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		logger.exit();
	}

	public HashMap<String, Request> getActiveRequests(HashMap<String, Request> activeRequests,
			Date startTimestamp, Date endTimestamp) throws WorkfrontException {
		logger.entry(activeRequests, startTimestamp, endTimestamp);
		
		// Create search parameters
		Map<String, Object> searchParams;
		
		// If the activeRequests object hasn't been initialized, then we need to find all
		// the active requests in Workfront
		if (activeRequests == null) {
			searchParams = formatActiveRequestsSearchParameters(null, null);
		}
		
		// If the activeRequests object has already been initialized, then we only need to find
		// any requests that have been updated since the last sync.
		else {
			searchParams = formatActiveRequestsSearchParameters(startTimestamp, endTimestamp);
		}
		
		// Search Workfront
		JSONArray requests;
		try {
			requests = client.search(Workfront.OBJCODE_ISSUE, searchParams, REQUEST_FIELDS);
		}
		catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		if (activeRequests == null) {
			activeRequests = new HashMap<String, Request>();
			logger.debug("Found {} active requests in Workfront", requests.length());
		}
		else {
			logger.debug("Found {} active requests that were updated between {} and {}", requests.length(), startTimestamp, endTimestamp);
		}
		
		// Process the results
		for (int i = 0; i < requests.length(); i++) {
			try {
				JSONObject request = (JSONObject)requests.get(i);
				String requestID = request.getString(Workfront.ID);
				String status = request.getString(Workfront.STATUS);
				String name = request.getString(Workfront.NAME);
				
				// If the request is not Closed and it isn't already in the list of projects, add it
				if (!activeRequests.containsKey(requestID) && !status.equals(Workfront.STATUS_CLOSED)) {
					Request r = new Request(request);
					activeRequests.put(r.getWorkfrontRequestID(), r);
					logger.debug("Added request {} ({}) to the list of requests to sync", name, requestID);
				}
				// If the request is in the list and the status is Closed, remove it
				else if(activeRequests.containsKey(requestID) && (status.equals(Workfront.STATUS_CLOSED))) {
					activeRequests.remove(requestID);
					logger.debug("Removed request {} ({}) from the list of requests to sync", name, requestID);
				}
			} catch (JSONException e) {
				throw new WorkfrontException(e);
			}
		}

		return logger.exit(activeRequests);
	}
	
	public HashMap<String, Project> updateProjectList(HashMap<String,Project> activeProjects, 
			Date startTimestamp, Date endTimestamp) throws WorkfrontException {
		logger.entry(activeProjects, startTimestamp, endTimestamp);
		
		// Create search parameters
		Map<String, Object> searchParams;
		
		// If the activeProjects object hasn't been initialized, then we need to find all
		// the active dev projects in Workfront
		if (activeProjects == null) {
			searchParams = formatActiveDevProjectsSearchParameters(null, null);
		}
		
		// If the activeProjects object has already been initialized, then we only need to find
		// any projects that have been updated since the last sync.
		else {
			searchParams = formatActiveDevProjectsSearchParameters(startTimestamp, endTimestamp);
		}
		
		// Search Workfront
		JSONArray projects;
		try {
			projects = client.search(Workfront.OBJCODE_PROJ, searchParams, PROJECT_FIELDS);
		}
		catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
			
		if (activeProjects == null) {
			activeProjects = new HashMap<String, Project>();
			logger.debug("Found {} active projects in Workfront", projects.length());
		}
		else {
			logger.debug("Found {} active projects that were updated between {} and {}", projects.length(), startTimestamp, endTimestamp);
		}
		
		// Process the results
		for (int i = 0; i < projects.length(); i++) {
			try {
				JSONObject project = (JSONObject)projects.get(i);
				String projectID = project.getString(Workfront.ID);
				String status = project.getString(Workfront.STATUS);
				String name = project.getString(Workfront.NAME);
				
				boolean syncWithJira = false;
				if (project.has(Workfront.SYNC_WITH_JIRA)) {
					syncWithJira = project.getString(Workfront.SYNC_WITH_JIRA).equals("Yes");
				}
				
				// If the syncWithJira flag is set and it isn't already in the list of projects, add it
				if (!activeProjects.containsKey(projectID) && syncWithJira) {
					Project p = new Project(project);
					p.setImplementationTaskID(getImplementationTaskID(p.getWorkfrontProjectID()));
					addDevTasks(p);
					activeProjects.put(p.getWorkfrontProjectID(), p);
					logger.debug("Added project {} ({}) to the list of projects to sync", name, projectID);
				}
				// If the project is in the list but the status is Closed or the syncWithJira flag isn't set, remove it
				else if(activeProjects.containsKey(projectID) && (status.equals(Workfront.STATUS_CLOSED) || !syncWithJira)) {
					activeProjects.remove(projectID);
					logger.debug("Removed project {} ({}) from the list of projects to sync", name, projectID);
				}
				// Otherwise, the project is in the list and should be synced with Jira
				else {
					// Update the list of tasks in the project
					addDevTasks(activeProjects.get(projectID));
				}
			} catch (JSONException e) {
				throw new WorkfrontException(e);
			} catch (StreamClientException e) {
				throw new WorkfrontException(e);
			}
		}

		return logger.exit(activeProjects);
	}

	public Task updateTask(Project project, Task updatedTask) throws WorkfrontException {
		logger.entry(project, updatedTask);
		
//		// Copy the Workfront task ID to the updated task
//		Task curTask = project.getDevTask(updatedTask.getJiraIssueID());
//		if (curTask == null) {
//			logger.error("Could not find task {} in development task list in project {}", updatedTask, project);
//			return updatedTask;
//		}
//		updatedTask.setWorkfrontTaskID(curTask.getWorkfrontTaskID());
//
//		// Set the parent ID to the implementation task
//		updatedTask.setWorkfrontParentTaskID(project.getImplementationTaskID());
		
		// Update the task in Workfront
		try {
			Map<String, Object> fields = formatTaskFields(project, updatedTask);
			logger.debug("Updating Workfront task: {}", fields.toString());
			client.put(Workfront.OBJCODE_TASK, updatedTask.getWorkfrontTaskID(), fields);
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		}
		
		return logger.exit(updatedTask);
	}

	public Task addImplementationSubtask(Project project, Task task) throws WorkfrontException {
		logger.entry(project, task);
		
		// Set the parent ID to the implementation task
		task.setWorkfrontParentTaskID(project.getImplementationTaskID());
		
		// Add the task to Workfront
		try {
			Map<String, Object> fields = formatTaskFields(project, task);
			logger.debug("Adding task to Workfront: {}", fields.toString());
			JSONObject newTask = client.post(Workfront.OBJCODE_TASK, fields);
			task.setWorkfrontTaskID(newTask.getString(Workfront.ID));
		} catch (StreamClientException e) {
			throw new WorkfrontException(e);
		} catch (JSONException e) {
			throw new WorkfrontException(e);
		}
		
		return logger.exit(task);
	}

	private Map<String, Object> formatTaskFields(Project project, Task task) {
		logger.entry(project, task);
		
		Map<String, Object> fields = new HashMap<String, Object>();

		if (task.getAssigneeID() != null) {
			fields.put(Workfront.ASSIGNED_TO_ID, task.getAssigneeID());
		}
		
		if (task.getWorkfrontParentTaskID() != null) {
			fields.put(Workfront.PARENT_ID, task.getWorkfrontParentTaskID());
		}

		if (task.getWorkfrontStatus() != null) {
			fields.put(Workfront.STATUS, task.getWorkfrontStatus());
		}

		if (task.getDuration() != null) {
			fields.put(Workfront.WORK_REQ_EXPRESSION, task.getDuration() + " Hours");
			fields.put(Workfront.DURATION_EXPRESSION, task.getDuration() + " Hours");
			fields.put(Workfront.DURATION_TYPE, Workfront.DURATION_EFFORT_DRIVEN);
		}
		
		if (task.getPercentComplete() != null) {
			fields.put(Workfront.PERCENT_COMPLETE, task.getPercentComplete());
			// TODO:
			// The Workfront API won't let us update the percent complete when the status of the Workfront
			// task is Complete. It is possible to get into a situation where the status of the Workfront
			// task is Complete, but the Epic in Jira is not complete. This happens when the Epic in Jira
			// was once marked Done, which caused the task in Workfront to be marked Complete, and then the
			// Epic in Jira was changed back to In Progress.
			//
			// To work around this issue we need to check for the scenario where the current status of the 
			// Workfront task is Complete and the new percent complete is less than 100% complete. When we
			// see this scenario we must set the status of the Workfront task back to In Progress. Then the
			// API will correctly update the percent complete.
		}

		if (task.getDescription() != null) {
			fields.put(Workfront.DESCRIPTION, task.getDescription());
		}
		
		fields.put(Workfront.PROJECT_ID, project.getWorkfrontProjectID());
		fields.put(Workfront.NAME, task.getName());
		fields.put(Workfront.JIRA_ISSUE_ID, task.getJiraIssueID());
		fields.put(Workfront.JIRA_ISSUE_TYPE, task.getJiraIssueType());
		fields.put(Workfront.JIRA_ISSUE_URL, task.getJiraIssueUrl());
		fields.put(Workfront.CUSTOM_FORM_ID, jiraTaskCustomFormID);
		
		return logger.exit(fields);
	}

	public String getUserID(String name) throws WorkfrontException {
		logger.entry(name);
		
		if (client == null || users == null) {
			throw new WorkfrontException("You must login before calling this function");
		}
		
		return logger.exit(users.get(name));
	}
	
	private HashMap<String, String> getWorkfrontUsers() throws WorkfrontException {
		logger.entry();
		
		HashMap<String, String> users = new HashMap<String, String>();

		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Workfront.LIMIT, Workfront.MAX_USERS); 
		JSONArray results;
		try {
			results = client.search(Workfront.OBJCODE_USER, params, new String[]{Workfront.ID, Workfront.NAME});

			for (int i=0; i<results.length(); i++) {
				users.put(results.getJSONObject(i).getString(Workfront.NAME), results.getJSONObject(i).getString(Workfront.ID));
			}
		} catch (StreamClientException e) { 
			throw new WorkfrontException(e);
		} catch (JSONException e) { //TODO:
			throw new WorkfrontException(e);
		}

		return logger.exit(users);
	}

	private void addDevTasks(Project project) throws StreamClientException {
		logger.entry(project);
		
		Map<String, Object> searchParams = formatDevTasksSearchParameters(project.getWorkfrontProjectID());
		JSONArray tasks = client.search(Workfront.OBJCODE_TASK, searchParams, TASK_FIELDS);

		for (int i = 0; i < tasks.length(); i++) {
			try {
				project.addDevTask(new Task((JSONObject)tasks.getJSONObject(i)));
			} catch (JSONException e) {
				throw new StreamClientException(e);
			}
		}
		
		logger.exit();
	}

	private String getImplementationTaskID(String projectID) throws JSONException, StreamClientException {
		logger.entry(projectID);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Workfront.PROJECT_ID, projectID);
		map.put(Workfront.SYNC_WITH_JIRA_MOD, Workfront.MOD_NOT_NULL);
		
		JSONArray results = client.search(Workfront.OBJCODE_TASK, map, TASK_FIELDS);
		if(results.length() < 1) {
			throw new StreamClientException("Could not find implementation task for project " + projectID);
		}
		
		return logger.exit(results.getJSONObject(0).getString(Workfront.ID));
	}

	private Map<String, Object> formatDevTasksSearchParameters(String projectID) {
		logger.entry(projectID);
		
		//TODO: Update the search params to find all issues where the Sync With Jira
		//      custom field is not null.
		Map<String, Object> search = new HashMap<String, Object>();
		search.put(Workfront.PROJECT_ID, projectID);
		search.put(Workfront.JIRA_SYNC_TASK_MOD, Workfront.MOD_NOT_NULL);
		
		return logger.exit(search);
	}
	
	private Map<String, Object> formatActiveRequestsSearchParameters(Date lastUpdateStart, Date lastUpdateEnd) {
		logger.entry(lastUpdateStart, lastUpdateEnd);

		Map<String, Object> search = new HashMap<String, Object>();

		// Active requests are found in the New Development Project Request project
		search.put(Workfront.PROJECT_ID, newRequestProjectID);
		
		// If no dates were provided, query for a fresh list of active requests
		// with an opportunity name
		if (lastUpdateStart == null || lastUpdateEnd == null) {
			search.put(Workfront.STATUS, Workfront.STATUS_CLOSED);
			search.put(Workfront.STATUS_MOD, Workfront.MOD_NOT_EQUAL_TO);
		}
		
		else if (lastUpdateStart != null && lastUpdateEnd != null) {
			search.put(Workfront.LAST_UPDATE_DATE, Workfront.dateFormatter.format(lastUpdateStart));
			search.put(Workfront.LAST_UPDATE_DATE_RANGE, Workfront.dateFormatter.format(lastUpdateEnd));
			search.put(Workfront.LAST_UPDATE_DATE_MOD, Workfront.MOD_BETWEEN);
		}
		
		// By default Workfront will only return 100 items in the query
		// Tell Workfront we want them all (or as many as we can get - Workfront has a max of 2,000)
		search.put(Workfront.LIMIT, Workfront.MAX_PROJECTS);
		
		return logger.exit(search);
	}

	private Map<String, Object> formatActiveDevProjectsSearchParameters(Date lastUpdateStart, Date lastUpdateEnd) {
		logger.entry(lastUpdateStart, lastUpdateEnd);
		
		Map<String, Object> search = new HashMap<String, Object>();
		
		// Active development projects can be found in the Development portfolio
		search.put(Workfront.PORTFOLIO_ID, portfolioID);

		// If no dates were provided, query for a fresh list of active projects
		// that are marked to be synced with Jira
		if (lastUpdateStart == null || lastUpdateEnd == null) {
			// Active dev projects are those projects that have a Current status...
			// TODO: sync with any status
//			search.put(Workfront.STATUS, Workfront.STATUS_CURRENT);
			// and have the custom field "Sync With Jira" set to "Yes".
			search.put(Workfront.SYNC_WITH_JIRA, Workfront.YES);
		}
		
		// If dates were provided, find all projects that were changed
		// since the last update.
		else if (lastUpdateStart != null && lastUpdateEnd != null) {
			search.put(Workfront.LAST_UPDATE_DATE, Workfront.dateFormatter.format(lastUpdateStart));
			search.put(Workfront.LAST_UPDATE_DATE_RANGE, Workfront.dateFormatter.format(lastUpdateEnd));
			search.put(Workfront.LAST_UPDATE_DATE_MOD, Workfront.MOD_BETWEEN);
		}
		
		// By default Workfront will only return 100 items in the query
		// Tell Workfront we want them all (or as many as we can get - Workfront has a max of 2,000)
		search.put(Workfront.LIMIT, Workfront.MAX_PROJECTS);
		
		return logger.exit(search);
	}
	
	private String getPortfolioID() throws StreamClientException, JSONException {
		logger.entry();
		return logger.exit(getObjectIdByName(Workfront.OBJCODE_PORTFOLIO, portfolioName));
	}
	
	private String getJiraTaskCustomFormID() throws StreamClientException, JSONException {
		logger.entry();
		return logger.exit(getObjectIdByName(Workfront.OBJCODE_FORM, jiraTaskCustomFormName));
	}

	
	private String getObjectID(String objcode, String objname, String field) throws StreamClientException, JSONException {
		logger.entry(objcode, objname);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(field, objname);
		JSONArray results = client.search(objcode, map, new String[]{"ID", field});
		
		if(results.length() < 1) {
			throw new StreamClientException("Object not found: objcode=" + objcode + ", objname=" + objname + ", field=" + field);
		}
		
		return logger.exit(results.getJSONObject(0).getString("ID"));
	}
	
	private String getObjectIdByName(String objcode, String objname) throws StreamClientException, JSONException {
		logger.entry(objcode, objname);
		return logger.exit(getObjectID(objcode, objname, Workfront.NAME));
	}

	private String getObjectIdByValue(String objcode, String objname) throws StreamClientException, JSONException {
		logger.entry(objcode, objname);
		return logger.exit(getObjectID(objcode, objname, Workfront.VALUE));
	}
}
