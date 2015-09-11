package com.spillman.common;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Request extends OpportunityHolder {
	private String name;
	private String workfrontRequestID;
	
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

	@Override
	public void setWorkfrontID(String id) {
		setWorkfrontRequestID(id);
	}

	@Override
	public String getWorkfrontID() {
		return getWorkfrontRequestID();
	}

	@Override
	public String getWorkfrontObjectCode() {
		return Workfront.OBJCODE_ISSUE;
	}
}
