package com.spillman.crm;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spillman.common.Account;
import com.spillman.common.Opportunity;

public class CRMClient {
	private static final Logger logger = LogManager.getLogger();
	
	private CRMSQLClient sqlClient;
	
	public CRMClient(String jdbcConnectionString) throws CRMException {
		logger.entry(jdbcConnectionString);
		sqlClient = new CRMSQLClient(jdbcConnectionString);
		logger.exit();
	}

	public List<Account> getNewAccounts(Date createdSince) throws CRMException {
		logger.entry();
		List<Account> accounts = sqlClient.getAccountNames(new java.sql.Timestamp(createdSince.getTime()));
		return logger.exit(accounts);
	}

	public List<Opportunity> getNewOpportunities(Date createdSince) throws CRMException {
		logger.entry();
		List<Opportunity> opportunities = sqlClient.getOpenOpportunities(new java.sql.Timestamp(createdSince.getTime()));
		return logger.exit(opportunities);
	}
	
	public List<Opportunity> getClosedOpportunities(Date modifiedSince) throws CRMException {
		logger.entry();
		List<Opportunity> opportunities = sqlClient.getClosedOpportunities(new java.sql.Timestamp(modifiedSince.getTime()));
		return logger.exit(opportunities);
	}
	
	public Opportunity getOpportunity(Opportunity o) throws CRMException {
		logger.entry(o);
		Opportunity opportunity = sqlClient.getOpportunity(o.getCrmOpportunityID());
		return logger.exit(opportunity);
	}
}