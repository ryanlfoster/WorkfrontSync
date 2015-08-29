package com.spillman.workfront;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Workfront {
	// Workfront Object Codes
	public static final String OBJCODE_FORM	 			= "ctgy";
	public static final String OBJCODE_HOUR	 			= "hour";
	public static final String OBJCODE_ISSUE			= "optask";
	public static final String OBJCODE_PARAM 			= "param";
	public static final String OBJCODE_POPT 			= "popt";
	public static final String OBJCODE_PORTFOLIO 		= "portfolio";
	public static final String OBJCODE_PROJ 			= "proj";
	public static final String OBJCODE_TASK	 			= "task";
	public static final String OBJCODE_TEMPLATE 		= "template";
	public static final String OBJCODE_USER	 			= "user";

	// Workfront Fields
	public static final String ASSIGNED_TO_ID			= "assignedToID";
	public static final String CUSTOM_FORM_ID			= "categoryID";
	public static final String DESCRIPTION 				= "description";
	public static final String DURATION_EXPRESSION		= "durationExpression";
	public static final String DURATION_MINUTES			= "durationMinutes";
	public static final String DURATION_TYPE			= "durationType";
	public static final String ENTRY_DATE				= "entryDate";
	public static final String FIELDS 					= "fields";
	public static final String HOURS					= "hours";
	public static final String ID 						= "ID";
	public static final String IS_HIDDEN				= "isHidden";
	public static final String JIRA_ISSUE_EPIC_NAME		= "DE:Jira Epic Name";
	public static final String JIRA_ISSUE_ID			= "DE:Jira Issue ID";
	public static final String JIRA_ISSUE_TYPE			= "DE:Jira Issue Type";
	public static final String JIRA_ISSUE_URL			= "DE:Jira Issue URL";
	public static final String JIRA_ISSUENUM			= "DE:Jira Issue Number";
	public static final String JIRA_PROJECT_ID			= "DE:Jira Project ID";
	public static final String JIRA_PROJECT_KEY			= "DE:Jira Project Key";
	public static final String LABEL					= "label";
	public static final String LAST_JIRA_SYNC			= "DE:Last Jira Sync";
	public static final String LAST_UPDATE_DATE			= "lastUpdateDate";
	public static final String LIMIT					= "$$LIMIT";
	public static final String NAME 					= "name";
	public static final String OPPORTUNITY_FLAG			= "DE:Flag Type";
	public static final String OPPORTUNITY_NAME			= "DE:Opportunity Name";
	public static final String OPPORTUNITY_PHASE		= "DE:Sales Phase";
	public static final String OPPORTUNITY_POSITION		= "DE:Position";
	public static final String OPPORTUNITY_PROBABILITY	= "DE:Probability";
	public static final String OPPORTUNITY_STATE		= "DE:Opportunity State";
	public static final String OWNER					= "owner";
	public static final String OWNER_ID					= "ownerID";
	public static final String OWNER_NAME				= "owner:name";
	public static final String PARAMETER_ID				= "parameterID";
	public static final String PARENT_ID				= "parentID";
	public static final String PERCENT_COMPLETE			= "percentComplete";
	public static final String PILOT_AGENCY				= "DE:Pilot Agency";
	public static final String PORTFOLIO_ID				= "portfolioID";
	public static final String PROGRAM					= "program";
	public static final String PROGRAM_NAME				= "program:name";
	public static final String PROJECT_ID				= "projectID";
	public static final String REFNUM					= "refnum";
	public static final String STATUS 					= "status";
	public static final String SYNC_WITH_JIRA			= "DE:Sync With Jira";
	public static final String TASK_ID					= "taskID";
	public static final String UNIQUE_KEY_VIOLATION		= "exception.database.uniquekeyviolation";
	public static final String URL						= "URL";
	public static final String VALUE					= "value";
	public static final String VERSIONS					= "DE:What versions of Spillman will be affected?";
	public static final String WORK_REQ_EXPRESSION		= "workRequiredExpression";
	
	// Workfront Modifiers
	public static final String JIRA_ISSUENUM_MOD		= "DE:Jira Issue Number_Mod";
	public static final String JIRA_ISSUE_ID_MOD		= "DE:Jira Issue ID_Mod";
	public static final String JIRA_SYNC_TASK_MOD		= "DE:Sync Task To Jira_Mod";
	public static final String LAST_UPDATE_DATE_MOD		= "lastUpdateDate_Mod";
	public static final String LAST_UPDATE_DATE_RANGE	= "lastUpdateDate_Range";
	public static final String MOD_BETWEEN				= "between";
	public static final String MOD_NOT_NULL				= "notnull";
	public static final String MOD_NOT_EQUAL_TO			= "ne";
	public static final String STATUS_MOD				= "status_Mod";
	public static final String SYNC_WITH_JIRA_MOD		= "DE:Sync With Jira_Mod";
	
	//Workfront Jira Issue Types
	public static final String JIRA_ISSUE_TYPE_BACKLOG	= "Backlog";
	public static final String JIRA_ISSUE_TYPE_EPIC		= "Epic";
	public static final String JIRA_ISSUE_TYPE_ESTIMATE	= "Estimate";
	public static final String JIRA_ISSUE_TYPE_EXCEPTION= "Exception";
	public static final String JIRA_ISSUE_TYPE_PILOT	= "Pilot";
	
	// Workfront Statuses
	public static final String STATUS_CURRENT 			= "CUR";
	public static final String STATUS_CLOSED 			= "CLS";
	public static final String STATUS_COMPLETE 			= "CPL";

	// Other Workfront Values
	public static final String DURATION_EFFORT_DRIVEN 	= "D";
	public static final String MAX_USERS				= "500";
	public static final String MAX_PROJECTS				= "2000";
	public static final String YES						= "Yes";
	
	// Workfront Date Formatters
	public static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final DateFormat dateFormatterTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSZ");
}
