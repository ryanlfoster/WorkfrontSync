package com.spillman.workfront;

@SuppressWarnings("serial")
public class WorkfrontException extends Exception {
	public WorkfrontException(Throwable t) {
		super(t);
	}
	
	public WorkfrontException(String message) {
		super(message);
	}
}
