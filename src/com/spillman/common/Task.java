package com.spillman.common;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Task {
	
	private String workfrontTaskID;
	private String workfrontParentTaskID;
	private String jiraIssuenum;
	private String jiraParentIssuenum;
	private String name;
	private String description;
	private String assigneeID;
	private String assigneeName;
	private String workfrontStatus;
	private String jiraStatus;
	private Double duration;
	private Double percentComplete;
	
	public Task() {		
		workfrontTaskID = null;
		workfrontParentTaskID = null;
		jiraIssuenum = null;
		jiraParentIssuenum = null;
		name = null;
		description = null;
		assigneeID = null;
		assigneeName = null;
		workfrontStatus = null;
		jiraStatus = null;
		duration = null;
		percentComplete = null;
	}

	public Task(JSONObject task) throws JSONException {
		// The JSONObject comes from the Workfront API
		// Set the Workfront fields
		workfrontTaskID = task.getString(Workfront.ID);
		workfrontParentTaskID = task.getString(Workfront.PARENT_ID);
		jiraIssuenum = task.getString(Workfront.JIRA_ISSUENUM);
		name = task.getString(Workfront.NAME);
		if (!task.isNull(Workfront.DESCRIPTION)) description = task.getString(Workfront.DESCRIPTION);
		if (!task.isNull(Workfront.ASSIGNED_TO_ID)) assigneeID = task.getString(Workfront.ASSIGNED_TO_ID);
		workfrontStatus = task.getString(Workfront.STATUS);
		duration = (double)task.getLong(Workfront.DURATION_MINUTES) / 60.0;
		percentComplete = task.getDouble(Workfront.PERCENT_COMPLETE);

		// We don't have any information about the Jira fields
		// Set the Jira fields to null
		jiraParentIssuenum = null;
		assigneeName = null;
		jiraStatus = null;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("workfrontTaskID", workfrontTaskID)
			.append("workfrontParentTaskID", workfrontParentTaskID)
			.append("jiraIssuenum", jiraIssuenum)
			.append("jiraParentIssuenum", jiraParentIssuenum)
			.append("name", name)
			.append("description", description)
			.append("assigneeID", assigneeID)
			.append("assigneeName", assigneeName)
			.append("workfrontStatus", workfrontStatus)
			.append("jiraStatus", jiraStatus)
			.append("duration", duration)
			.append("percentComplete", percentComplete)
			.toString();
	}
	
	public String getWorkfrontTaskID() {
		return workfrontTaskID;
	}

	public void setWorkfrontTaskID(String taskID) {
		this.workfrontTaskID = taskID;
	}

	public String getJiraIssuenum() {
		return jiraIssuenum;
	}

	public void setJiraIssuenum(String jiraKey) {
		this.jiraIssuenum = jiraKey;
	}

	public String getJiraParentIssuenum() {
		return jiraParentIssuenum;
	}

	public void setJiraParentIssuenum(String jiraParentIssuenum) {
		this.jiraParentIssuenum = jiraParentIssuenum;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAssigneeID() {
		return assigneeID;
	}

	public void setAssigneeID(String assigneeID) {
		this.assigneeID = assigneeID;
	}

	public String getWorkfrontStatus() {
		return workfrontStatus;
	}

	public void setWorkfrontStatus(String status) {
		this.workfrontStatus = status;
	}

	public String getWorkfrontParentTaskID() {
		return workfrontParentTaskID;
	}

	public void setWorkfrontParentTaskID(String parentTaskID) {
		this.workfrontParentTaskID = parentTaskID;
	}

	public Double getDuration() {
		return duration;
	}

	public void setDuration(Double duration) {
		this.duration = duration;
	}

	public Double getPercentComplete() {
		return percentComplete;
	}

	public void setPercentComplete(Double percentComplete) {
		this.percentComplete = percentComplete;
	}

	public String getAssigneeName() {
		return assigneeName;
	}

	public void setAssigneeName(String assigneeName) {
		this.assigneeName = assigneeName;
	}

	public String getJiraStatus() {
		return jiraStatus;
	}

	public void setJiraStatus(String jiraStatus) {
		this.jiraStatus = jiraStatus;
	}

}
