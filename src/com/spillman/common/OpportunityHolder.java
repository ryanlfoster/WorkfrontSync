package com.spillman.common;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public abstract class OpportunityHolder implements WorkfrontObject {
	protected Opportunity opportunity;
	protected List<String> opportunityIDs;
	protected Integer combinedProbability;


	public Integer getCombinedProbability() {
		return combinedProbability;
	}

	public void setCombinedProbability(Integer combinedProbability) {
		this.combinedProbability = combinedProbability;
	}

	public Opportunity getOpportunity() {
		return opportunity;
	}

	public void setOpportunity(Opportunity opportunity) {
		this.opportunity = opportunity;
	}
	
	public List<String> getAllOpportunityIDs() {
		return opportunityIDs;
	}
	
	public void setOpportunities(JSONObject project) throws JSONException {
		setOpportunity(new Opportunity(project));

		// Add the ID of the primary opportunity to the list
		opportunityIDs = new ArrayList<String>();
		if (this.opportunity.getCrmOpportunityID() != null) {
			opportunityIDs.add(this.opportunity.getCrmOpportunityID());
		}
		
		// Add the IDs of additional opportunities to the list
		if (project.has(Workfront.OPPORTUNITIES)) {
			setOpportunityIDs(project.get(Workfront.OPPORTUNITIES));
		}
		
	}
	
	private void setOpportunityIDs(Object object) throws JSONException {
		if (object instanceof JSONArray) {
			for (int i = 0; i < ((JSONArray)object).length(); i++) {
				opportunityIDs.add(((JSONArray)object).getString(i));
			}
		} else if (object != JSONObject.NULL) {
			opportunityIDs.add(object.toString());
		}
	}
}
