package com.spillman.jira;

@SuppressWarnings("serial")
public class MissingDevTeamException extends JiraException {

	public MissingDevTeamException(String message) {
		super(message);
	}
	
	public MissingDevTeamException(Throwable t) {
		super(t);
	}

}
