package com.spillman.jira;

@SuppressWarnings("serial")
public class JiraRestAPIException extends Exception {
	
	public JiraRestAPIException(String message) {
		super(message);
	}
	
	public JiraRestAPIException(Throwable cause) {
		super(cause);
	}

}
