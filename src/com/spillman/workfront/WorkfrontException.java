package com.spillman.workfront;

import com.attask.api.StreamClientException;

@SuppressWarnings("serial")
public class WorkfrontException extends Exception {
	private String msgkey;
	
	public WorkfrontException(Throwable t) {
		super(t);
		
		if (t instanceof StreamClientException) {
			msgkey = ((StreamClientException)t).getMessageKey();
		}
	}
	
	public WorkfrontException(String message) {
		super(message);
	}
	
	public String getMessageKey() {
		return msgkey;
	}
}
