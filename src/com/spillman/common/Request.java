package com.spillman.common;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Request {
	private String name;
	private String workfrontRequestID;
	private Opportunity opportunity;
	
	public Request() {
	}

	public Request(String name, String workfrontID, String opportunityName, Integer probability, String flag, String phase, String position) {
		this.name = name;
		this.workfrontRequestID = workfrontID;
	}
	
	public Request(JSONObject request) throws JSONException {
		setName(request.getString(Workfront.NAME));
		setWorkfrontRequestID(request.getString(Workfront.ID));
		setOpportunity(new Opportunity(request));
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

	public Opportunity getOpportunity() {
		return opportunity;
	}

	public void setOpportunity(Opportunity opportunity) {
		this.opportunity = opportunity;
	}

}
