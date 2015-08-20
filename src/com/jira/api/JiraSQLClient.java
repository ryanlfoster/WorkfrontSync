package com.jira.api;

import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spillman.jira.JiraException;

public class JiraSQLClient {
	private static final Logger logger = LogManager.getLogger();

	private final static String EPICS_SUMMARY_SQL = 
			"SELECT PROJECT, Epic_ID, Epic_Issuenum, Epic_Name, Epic_Status, Epic_Estimate, Total_Time_Spent, Total_Story_Points, "
			+ "Total_Subtasks_Time_Spent, Total_Time_Spent_Closed, Total_Story_Points_Closed, Total_Subtasks_Time_Spent_Closed "
			+ "FROM EpicsSummary "
			+ "WHERE PROJECT = ?";			

	private final static String WORK_LOG_WITH_EPIC_SQL =
			"SELECT ID, issueid, ProjectID, Issuenum, DateWorked, HoursWorked, Worker, Description, Epic_Issuenum, IssueKey "
			+ "FROM     ProjectWorkLogWithEpic "
			+ "WHERE  (ProjectID = ?) AND (DateWorked >= ?) AND (DateWorked < ?)";
	
	private final static String WORK_LOG_WITH_EPIC_NO_START_DATE_SQL =
			"SELECT ID, issueid, ProjectID, Issuenum, DateWorked, HoursWorked, Worker, Description, Epic_Issuenum "
			+ "FROM     ProjectWorkLogWithEpic "
			+ "WHERE  (ProjectID = ?) AND (DateWorked < ?)";
	
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
		// Establish the connection.
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	    	con = DriverManager.getConnection(connectionString);
	    	logger.debug("Jira database JDBC conenction string: {}", connectionString);
		} catch (ClassNotFoundException e) {
			throw new JiraException(e);
		} catch (SQLException e) {
			throw new JiraException(e);
		}
	}
	
	
	public ResultSet getEpics(String projectID) throws JiraException {
		// Create an SQL prepared statement.
		if (epicsSummaryStatement == null) {
			try { epicsSummaryStatement = con.prepareStatement(EPICS_SUMMARY_SQL); } 
			catch (SQLException e) { throw new JiraException(e); }
		}

		// Provide values to the prepared statement
		try {
			epicsSummaryStatement.setInt(1, Integer.parseInt(projectID));
			return epicsSummaryStatement.executeQuery();
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
}
