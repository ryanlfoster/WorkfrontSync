package com.spillman.crm;

@SuppressWarnings("serial")
public class CRMException extends Exception {
	public CRMException(String message) {
		super(message);
	}
	
	public CRMException(Throwable cause) {
		super(cause);
	}
}
