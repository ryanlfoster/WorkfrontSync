package com.spillman.jira;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.axis.encoding.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.spillman.SyncProperties;
import com.spillman.common.Task;


public class JiraRestClient {
	private static final Logger logger = LogManager.getLogger();

	private static final String METHOD_POST = "POST";

	private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private String createProjectUrl;
	private String createIssueUrl;
	private String linkIssueUrl;
	private String browseUrl;
	private String authToken;
	private String epicIssueType;
	private String epicStoryLinkName;
	private HashMap<String,String> issueTypes;

	public JiraRestClient (SyncProperties props) {
		this.createProjectUrl = props.getJiraCreateProjectUrl();
		this.createIssueUrl = props.getJiraCreateIssueUrl();
		this.linkIssueUrl = props.getJiraLinkIssueUrl();
		this.issueTypes = props.getJiraIssueTypes();
		this.epicIssueType = props.getJiraEpicIssueType();
		this.browseUrl = props.getJiraBrowseUrl();
		this.epicStoryLinkName = props.getJiraEpicStoryLinkName();
		
		String username = props.getJiraUsername();
		String password = props.getJiraPassword();
		this.authToken = Base64.encode(new String(username + ":" + password).getBytes());
	}

	public String createProject(Map<String, Object> params) throws JiraRestAPIException {
		JSONObject result = request(createProjectUrl, new JSONObject(params).toString());
		
		// Verify result
		try {
			if (result.has(Jira.WARNING_DATA)) {
				JSONObject warning;
				warning = result.getJSONObject(Jira.WARNING_DATA);
				if (warning.has(Jira.HAS_WARNINGS) && warning.getBoolean(Jira.HAS_WARNINGS)) {
					logger.warn("Project was created with warnings.\n{}", warning.getJSONArray(Jira.WARNINGS).toString());
				}
			} else if (!result.has(Jira.SQL_PROJECT_ID)) {
				throw new JiraRestAPIException("Error creating Jira project.");
			}

			return result.getString(Jira.PROJECT_ID);
		} catch (JSONException e) {
			throw new JiraRestAPIException(e);
		}
	}

	public Task createEpic(String jiraProjectID, String devteam, String name) throws JiraRestAPIException {
		Task task = new Task();
		task.setName(name);
		task.setJiraIssueType(epicIssueType);
		task.setDuration(0.0);
		createIssue(jiraProjectID, devteam, task);
		return task;
	}
	
	public void createIssue(String jiraProjectID, String devteam, Task task) throws JiraRestAPIException {
		// Create the issue
		String params;
		try {
			params = formatParameters(jiraProjectID, devteam, task);
		} catch (JiraRestAPIException e) {
			logger.catching(e);
			return;
		}
		JSONObject result = request(createIssueUrl, params);

		// Very the results
		if (!result.has(Jira.JSON_ID) || !result.has(Jira.JSON_KEY)) {
			throw new JiraRestAPIException("Error creating issue: parameters=" + params.toString() + ", response=" + result.toString());
		}

		try {
			task.setJiraIssueID(result.getString(Jira.JSON_ID));
			task.setJiraIssueUrl(browseUrl + result.getString(Jira.JSON_KEY));
			task.setJiraIssueKey(result.getString(Jira.JSON_KEY));
		} catch (JSONException e) {
			throw new JiraRestAPIException(e);
		}
	}

	public boolean linkIssueToEpic(String issueKey, String epicKey) throws JiraRestAPIException {
		HashMap<String,Object> type = new HashMap<String,Object>();
		HashMap<String,Object> inwardIssue = new HashMap<String,Object>();
		HashMap<String,Object> outwardIssue = new HashMap<String,Object>();
		
		type.put(Jira.JSON_NAME, epicStoryLinkName);
		inwardIssue.put(Jira.JSON_KEY, epicKey);
		outwardIssue.put(Jira.JSON_KEY, issueKey);
		
		HashMap<String,Object> params = new HashMap<String,Object>();
		params.put(Jira.JSON_TYPE,type);
		params.put(Jira.JSON_INWARD_ISSUE, inwardIssue);
		params.put(Jira.JSON_OUTWARD_ISSUE, outwardIssue);

		request(linkIssueUrl, new JSONObject(params).toString());
		return true;
	}
	
	private String formatParameters(String jiraProjectID, String devteam, Task task) throws JiraRestAPIException {
		HashMap<String, Object> project = new HashMap<String, Object>();
		project.put(Jira.JSON_ID, jiraProjectID);

		HashMap<String, Object> issueType = new HashMap<String, Object>();
		if (issueTypes.get(task.getJiraIssueType()) == null) {
			throw new JiraRestAPIException("Unable to create issue because issue type '" + task.getJiraIssueType() + "' is not defined.");
		}
		issueType.put(Jira.JSON_ID, issueTypes.get(task.getJiraIssueType()));
		
		HashMap<String, Object> team = new HashMap<String, Object>();
		team.put(Jira.JSON_VALUE, devteam);
		
		HashMap<String, Object> fields = new HashMap<String, Object>();
		fields.put(Jira.JSON_PROJECT, project);
		fields.put(Jira.JSON_SUMMARY, task.getName());
		fields.put(Jira.JSON_ISSUE_TYPE, issueType);
		fields.put(Jira.JSON_DEV_TEAM, team);

		if (task.getJiraIssueType().equals(epicIssueType)) {
			// Creating an Epic type issue requires the epic name
			fields.put(Jira.JSON_EPIC_NAME, task.getName());
		} 
		else if (task.getJiraIssueType().equals("Pilot")) {
			// Creating a Pilot type issue requires an agency
			if (task.getPilotAgency() == null || task.getPilotAgency().isEmpty()) {
				throw new JiraRestAPIException("Unable to create Pilot issue because no agency was defined.");
			}
			HashMap<String,Object> pilotAgency = new HashMap<String,Object>();
			pilotAgency.put(Jira.JSON_VALUE, task.getPilotAgency());
			List<Object> pilotAgencyList = new ArrayList<Object>();
			pilotAgencyList.add(pilotAgency);
			fields.put(Jira.JSON_PILOT_AGENCY, pilotAgencyList);
		}
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(Jira.JSON_FIELDS, fields);
		
		return new JSONObject(map).toString();
	}

	private JSONObject request(String url, String params) throws JiraRestAPIException {
		HttpURLConnection conn = null;

		try {
			conn = createConnection(url, METHOD_POST);

			// Send request
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
			writer.write(params);
			writer.close();
			wr.close();
			
//			Writer out = new OutputStreamWriter(conn.getOutputStream());
//			out.write(params);
//			out.flush();
//			out.close();
			
			// Read response
			BufferedReader in;
			if (conn.getResponseCode() >= 400) {
				in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			}
			else {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}

			StringBuilder response = new StringBuilder();
			String line;

			while ((line = in.readLine()) != null) {
				response.append(line);
			}

			in.close();

			if (url.equals(linkIssueUrl)) {
				// The Jira API for linking issues doesn't return anything *shrug*
				return null;
			} else {
				// Decode JSON
				return new JSONObject(response.toString());
			}
		}
		catch (Exception e) {
			throw new JiraRestAPIException(e);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private HttpURLConnection createConnection (String spec, String method) throws IOException {
		URL url = new URL(spec);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection) conn).setHostnameVerifier(HOSTNAME_VERIFIER);
		}

		conn.setAllowUserInteraction(false);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setConnectTimeout(60000);
		conn.setReadTimeout(300000);
		conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		conn.setRequestProperty("Authorization", "Basic " + authToken);
		conn.connect();

		return conn;
	}
}

/*-------------------------------------------------------------------------
 * Sample JSON for creating issues in Jira.
 * Note: Only fields on the Jira create screen can be included in the JSON.
 * For example, the original estimate and the time remaining fields are not
 * on the create screen for Epics, therefore they cannot be included in the
 * JSON when when creating an epic.
 *
 *-------------------------------------------------------------------------
 * Sample JSON to create an EPIC using the Jira API
 * customfield_13363 is the Epic Name
 *
{
  "fields": {
    "project": {
      "id": "11660"
    },
    "summary": "Test of the API to create an epic",
    "issuetype": {
      "id": "10"
    },
    "description": "This is a short description",
    "customfield_13363":"Epic 1 from API" 
  }
}

 * RESPONSE:
{
  "id": "86730",
  "key": "ATWSP-12",
  "self": "https://darla.spillman.com/rest/api/2/issue/86730"
}

 *-------------------------------------------------------------------------
 *
 * Sample JSON to create a Backlog using the Jira API
 * customfield_10553 is the Story Points
 *
{
  "fields": {
    "project": {
      "id": "11660"
    },
    "summary": "Test of the API to create a backlog",
    "issuetype": {
      "id": "31"
    },
    "description": "This is a short description",
    "customfield_10553":8 
  }
}

 * RESPONSE:
{
  "id": "86731",
  "key": "ATWSP-13",
  "self": "https://darla.spillman.com/rest/api/2/issue/86731"
} 

 *-------------------------------------------------------------------------
 *
 * Sample JSON to create a Pilot issue using the Jira API
 * customfield_10290 is the agency
 *
{
  "fields": {
    "project": {
      "id": "11660"
    },
    "summary": "Test of the API to create a pilot issue",
    "issuetype": {
      "id": "10100"
    },
    "customfield_10290": [{
        "value":"AZPIMSO"
      }]
  }
}
 *-------------------------------------------------------------------------
 *
 * Sample JSON to link an issue to an epic
 * In this example, link ATWSP-7 as an issue in epic ATWSP-6
 * 
{
  "type": {
    "name": "Epic-Story Link"
  },
  "inwardIssue": {
    "key": "ATWSP-6"
  },
  "outwardIssue": {
    "key": "ATWSP-7"
  },
  "comment": {
    "body": "Linked related issue!",
    "visibility": {
      "type": "group",
      "value": "jira-users"
    }
  }
}

 * RESPONSE:
 * The response is null, and the status code is 201

 *-------------------------------------------------------------------------
 *
 * Sample response from createmeta API call for project 11660
 * https://darla.spillman.com/rest/api/2/issue/createmeta?projectIds=11660
 * 
{
  "expand": "projects",
  "projects": [
    {
      "self": "https://darla.spillman.com/rest/api/2/project/11660",
      "id": "11660",
      "key": "ATWSP",
      "name": "Test Workfront Sync Program",
      "avatarUrls": {
        "16x16": "https://darla.spillman.com/secure/projectavatar?size=xsmall&pid=11660&avatarId=10011",
        "24x24": "https://darla.spillman.com/secure/projectavatar?size=small&pid=11660&avatarId=10011",
        "32x32": "https://darla.spillman.com/secure/projectavatar?size=medium&pid=11660&avatarId=10011",
        "48x48": "https://darla.spillman.com/secure/projectavatar?pid=11660&avatarId=10011"
      },
      "issuetypes": [
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/31",
          "id": "31",
          "description": "",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/improvement.png",
          "name": "Backlog",
          "subtask": false
        },
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/23",
          "id": "23",
          "description": "A problem found in an unreleased product",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/bug.png",
          "name": "Exception",
          "subtask": false
        },
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/10",
          "id": "10",
          "description": "Created by JIRA Agile - do not edit or delete. Issue type for a big user story that needs to be broken down.",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/epic.png",
          "name": "Epic",
          "subtask": false
        },
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/32",
          "id": "32",
          "description": "",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/task_agile.png",
          "name": "Development Task",
          "subtask": true
        },
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/33",
          "id": "33",
          "description": "",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/health.png",
          "name": "Testing Task",
          "subtask": true
        },
        {
          "self": "https://darla.spillman.com/rest/api/2/issuetype/10100",
          "id": "10100",
          "description": "",
          "iconUrl": "https://darla.spillman.com/images/icons/issuetypes/exclamation.png",
          "name": "Pilot",
          "subtask": false
        }
      ]
    }
  ]
}

 *
 *
 */
