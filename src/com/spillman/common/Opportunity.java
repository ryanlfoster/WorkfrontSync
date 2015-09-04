package com.spillman.common;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class Opportunity {
	private String  name;
	private String  crmOpportunityID;
	private Integer probability;
	private String  flag;
	private String  phase;
	private String  position;
	private Integer state;
	
	public Opportunity() {
	}
	
	public Opportunity(String name, String guid) {
		this.name = name;
		this.crmOpportunityID = guid;
	}
	
	public Opportunity(JSONObject request) throws JSONException {
		this.name = request.getStringOrNull(Workfront.OPPORTUNITY_NAME);
		this.crmOpportunityID = request.getStringOrNull(Workfront.OPPORTUNITY_NAME);
		this.flag = request.getStringOrNull(Workfront.OPPORTUNITY_FLAG);
		this.phase = request.getStringOrNull(Workfront.OPPORTUNITY_PHASE);
		this.position = request.getStringOrNull(Workfront.OPPORTUNITY_POSITION);
		if (!request.isNull(Workfront.OPPORTUNITY_STATE)) this.state = request.getInt(Workfront.OPPORTUNITY_STATE);
		if (!request.isNull(Workfront.OPPORTUNITY_PROBABILITY)) this.probability = request.getInt(Workfront.OPPORTUNITY_PROBABILITY);
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("name", name)
			.append("crmOpportunityID", crmOpportunityID)
			.append("probability", probability)
			.append("flag", flag)
			.append("pahse", phase)
			.append("position", position)
			.toString();
	}

	public boolean hasSameStatus(Opportunity opp) {
		return Objects.equals(this.flag, opp.getFlag())
				&& Objects.equals(this.phase, opp.getPhase())
				&& Objects.equals(this.position, opp.getPosition())
				&& Objects.equals(this.probability, opp.getProbability())
				&& Objects.equals(this.state, opp.getState());
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCrmOpportunityID() {
		return crmOpportunityID;
	}

	public void setCrmOpportunityID(String oppID) {
		this.crmOpportunityID = oppID;
	}

	public Integer getProbability() {
		return probability;
	}

	public void setProbability(Integer probability) {
		this.probability = probability;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}
}
