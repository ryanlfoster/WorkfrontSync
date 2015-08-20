package com.spillman.common;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class WorkLog {

	private String workfrontTaskID;
	private String jiraIssuenum;
	private String epicIssuenum;
	private Double hoursWorked;
	private String workfrontOwnerID;
	private String jiraWorker;
	private Date dateWorked;
	private String description;
	private String jiraIssueUrl;
	
	public WorkLog() {
		
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("workfrontTaskID", workfrontTaskID)
			.append("jiraIssuenum", jiraIssuenum)
			.append("epicIssuenum", epicIssuenum)
			.append("hoursWorked", hoursWorked)
			.append("workfrontOwnerID", workfrontOwnerID)
			.append("jiraWorker", jiraWorker)
			.append("dateWorked", dateWorked)
			.append("description", description)
			.append("jiraIssueUrl", jiraIssueUrl)
			.toString();
	}
	
	public String getWorkfrontTaskID() {
		return workfrontTaskID;
	}

	public void setWorkfrontTaskID(String workfrontTaskID) {
		this.workfrontTaskID = workfrontTaskID;
	}

	public String getJiraIssuenum() {
		return jiraIssuenum;
	}

	public void setJiraIssuenum(String jiraIssuenum) {
		this.jiraIssuenum = jiraIssuenum;
	}

	public Double getHoursWorked() {
		return hoursWorked;
	}

	public void setHoursWorked(Double hoursWorked) {
		this.hoursWorked = hoursWorked;
	}

	public String getWorkfrontOwnerID() {
		return workfrontOwnerID;
	}

	public void setWorkfrontOwnerID(String workfrontOwnerID) {
		this.workfrontOwnerID = workfrontOwnerID;
	}

	public String getJiraWorker() {
		return jiraWorker;
	}

	public void setJiraWorker(String jiraWorker) {
		this.jiraWorker = jiraWorker;
	}

	public Date getDateWorked() {
		return dateWorked;
	}

	public void setDateWorked(Date dateWorked) {
		this.dateWorked = dateWorked;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEpicIssuenum() {
		return epicIssuenum;
	}

	public void setEpicIssuenum(String epicIssuenum) {
		this.epicIssuenum = epicIssuenum;
	}

	public String getJiraIssueUrl() {
		return jiraIssueUrl;
	}

	public void setJiraIssueUrl(String jiraIssueUrl) {
		this.jiraIssueUrl = jiraIssueUrl;
	}
}
