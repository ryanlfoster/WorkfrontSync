package com.jira.api;

import com.spillman.jira.JiraException;

@SuppressWarnings("serial")
public class JiraIssueNotFoundException extends JiraException {
	
	public JiraIssueNotFoundException(String message) {
		super(message);
	}
	
	public JiraIssueNotFoundException(Throwable cause) {
		super(cause);
	}

}
