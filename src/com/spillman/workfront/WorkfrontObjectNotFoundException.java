package com.spillman.workfront;

@SuppressWarnings("serial")
public class WorkfrontObjectNotFoundException extends WorkfrontException {

	public WorkfrontObjectNotFoundException(String message) {
		super(message);
	}

	public WorkfrontObjectNotFoundException(Throwable t) {
		super(t);
	}
}
