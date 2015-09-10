package com.spillman.jira;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.spillman.common.Account;
import com.spillman.common.Task;
import com.spillman.common.WorkLog;

public class JiraSQLClient {
	private final static String EPICS_QUERY = 
			"SELECT PROJECT, Epic_ID, Epic_Issuenum, Epic_Name, Epic_Status, Epic_Estimate, Total_Time_Spent, Total_Story_Points, "
			+ "Total_Subtasks_Time_Spent, Total_Time_Spent_Closed, Total_Story_Points_Closed, Total_Subtasks_Time_Spent_Closed, IssueKey, IssueType "
			+ "FROM EpicsSummary ";
	
	private final static String EPICS_SUMMARY_SQL = EPICS_QUERY 
			+ "WHERE PROJECT = ?";			

	private final static String EPIC_SQL = EPICS_QUERY 
			+ "WHERE EPIC_ID = ?";			

	private final static String WORK_LOG_WITH_EPIC_SQL =
			"SELECT ID, issueid, ProjectID, Issuenum, DateWorked, HoursWorked, Worker, Description, Epic_Issuenum, IssueKey, Epic_ID "
			+ "FROM     ProjectWorkLogWithEpic "
			+ "WHERE  (ProjectID = ?) AND (CREATED >= ?) AND (CREATED < ?)";
	
	private final static String WORK_LOG_WITH_EPIC_NO_START_DATE_SQL =
			"SELECT ID, issueid, ProjectID, Issuenum, DateWorked, HoursWorked, Worker, Description, Epic_Issuenum, IssueKey, Epic_ID "
			+ "FROM     ProjectWorkLogWithEpic "
			+ "WHERE  (ProjectID = ?) AND (CREATED < ?)";
	
	private final static String VALID_KEY_SQL =
			"SELECT [pname] "
			+ "FROM [jira].[project] "
			+ "WHERE pkey = ? OR ORIGINALKEY = ?";

	private final static String VALID_PROJECT_NAME_SQL =
			"SELECT [pname] "
			+ "FROM [jira].[project] "
			+ "WHERE pname = ?";
	
	private final static String EPIC_ID_SQL =
			"SELECT jira.jiraissue.ID, jira.project.pkey + '-' + CONVERT (varchar, jira.jiraissue.issuenum) as pkey "
			+ "FROM     jira.jiraissue INNER JOIN "
			+ "                  jira.project ON jira.jiraissue.PROJECT = jira.project.ID "
			+ "WHERE jira.jiraissue.issuetype = 10 AND jira.jiraissue.SUMMARY LIKE ? AND jira.jiraissue.PROJECT = ?";
	
	private final static String PILOT_AGENCIES_SQL = 
			"SELECT [ID], [customvalue] AS AgencyCode, [disabled] "
			+ "		  FROM [jira].[customfieldoption] "
			+ "		  WHERE [CUSTOMFIELD] = 10290  "
			+ "		  ORDER BY [SEQUENCE]";
	
	private final static String ISSUE_SQL = 
			"SELECT [ID], [Summary], [IssueType], [IssueKey], [IssueStatus] "
			+ "		  FROM [dbo].[IssueSummary] "
			+ "		  WHERE [ID] = ?";

	private Connection con = null;
	private Statement pilotAgencyStatement = null;
	private PreparedStatement backlogItemsStatement = null;
	private PreparedStatement epicIdStatement = null;
	private PreparedStatement epicStatement = null;
	private PreparedStatement epicsSummaryStatement = null;
	private PreparedStatement issueStatement = null;
	private PreparedStatement validKeyStatement = null;
	private PreparedStatement validProjectNameStatement = null;
	private PreparedStatement workLogStatement = null;
	private PreparedStatement workLogNoStartDateStatement = null;

	
	public JiraSQLClient(String connectionString) throws JiraException {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	    	con = DriverManager.getConnection(connectionString);
		} catch (ClassNotFoundException e) {
			throw new JiraException(e);
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public List<Account> getPilotAgencies() throws JiraException {
		if (pilotAgencyStatement == null) {
			try { pilotAgencyStatement = con.createStatement(); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		List<Account> accounts = new ArrayList<Account>();
		
		try {
			ResultSet rs = pilotAgencyStatement.executeQuery(PILOT_AGENCIES_SQL);
			while (rs.next()) {
				accounts.add(new Account(null, null, rs.getString(Jira.AGENCY_CODE)));
			}
			return accounts;
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public String getEpicKey(String name, String projectID) throws JiraException {
		if (epicIdStatement == null) {
			try { epicIdStatement = con.prepareStatement(EPIC_ID_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			epicIdStatement.setString(1, name);
			epicIdStatement.setInt(2, Integer.parseInt(projectID));
			ResultSet rs = epicIdStatement.executeQuery();
			if (rs.next()) {
				return rs.getString(Jira.PKEY);
			} else {
				throw new JiraIssueNotFoundException("No epic foud for name " + name + " in project " + projectID);
			}
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public ArrayList<Task> getEpics(String projectID) throws JiraException {
		if (epicsSummaryStatement == null) {
			try { epicsSummaryStatement = con.prepareStatement(EPICS_SUMMARY_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			epicsSummaryStatement.setInt(1, Integer.parseInt(projectID));
			return processEpics(epicsSummaryStatement.executeQuery());
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public Task getEpic(String issueID) throws JiraException {
		if (epicStatement == null) {
			try { epicStatement = con.prepareStatement(EPIC_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			epicStatement.setInt(1, Integer.parseInt(issueID));
			ArrayList<Task> epics = processEpics(epicStatement.executeQuery());
			if (epics.size() < 1) {
				throw new JiraIssueNotFoundException("No epic foud for issueID " + issueID);
			}
			return epics.get(0);
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public Task getIssue(String issueID) throws JiraException {
		if (issueStatement == null) {
			try { issueStatement = con.prepareStatement(ISSUE_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			issueStatement.setInt(1, Integer.parseInt(issueID));
			ResultSet rs = issueStatement.executeQuery();
			Task task = processTask(rs);
			if (task == null) {
				throw new JiraIssueNotFoundException("No issue found for issueID " + issueID);
			}
			return task; 
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}


	public ArrayList<WorkLog> getWorkLog(String projectID, Timestamp startTime, Timestamp endTime) throws JiraException {
		if (workLogStatement == null) {
			try { workLogStatement = con.prepareStatement(WORK_LOG_WITH_EPIC_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			workLogStatement.setInt(1, Integer.parseInt(projectID));
			workLogStatement.setTimestamp(2, startTime);
			workLogStatement.setTimestamp(3, endTime);
			return processWorkLogEntries(workLogStatement.executeQuery());
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public ArrayList<WorkLog> getWorkLog(String projectID, Timestamp endTime) throws JiraException {
		if (workLogNoStartDateStatement == null) {
			try { workLogNoStartDateStatement = con.prepareStatement(WORK_LOG_WITH_EPIC_NO_START_DATE_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			workLogNoStartDateStatement.setInt(1, Integer.parseInt(projectID));
			workLogNoStartDateStatement.setTimestamp(2, endTime);
			return processWorkLogEntries(workLogNoStartDateStatement.executeQuery());
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public boolean projectKeyExists(String key) throws JiraException {
		if (validKeyStatement == null) {
			try { validKeyStatement = con.prepareStatement(VALID_KEY_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			validKeyStatement.setString(1, key);
			validKeyStatement.setString(2, key);
			ResultSet rs = validKeyStatement.executeQuery();
			boolean retval = rs.next();
			rs.close();
			return retval; 
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public boolean projectNameExists(String projectName) throws JiraException {
		if (validProjectNameStatement == null) {
			try { validProjectNameStatement = con.prepareStatement(VALID_PROJECT_NAME_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		try {
			validProjectNameStatement.setString(1, projectName);
			ResultSet rs = validProjectNameStatement.executeQuery();
			boolean retval = rs.next();
			rs.close();
			return retval; 
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public void close() {
		if (backlogItemsStatement != null) try { backlogItemsStatement.close(); } catch(Exception e) {}
		if (con != null) try { con.close(); } catch(Exception e) {}		
	}

	private ArrayList<Task> processEpics(ResultSet rs) throws SQLException {
		ArrayList<Task> tasks = new ArrayList<Task>();
		
		while (rs.next()) {
			Task task = new Task();

			task.setName(rs.getString(Jira.EPIC_NAME));
			task.setJiraIssueID(Integer.toString(rs.getInt(Jira.EPIC_ID)));
			task.setJiraIssueKey(rs.getString(Jira.ISSUE_KEY));
			task.setJiraIssueType(rs.getString(Jira.ISSUE_TYPE));

			/*
			 Algorithm for calculating duration and percent complete:
			 - Assumption: all the results are epics
			 - Assumption: values in the result are a summary of all stories and subtasks in the epic
			 
			 If the completed story points is zero use the original estimate of the epic as the duration
			 Otherwise calculate a velocity using time spent on completed stories and story points completed (i.e., time_spent / points_completed)
			 and apply that velocity to the remaining points to calculate the duration (i.e., velocity * remaining points + time_spent)
			 
			 If the status of the epic is "Done", then set the percent complete to 100%
			 Otherwise, calculate the percent complete as completed_story_points / total_story_points
			 */
			Double epicEstimate = rs.getDouble(Jira.EPIC_ESTIMATE);
			Double totalStoryPoints = rs.getDouble(Jira.TOTAL_STORY_POINTS);
			Double timeSpentClosedStories = rs.getDouble(Jira.TOTAL_TIME_SPENT_CLOSED);
			Double storyPointsClosedStories = rs.getDouble(Jira.TOTAL_STORY_POINTS_CLOSED);
			Double subtasksTimeSpentClosedStories = rs.getDouble(Jira.TOTAL_SUBTASKS_TIME_SPENT_CLOSED);
			
			Double duration;
			Double percentComplete;
			if (storyPointsClosedStories <= 0) {
				duration = epicEstimate;
				percentComplete = 0.0;
			}
			else {
				Double velocity = timeSpentClosedStories / storyPointsClosedStories;
				duration = velocity * (totalStoryPoints - storyPointsClosedStories) + timeSpentClosedStories + subtasksTimeSpentClosedStories;
				percentComplete = 100 * storyPointsClosedStories / totalStoryPoints;
				
				// Round the duration and percent complete to the nearest whole number
				duration = (double)Math.round(duration);
				percentComplete = (double)Math.round(percentComplete);
			}
			
			if (rs.getString(Jira.EPIC_STATUS).equals(Jira.EPIC_STATUS_DONE)) {
				percentComplete = 100.0;
			}
			
			task.setDuration(duration);
			task.setPercentComplete(percentComplete);
			
			tasks.add(task);
		}
		
		return tasks;
	}

	
	private Task processTask(ResultSet rs)	throws SQLException, JiraIssueNotFoundException {
		Task task = null;
		if (rs.next()) {
			task = new Task();
			task.setName(rs.getString(Jira.SUMMARY));
			task.setJiraIssueID(Integer.toString(rs.getInt(Jira.SQL_ID)));
			task.setJiraIssueKey(rs.getString(Jira.ISSUE_KEY));
			task.setJiraIssueType(rs.getString(Jira.ISSUE_TYPE));
			if (rs.getString(Jira.ISSUE_STATUS).equals(Jira.ISSUE_STATUS_CLOSED)) {
				task.setPercentComplete(100.0);
			} else {
				task.setPercentComplete(0.0);
			}
		}
		return task;
	}
	
	
	private ArrayList<WorkLog> processWorkLogEntries(ResultSet rs) throws SQLException {
		ArrayList<WorkLog> worklog = new ArrayList<WorkLog>();
		
		while (rs.next()) {
			WorkLog wl = new WorkLog();
			wl.setDateWorked(rs.getTimestamp(Jira.DATE_WORKED));
			wl.setDescription(rs.getString(Jira.DESCRIPTION));
			wl.setHoursWorked(rs.getDouble(Jira.HOURS_WORKED));
			wl.setJiraIssuenum(rs.getString(Jira.ISSUENUM));
			wl.setJiraWorker(rs.getString(Jira.WORKER));
			wl.setEpicIssuenum(rs.getString(Jira.EPIC_ID));
			wl.setJiraIssueKey(rs.getString(Jira.ISSUE_KEY));
			worklog.add(wl);
		}
		
		return worklog;
	}
}
