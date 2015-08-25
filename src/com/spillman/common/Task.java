package com.spillman.common;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Task {
	
	private String workfrontTaskID;
	private String workfrontParentTaskID;
	private String jiraIssueID;
//	private String jiraParentIssuenum;
	private String jiraIssueType;
	private String jiraIssueUrl;
	private String jiraIssueKey;
	private String name;
	private String description;
	private String assigneeID;
//	private String assigneeName;
	private String workfrontStatus;
//	private String jiraStatus;
	private Double duration;
	private Double percentComplete;
	
	public Task() {		
	}

	public Task(JSONObject task) throws JSONException {
		// The JSONObject comes from the Workfront API
		// Parse the Workfront fields
		workfrontTaskID = task.getStringOrNull(Workfront.ID);
		workfrontParentTaskID = task.getStringOrNull(Workfront.PARENT_ID);
		jiraIssueID = task.getStringOrNull(Workfront.JIRA_ISSUE_ID);
		jiraIssueType = task.getStringOrNull(Workfront.JIRA_ISSUE_TYPE);
		jiraIssueUrl = task.getStringOrNull(Workfront.JIRA_ISSUE_URL);
		name = task.getStringOrNull(Workfront.NAME);
		description = task.getStringOrNull(Workfront.DESCRIPTION);
		assigneeID = task.getStringOrNull(Workfront.ASSIGNED_TO_ID);
		workfrontStatus = task.getStringOrNull(Workfront.STATUS);
		duration = (double)task.getLong(Workfront.DURATION_MINUTES) / 60.0;
		percentComplete = task.getDouble(Workfront.PERCENT_COMPLETE);
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("workfrontTaskID", workfrontTaskID)
			.append("workfrontParentTaskID", workfrontParentTaskID)
			.append("jiraIssuenum", jiraIssueID)
//			.append("jiraParentIssuenum", jiraParentIssuenum)
			.append("name", name)
			.append("description", description)
			.append("assigneeID", assigneeID)
//			.append("assigneeName", assigneeName)
			.append("workfrontStatus", workfrontStatus)
//			.append("jiraStatus", jiraStatus)
			.append("duration", duration)
			.append("percentComplete", percentComplete)
			.toString();
	}
	
	public boolean equals(Task task) {
		return (
		// Check the assignee
//		!Objects.equals(this.getAssigneeName(), task.getAssigneeName()) ||
		// Check the parent
//		!Objects.equals(this.getJiraParentIssuenum(), task.getJiraParentIssuenum()) ||
		// Check the duration
		!Objects.equals(this.getDuration(), task.getDuration()) ||
		// Check the percent complete
		!Objects.equals(this.getPercentComplete(), task.getPercentComplete()) ||
		// Check the name
		!Objects.equals(this.getName(), task.getName()) ||
		// Check the description
		!Objects.equals(this.getDescription(), task.getDescription())
		);				
	}
	
	public String getWorkfrontTaskID() {
		return workfrontTaskID;
	}

	public void setWorkfrontTaskID(String taskID) {
		this.workfrontTaskID = taskID;
	}

	public String getJiraIssueID() {
		return jiraIssueID;
	}

	public void setJiraIssueID(String id) {
		this.jiraIssueID = id;
	}

//	public String getJiraParentIssuenum() {
//		return jiraParentIssuenum;
//	}
//
//	public void setJiraParentIssuenum(String jiraParentIssuenum) {
//		this.jiraParentIssuenum = jiraParentIssuenum;
//	}
//
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

//	public String getAssigneeName() {
//		return assigneeName;
//	}
//
//	public void setAssigneeName(String assigneeName) {
//		this.assigneeName = assigneeName;
//	}
//
//	public String getJiraStatus() {
//		return jiraStatus;
//	}
//
//	public void setJiraStatus(String jiraStatus) {
//		this.jiraStatus = jiraStatus;
//	}
//
	public String getJiraIssueType() {
		return jiraIssueType;
	}

	public void setJiraIssueType(String jiraIssueType) {
		this.jiraIssueType = jiraIssueType;
	}

	public String getJiraIssueUrl() {
		return jiraIssueUrl;
	}

	public void setJiraIssueUrl(String jiraIssueUrl) {
		this.jiraIssueUrl = jiraIssueUrl;
	}

	public String getJiraIssueKey() {
		return jiraIssueKey;
	}

	public void setJiraIssueKey(String jiraIssueKey) {
		this.jiraIssueKey = jiraIssueKey;
	}

}
