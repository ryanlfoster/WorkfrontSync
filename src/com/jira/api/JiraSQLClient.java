package com.jira.api;

import java.sql.*;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spillman.common.Task;
import com.spillman.jira.JiraException;

public class JiraSQLClient {
	private static final Logger logger = LogManager.getLogger();

	private final static String EPICS_SUMMARY_SQL = 
			"SELECT PROJECT, Epic_ID, Epic_Issuenum, Epic_Name, Epic_Status, Epic_Estimate, Total_Time_Spent, Total_Story_Points, "
			+ "Total_Subtasks_Time_Spent, Total_Time_Spent_Closed, Total_Story_Points_Closed, Total_Subtasks_Time_Spent_Closed, IssueKey, IssueType "
			+ "FROM EpicsSummary "
			+ "WHERE PROJECT = ?";			

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

	private Connection con = null;
	private PreparedStatement backlogItemsStatement = null;
	private PreparedStatement epicsSummaryStatement = null;
	private PreparedStatement validKeyStatement = null;
	private PreparedStatement validProjectNameStatement = null;
	private PreparedStatement workLogStatement = null;
	private PreparedStatement workLogNoStartDateStatement = null;

	
	public JiraSQLClient(String connectionString) throws JiraException {
		logger.entry(connectionString);
		
		// Establish the connection.
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	    	con = DriverManager.getConnection(connectionString);
		} catch (ClassNotFoundException e) {
			throw new JiraException(e);
		} catch (SQLException e) {
			throw new JiraException(e);
		}
		
		logger.exit();
	}
	
	
	public ArrayList<Task> getEpics(String projectID) throws JiraException {
		// Create an SQL prepared statement.
		if (epicsSummaryStatement == null) {
			try { epicsSummaryStatement = con.prepareStatement(EPICS_SUMMARY_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			epicsSummaryStatement.setInt(1, Integer.parseInt(projectID));
			return processEpics(epicsSummaryStatement.executeQuery());
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public ResultSet getWorkLog(String projectID, Timestamp startTime, Timestamp endTime) throws JiraException {
		// Create an SQL prepared statement.
		if (workLogStatement == null) {
			try { workLogStatement = con.prepareStatement(WORK_LOG_WITH_EPIC_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			workLogStatement.setInt(1, Integer.parseInt(projectID));
			workLogStatement.setTimestamp(2, startTime);
			workLogStatement.setTimestamp(3, endTime);
			return workLogStatement.executeQuery();
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public ResultSet getWorkLog(String projectID, Timestamp endTime) throws JiraException {
		// Create an SQL prepared statement.
		if (workLogNoStartDateStatement == null) {
			try { workLogNoStartDateStatement = con.prepareStatement(WORK_LOG_WITH_EPIC_NO_START_DATE_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			workLogNoStartDateStatement.setInt(1, Integer.parseInt(projectID));
			workLogNoStartDateStatement.setTimestamp(2, endTime);
			return workLogNoStartDateStatement.executeQuery();
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public boolean projectKeyExists(String key) throws JiraException {
		// Create an SQL prepared statement.
		if (validKeyStatement == null) {
			try { validKeyStatement = con.prepareStatement(VALID_KEY_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			validKeyStatement.setString(1, key);
			validKeyStatement.setString(2, key);
			ResultSet rs = validKeyStatement.executeQuery();
			return rs.next(); 
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public boolean projectNameExists(String projectName) throws JiraException {
		// Create an SQL prepared statement.
		if (validProjectNameStatement == null) {
			try { validProjectNameStatement = con.prepareStatement(VALID_PROJECT_NAME_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			validProjectNameStatement.setString(1, projectName);
			ResultSet rs = validProjectNameStatement.executeQuery();
			return rs.next(); 
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public void close() {
		if (backlogItemsStatement != null) try { backlogItemsStatement.close(); } catch(Exception e) {}
		if (con != null) try { con.close(); } catch(Exception e) {}		
	}

	private ArrayList<Task> processEpics(ResultSet rs) throws SQLException {
		logger.entry(rs);
		
		ArrayList<Task> tasks = new ArrayList<Task>();
		
		while (rs.next()) {
			Task task = new Task();

			task.setName(rs.getString(Jira.EPIC_NAME));
			task.setJiraIssueID(Integer.toString(rs.getInt(Jira.EPIC_ID)));
			task.setJiraIssueKey(rs.getString(Jira.ISSUE_KEY));
			task.setJiraIssueType(rs.getString(Jira.ISSUE_TYPE));
//			task.setJiraStatus(rs.getString(Jira.EPIC_STATUS));
//			task.setJiraParentIssuenum(null);

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
		
		return logger.exit(tasks);
	}
}
