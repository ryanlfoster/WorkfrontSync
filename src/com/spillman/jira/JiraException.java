package com.spillman.jira;

@SuppressWarnings("serial")
public class JiraException extends Exception {
	public JiraException(Throwable t) {
		super(t);
	}
	
	public JiraException(String message) {
		super(message);
	}
}
