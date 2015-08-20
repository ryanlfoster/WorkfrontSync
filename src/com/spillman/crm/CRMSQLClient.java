package com.spillman.crm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spillman.common.Account;
import com.spillman.common.Opportunity;


public class CRMSQLClient {
	private static final Logger logger = LogManager.getLogger();

	final private static String ACCOUNT_NAMES_SQL = 
			"SELECT DISTINCT CAST(AC.AccountId as varchar(MAX)) as AccountGUID, AC.Name AS AccountName, AC.st_agencycode AS CustomerCode, AC.Name + ' (' + AC.St_AgencyCode + ')' AS UniqueName "
			+ "FROM Account AC "
			+ "WHERE AC.StatusCode = 1 AND AC.CustomerTypeCode  = 3 AND AC.St_CustomerType in (1,2,4,5,100000000) AND AC.st_DateLeftSpillman IS NULL "
			+ "    AND AC.ModifiedOn >= ?";
	
	final private static String OPPORUNITY_SELECT_STATEMENT =
			"SELECT DISTINCT OP.Name AS OpportunityName, OP.CloseProbability, PHS.Value AS Phase, FLG.Value AS Flag, "
			+ "POS.Value AS Position, CAST(OP.OpportunityId AS varchar(MAX)) AS OpportunityGUID, OP.StateCode "
			+ "FROM     Opportunity AS OP LEFT OUTER JOIN "
                  + "FilteredStringMap AS SRC ON SRC.AttributeName = 'st_source' AND SRC.FilteredViewName = 'filteredopportunity' AND SRC.AttributeValue = OP.St_Source LEFT OUTER JOIN "
                  + "FilteredStringMap AS PHS ON PHS.AttributeName = 'st_salesphase' AND PHS.FilteredViewName = 'filteredopportunity' AND PHS.AttributeValue = OP.St_SalesPhase LEFT OUTER JOIN "
                  + "FilteredStringMap AS FLG ON FLG.AttributeName = 'st_flagtype' AND FLG.FilteredViewName = 'filteredopportunity' AND FLG.AttributeValue = OP.St_FlagType LEFT OUTER JOIN "
                  + "FilteredStringMap AS POS ON POS.AttributeName = 'st_position' AND POS.FilteredViewName = 'filteredopportunity' AND POS.AttributeValue = OP.St_Position LEFT OUTER JOIN "
                  + "FilteredStringMap AS TYP ON TYP.AttributeName = 'st_opportunitytype' AND TYP.FilteredViewName = 'filteredopportunity' AND TYP.AttributeValue = OP.St_OpportunityType LEFT OUTER JOIN "
                  + "FilteredStringMap AS REL ON REL.AttributeName = 'st_relationshiptype' AND REL.FilteredViewName = 'filteredopportunity' AND REL.AttributeValue = OP.St_RelationshipType LEFT OUTER JOIN "
                  + "FilteredStringMap AS OPP ON OPP.AttributeName = 'st_opportunitytype' AND OPP.FilteredViewName = 'filteredopportunity' AND OPP.AttributeValue = OP.St_OpportunityType ";

	final private static String OPEN_OPPORTUNITIES_SQL = OPPORUNITY_SELECT_STATEMENT
            + "WHERE  (OP.AccountIdName NOT LIKE '%Spillman%') AND (OP.StateCode = 0) AND (TYP.Value <> 'Citadex') AND (TYP.Value <> 'Add-on') "
            + "    AND OP.CreatedOn >= ?";

	final private static String CLOSED_OPPORTUNITIES_SQL = OPPORUNITY_SELECT_STATEMENT
            + "WHERE  (OP.AccountIdName NOT LIKE '%Spillman%') AND (OP.StateCode != 0) AND (TYP.Value <> 'Citadex') AND (TYP.Value <> 'Add-on') "
            + "    AND OP.ModifiedOn >= ?";

	final private static String OPPORTUNITY_SQL = OPPORUNITY_SELECT_STATEMENT 
            + "WHERE  OP.OpportunityId = ?";

	private Connection con = null;
	private PreparedStatement accountNamesStatement = null;
	private PreparedStatement openOpportunitiesStatement = null;
	private PreparedStatement closedOpportunitiesStatement = null;
	private PreparedStatement opportunityStatement = null;
	private Calendar gmtcal = null;

	public CRMSQLClient(String connectionString) throws CRMException {
		logger.entry(connectionString);
		
		// Establish the connection.
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	    	con = DriverManager.getConnection(connectionString);
	    	logger.debug("Jira database JDBC conenction string: {}", connectionString);
		} catch (ClassNotFoundException e) {
			throw new CRMException(e);
		} catch (SQLException e) {
			throw new CRMException(e);
		}
		
		gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		
		logger.exit();
	}
	
	public List<Account> getAccountNames(Timestamp createdSince) throws CRMException {
		logger.entry(createdSince);
		
		try {
			if (accountNamesStatement == null) {
				accountNamesStatement = con.prepareStatement(ACCOUNT_NAMES_SQL);
			}
			
			List<Account> codes = new ArrayList<Account>();
			
			accountNamesStatement.setTimestamp(1, createdSince, gmtcal);
			ResultSet rs = accountNamesStatement.executeQuery();
			while (rs.next()) {
				codes.add(new Account(rs.getString(CRM.UNIQUE_ACCOUNT_NAME), rs.getString(CRM.ACCOUNT_GUID)));
			}
			rs.close();
			
			return logger.exit(codes);
		} catch (SQLException e) {
			throw new CRMException(e);
		}
	}

	public List<Opportunity> getOpenOpportunities(Timestamp createdSince) throws CRMException {
		logger.entry(createdSince);
		
		try {
			if (openOpportunitiesStatement == null) {
				openOpportunitiesStatement = con.prepareStatement(OPEN_OPPORTUNITIES_SQL);
			}
			
			List<Opportunity> codes = new ArrayList<Opportunity>();

			openOpportunitiesStatement.setTimestamp(1, createdSince, gmtcal);
			ResultSet rs = openOpportunitiesStatement.executeQuery();
			while (rs.next()) {
				codes.add(createOpportunityFromResultSet(rs));
			}
			rs.close();
			
			return logger.exit(codes);
		} catch (SQLException e) {
			throw new CRMException(e);
		}
	}

	public List<Opportunity> getClosedOpportunities(Timestamp modifiedSince) throws CRMException {
		logger.entry(modifiedSince);
		
		try {
			if (closedOpportunitiesStatement == null) {
				closedOpportunitiesStatement = con.prepareStatement(CLOSED_OPPORTUNITIES_SQL);
			}
			
			List<Opportunity> codes = new ArrayList<Opportunity>();

			closedOpportunitiesStatement.setTimestamp(1, modifiedSince, gmtcal);
			ResultSet rs = closedOpportunitiesStatement.executeQuery();
			while (rs.next()) {
				codes.add(createOpportunityFromResultSet(rs));
			}
			rs.close();
			
			return logger.exit(codes);
		} catch (SQLException e) {
			throw new CRMException(e);
		}
	}

	public Opportunity getOpportunity(String id) throws CRMException {
		logger.entry(id);
		
		try {
			if (opportunityStatement == null) {
				opportunityStatement = con.prepareStatement(OPPORTUNITY_SQL);
			}
			
			opportunityStatement.setString(1, id);
			ResultSet rs = opportunityStatement.executeQuery();
			Opportunity opp = null;
			if (rs.next()) {
				opp = createOpportunityFromResultSet(rs);
			}
			else {
				logger.trace("The query didn't return any results.");
			}
			rs.close();
			return logger.exit(opp);
		} catch (SQLException e) {
			throw new CRMException(e);
		}
			
	}
	
	private Opportunity createOpportunityFromResultSet(ResultSet rs) throws SQLException {
		Opportunity o = new Opportunity();
		o.setCrmOpportunityID(rs.getString(CRM.OPPORTUNITY_GUID));
		o.setName(rs.getString(CRM.OPPORTUNITY_NAME));
		o.setPhase(rs.getString(CRM.OPPORTUNITY_PHASE));
		o.setProbability(rs.getInt(CRM.OPPORTUNITY_PROBABILITY));
		o.setFlag(rs.getString(CRM.OPPORTUNITY_FLAG));
		o.setPosition(rs.getString(CRM.OPPORTUNITY_POSITION));
		o.setState(rs.getInt(CRM.OPPORTUNITY_STATE));
		return o;
	}

}