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
	
	public Integer getCombinedProbability(List<String> oppIDs) throws CRMException {
		List<Opportunity> opps = sqlClient.getOpportunities(oppIDs);
		
		if (opps.size() == 1) {
			return opps.get(0).getProbability();
		}
		
		int setSize = opps.size();
		int sumProbability = 0;
		
		for (Opportunity o : opps) {
			sumProbability += o.getProbability();
		}
		
		/*
		 * The formula for the combined probability - the probability that at least one
		 * of the opportunities will happen - is: 
		 * 
		 * opportunity_1_probability + ... + opportunity_n_probability - probability all opportunities will happen
		 */
		//TODO: This formula isn't right.
		int combinedProbability = (int)((double)sumProbability - (sumProbability / setSize));
		return Math.min(100, combinedProbability);
	}
}