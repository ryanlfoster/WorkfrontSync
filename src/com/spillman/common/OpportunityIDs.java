package com.spillman.common;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.workfront.Workfront;

public class OpportunityIDs {
	private List<String> ids;
	
	public OpportunityIDs(JSONObject json) throws JSONException {
		ids = new ArrayList<String>();
		
		if (json.has(Workfront.OPPORTUNITIES)) {
			Object object = json.get(Workfront.OPPORTUNITIES);
			if (object instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray)object).length(); i++) {
					ids.add(((JSONArray)object).getString(i));
				}
			} else if (object != JSONObject.NULL) {
				ids.add(object.toString());
			}
		}
	}

}
