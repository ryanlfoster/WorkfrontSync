package com.spillman.common;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Project extends OpportunityHolder {
	private static final Logger logger = LogManager.getLogger();
	
	private String workfrontProjectID;
	private String name;
	private String description;
	private String status;
	private String jiraProjectID;
	private String jiraProjectKey;
	private String owner;
	private String implementationTaskID;
	private String jiraDevTeam;
	private String workfrontProgram;
	private String url;
	private List<String> versions;
	private boolean syncWithJira;
	private Date lastJiraSync;
	private HashMap<String,Boolean> specialEpics;
	private HashMap<String,Task> wfDevTasks; // Hashed by the Workfront ID
	private HashMap<String,Task> jiraDevTasks; // Hash by the Jira ID
	
	public Project() {	
	}
	
	public Project(JSONObject project) throws JSONException {
		update(project);

		this.wfDevTasks = new HashMap<String,Task>();
		this.jiraDevTasks = new HashMap<String,Task>();
		this.specialEpics = new HashMap<String,Boolean>();
	}
	
	public void update(JSONObject project) throws JSONException {
		setWorkfrontProjectID(project.getString(Workfront.ID));
		setName(project.getString(Workfront.NAME));
		setStatus(project.getString(Workfront.STATUS));
		setOwner(project.getJSONObject(Workfront.OWNER).getString(Workfront.NAME));
		setJiraProjectID(project.getStringOrNull(Workfront.JIRA_PROJECT_ID));
		setJiraProjectKey(project.getStringOrNull(Workfront.JIRA_PROJECT_KEY));
		setDescription(project.getStringOrNull(Workfront.DESCRIPTION));
		setURL(project.getStringOrNull(Workfront.URL));
		setOpportunities(project);

		if (!project.isNull(Workfront.COMBINED_PROBABILITY)) {
			setCombinedProbability(project.getInt(Workfront.COMBINED_PROBABILITY));
		}

		if (!project.isNull(Workfront.PROGRAM)) {
			setWorkfrontProgram(project.getJSONObject(Workfront.PROGRAM).getString(Workfront.NAME));
		}
		
		if (!project.isNull(Workfront.VERSIONS)) {
			setVersions(getArrayValues(project, Workfront.VERSIONS));
		}

		if (!project.isNull(Workfront.SYNC_WITH_JIRA)) {
			setSyncWithJira(project.getString(Workfront.SYNC_WITH_JIRA).equals(Workfront.YES));
		}
		
		if (!project.isNull(Workfront.LAST_JIRA_SYNC)) {
			try {
				setLastJiraSync(Workfront.dateFormatterTZ.parse(project.getString(Workfront.LAST_JIRA_SYNC)));
			} catch (ParseException e) {
				logger.catching(e);
			}
		}
	}

	public String toString() {
		return new ToStringBuilder(this)
				.append("workfrontProjectID", workfrontProjectID)
				.append("name", name)
				.append("description", description)
				.append("status", status)
				.append("jiraProjectID", jiraProjectID)
				.append("jiraProjectKey", jiraProjectKey)
				.append("owner", owner)
				.append("implementationTaskID", implementationTaskID)
				.append("jiraDevTeam", jiraDevTeam)
				.append("workfrontProgram", workfrontProgram)
				.append("url", url)
				.append("versions", versions)
				.append("syncWithJira", syncWithJira)
				.append("lastJiraSync", lastJiraSync)
				.append("opportunity", opportunity)
				.append("opportunityIDs", opportunityIDs)
				.append("combinedProbability", combinedProbability)
				.append("devTasks", wfDevTasks).toString();
	}

	public String getWorkfrontProjectID() {
		return workfrontProjectID;
	}

	public void setWorkfrontProjectID(String projectID) {
		this.workfrontProjectID = projectID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJiraProjectID() {
		return jiraProjectID;
	}

	public void setJiraProjectID(String projectID) {
		this.jiraProjectID = projectID;
	}

	public boolean hasJiraProjectID() {
		return jiraProjectID != null;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getImplementationTaskID() {
		return implementationTaskID;
	}

	public void setImplementationTaskID(String implementationTaskID) {
		this.implementationTaskID = implementationTaskID;
	}

	public HashMap<String, Task> getWorkfrontDevTasks() {
		return wfDevTasks;
	}

	public void addDevTask(Task newTask) {
		if (newTask.getWorkfrontTaskID() != null) {
			wfDevTasks.put(newTask.getWorkfrontTaskID(), newTask);
		}
		if (newTask.getJiraIssueID() != null) {
			jiraDevTasks.put(newTask.getJiraIssueID(), newTask);
		}
	}
	
	public Task getDevTaskByWorkfrontID(String key) {
		return wfDevTasks.get(key);
	}
	
	public Task getDevTaskByJiraID(String key) {
		return jiraDevTasks.get(key);
	}

	public boolean hasWorkfrontTask(String key) {
		return wfDevTasks.containsKey(key);
	}
	
	public boolean hasJiraTask(String key) {
		return jiraDevTasks.containsKey(key);
	}
	
	public boolean hasSpecialEpic(String key) {
		return specialEpics.containsKey(key);
	}
	
	public void addSpecialEpic(String key) {
		specialEpics.put(key, true);
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getJiraDevTeam() {
		return jiraDevTeam;
	}

	public void setJiraDevTeam(String devTeam) {
		this.jiraDevTeam = devTeam;
	}

	public String getWorkfrontProgram() {
		return workfrontProgram;
	}

	public void setWorkfrontProgram(String workfrontProgram) {
		this.workfrontProgram = workfrontProgram;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public List<String> getVersions() {
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
	}
	
	public void addVersion(String version) {
		if (this.versions == null) {
			versions = new ArrayList<String>();
		}
		versions.add(version);
	}

	public boolean isSyncWithJira() {
		return syncWithJira;
	}

	public void setSyncWithJira(boolean syncWithJira) {
		this.syncWithJira = syncWithJira;
	}

	public Date getLastJiraSync() {
		return lastJiraSync;
	}

	public void setLastJiraSync(Date lastJiraSync) {
		this.lastJiraSync = lastJiraSync;
	}

	public String getJiraProjectKey() {
		return jiraProjectKey;
	}

	public void setJiraProjectKey(String jiraProjectKey) {
		this.jiraProjectKey = jiraProjectKey;
	}

	private List<String> getArrayValues(JSONObject fields, String fieldName) {
		try {
			if (fields.has(fieldName) && !fields.isNull(fieldName)) {
				List<String> values = new ArrayList<String>();
				JSONArray items = fields.optJSONArray(fieldName);
				if (items != null) {
					// Workfront returned an array of strings. Add them to the list.
					for (int i = 0; i < items.length(); i++) {
						values.add(items.getString(i));
					}
				} else {
					// Workfront returned a single string. Add it to the list.
					values.add(fields.getString(fieldName));
				}
				return values;
			} else {
				return null;
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public void setWorkfrontID(String id) {
		setWorkfrontProjectID(id);
	}

	@Override
	public String getWorkfrontID() {
		return getWorkfrontProjectID();
	}

	@Override
	public String getWorkfrontObjectCode() {
		return Workfront.OBJCODE_PROJ;
	}
}
