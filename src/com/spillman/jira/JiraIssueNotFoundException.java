package com.spillman.jira;


@SuppressWarnings("serial")
public class JiraIssueNotFoundException extends JiraException {
	
	public JiraIssueNotFoundException(String message) {
		super(message);
	}
	
	public JiraIssueNotFoundException(Throwable cause) {
		super(cause);
	}

}
