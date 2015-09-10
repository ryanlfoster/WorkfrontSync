package com.spillman.common;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.crm.CRM;
import com.spillman.workfront.Workfront;

public class Opportunity implements Comparable<Opportunity>{
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
		this.name = request.getStringOrNull(Workfront.LEAD_OPPORTUNITY);
		this.crmOpportunityID = request.getStringOrNull(Workfront.LEAD_OPPORTUNITY);
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

	@Override
	public int compareTo(Opportunity arg0) {
		
		// First, test the state
		// If the state is the same and the state is either WON or LOST, the opportunities are equal
		if (this.state.equals(arg0.getState()) && (this.state == CRM.STATE_WIN || this.state == CRM.STATE_LOSE)) {
			return 0;
		} 
		// If the states are not the same, a WON opportunity is better
		else if (this.state == CRM.STATE_WIN) {
			return 1;
		} else if (arg0.getState() == CRM.STATE_WIN){
			return -1;
		}
		//If the states are not the same and neither opportunity is WON, then one of them must be LOST
		else if (this.state == CRM.STATE_LOSE) {
			return -1;
		} else if (arg0.getState() == CRM.STATE_LOSE) {
			return 1;
		}
		
		// If we get to here, both opportunities have an OPEN status
		// Next, test the probability
		if (!this.probability.equals(arg0.getProbability())) {
			return (this.probability > arg0.getProbability() ? 1 : -1);
		}
		
		// If we get to here, the probabilities are equal
		// Next, test the flag
		if (!this.flag.equals(arg0.getFlag())) {
			// ASSUMPTION: The flag (which is a string) starts with a number,
			// for example "1 - Committed" or "2 - Back Up" or "3 - Other",
			// and the lower the number the higher/better the flag
			return (-1) * this.flag.compareTo(arg0.getFlag());
		}

		// If we get to here, the two opportunities are equal
		return 0;
	}
}
