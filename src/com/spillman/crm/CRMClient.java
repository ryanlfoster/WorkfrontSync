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
	
	public Opportunity getLeadingOpportunity(List<String> oppIDs) throws CRMException {
		List<Opportunity> opps = sqlClient.getOpportunities(oppIDs);

		Opportunity leadopp = null;
		for (Opportunity o : opps) {
			if (leadopp == null) {
				leadopp = o;
			}
			else if (leadopp.compareTo(o) < 0) {
				leadopp = o;
			}
		}
		
		return leadopp;
	}
	
	public Integer getCombinedProbability(List<String> oppIDs) throws CRMException {
		List<Opportunity> opps = sqlClient.getOpportunities(oppIDs);
		
		/*
		 * The formula for the combined probability - the probability that at least one
		 * of the opportunities will happen - is:
		 * 
		 *   combined_probability (cp) = 1 - the probability that none of them will happen
		 *   
		 * The probability that an opportunity will be lost is:
		 * 
		 *    probability_lose (pl) = 1 - the probability the opportunity will be won 
		 * 
		 * The probability that all the opportunities will be lost is:
		 * 
		 *   probablity_lose_all (pla) = pl_1 X pl_2 X ... X pl_n
		 * 
		 */

		double pla = 1.0;
		
		for (Opportunity o : opps) {
			// convert probability to a decimal
			double pw = (double)o.getProbability() / 100.0;
			double pl = 1 - pw;
			pla = pla * pl;
		}
		
		double cp = 1 - pla;
		
		return (int)(cp * 100);
	}
}