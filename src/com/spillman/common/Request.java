package com.spillman.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Request {
	private String name;
	private String workfrontRequestID;
	private Integer combinedProbability;
	private Opportunity opportunity;
	private List<String> opportunityIDs;
	
	public Request() {
	}

	public Request(String name, String workfrontID) {
		this.name = name;
		this.workfrontRequestID = workfrontID;
	}
	
	public Request(JSONObject request) throws JSONException {
		setName(request.getString(Workfront.NAME));
		setWorkfrontRequestID(request.getString(Workfront.ID));
		setOpportunities(request);
		if (!request.isNull(Workfront.COMBINED_PROBABILITY)) {
			setCombinedProbability(request.getInt(Workfront.COMBINED_PROBABILITY));
		}
	}
	
	public String toString() {
		return new ToStringBuilder(this)
				.append("name", name)
				.append("workfrontID", workfrontRequestID)
				.append("opportunity", opportunity)
				.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getWorkfrontRequestID() {
		return workfrontRequestID;
	}

	public void setWorkfrontRequestID(String workfrontID) {
		this.workfrontRequestID = workfrontID;
	}

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
	
	public void setOpportunities(JSONObject request) throws JSONException {
		setOpportunity(new Opportunity(request));

		// Add the ID of the primary opportunity to the list
		opportunityIDs = new ArrayList<String>();
		if (this.opportunity.getCrmOpportunityID() != null) {
			opportunityIDs.add(this.opportunity.getCrmOpportunityID());
		}
		
		// Add the IDs of additional opportunities to the list
		if (request.has(Workfront.ADDITIONAL_OPPORTUNITIES)) {
			setOpportunityIDs(request.get(Workfront.ADDITIONAL_OPPORTUNITIES));
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
