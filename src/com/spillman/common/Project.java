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

public class Project {
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
	private HashMap<String, Task> devTasks;
	
	public Project() {	
	}
	
	public Project(JSONObject project) throws JSONException {
		setWorkfrontProjectID(project.getString(Workfront.ID));
		setName(project.getString(Workfront.NAME));
		setStatus(project.getString(Workfront.STATUS));
		setOwner(project.getJSONObject(Workfront.OWNER).getString(Workfront.NAME));
		setImplementationTaskID(null);
		setDevTasks(null);

		if (project.isNull(Workfront.JIRA_PROJECT_ID)) {
			setJiraProjectID(null);
		} else {
			setJiraProjectID(project.getString(Workfront.JIRA_PROJECT_ID));
		}
		
		if (project.isNull(Workfront.JIRA_PROJECT_KEY)) {
			setJiraProjectKey(null);
		} else {
			setJiraProjectKey(project.getString(Workfront.JIRA_PROJECT_KEY));
		}
		
		if (project.isNull(Workfront.DESCRIPTION)) {
			setDescription("");
		} else {
			setDescription(project.getString(Workfront.DESCRIPTION));
		}
		
		if (project.isNull(Workfront.PROGRAM)) {
			setWorkfrontProgram(null);
		} else {
			setWorkfrontProgram(project.getJSONObject(Workfront.PROGRAM).getString(Workfront.NAME));
		}
		
		if (project.isNull(Workfront.URL)) {
			setURL(null);
		} else {
			setURL(project.getString(Workfront.URL));
		}
		
		if (project.isNull(Workfront.VERSIONS)) {
			setVersions(null);
		} else {
			setVersions(getArrayValues(project, Workfront.VERSIONS));
		}

		if (project.isNull(Workfront.SYNC_WITH_JIRA)) {
			setSyncWithJira(false);
		} else {
			setSyncWithJira(project.getString(Workfront.SYNC_WITH_JIRA).equals("Yes"));
		}
		
		if (project.isNull(Workfront.LAST_JIRA_SYNC)) {
			setLastJiraSync(null);
		} else {
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
				.append("devTasks", devTasks).toString();
	}

	public String getWorkfrontParentTaskID(Task task) {
		// Lookup up the parent task using the parentJiraIssuenum
		Task t = devTasks.get(task.getJiraParentIssuenum());
		if (t != null) {
			return t.getWorkfrontTaskID();
		}
		else {
			return null;
		}
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

	public HashMap<String, Task> getDevTasks() {
		return devTasks;
	}

	public void setDevTasks(HashMap<String, Task> devTasks) {
		this.devTasks = devTasks;
	}

	public void addDevTask(Task newTask) {
		if (newTask.getWorkfrontTaskID() != null) {
			devTasks.put(newTask.getWorkfrontTaskID(), newTask);
		}
		if (newTask.getJiraIssuenum() != null) {
			devTasks.put(newTask.getJiraIssuenum(), newTask);
		}
	}
	
	public Task getDevTask(String key) {
		return devTasks.get(key);
	}
	
	public boolean hasTask(String key) {
		return devTasks.containsKey(key);
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

	public boolean getSyncWithJira() {
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
}
