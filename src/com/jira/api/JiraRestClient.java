package com.jira.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.axis.encoding.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;


public class JiraRestClient {
	private static final Logger logger = LogManager.getLogger();

	private static final String METHOD_POST = "POST";

	private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private String hostname;
	private String authToken;

	public JiraRestClient (String hostname, String username, String password) {
		this.hostname = hostname;
		this.authToken = Base64.encode(new String(username + ":" + password).getBytes());
	}

	public String createProject(Map<String, Object> params) throws JiraRestAPIException {
		HttpURLConnection conn = null;

		try {
			conn = createConnection(hostname, METHOD_POST);

			// Send request
			Writer out = new OutputStreamWriter(conn.getOutputStream());
			out.write(new JSONObject(params).toString());
			out.flush();
			out.close();
			
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

			// Decode JSON
			JSONObject result = new JSONObject(response.toString());

			// Verify result
			if (result.has(Jira.WARNING_DATA)) {
				JSONObject warning = result.getJSONObject(Jira.WARNING_DATA);
				if (warning.has(Jira.HAS_WARNINGS) && warning.getBoolean(Jira.HAS_WARNINGS)) {
					logger.warn("Project was created with warnings.\n{}", warning.getJSONArray(Jira.WARNINGS).toString());
				}
			}
			else if (!result.has(Jira.SQL_PROJECT_ID)) {
				throw new JiraRestAPIException("Error creating Jira project.");
			}

			return result.getString(Jira.PROJECT_ID);
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
