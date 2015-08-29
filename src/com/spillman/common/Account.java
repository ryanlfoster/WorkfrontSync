package com.spillman.common;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Account {
	public String accountName;
	public String accountGUID;
	public String agencyCode;
	
	public Account(String name, String guid, String code) {
		this.accountName = name;
		this.accountGUID = guid;
		this.agencyCode = code;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("name", accountName)
			.append("GUID", accountGUID)
			.append("code", agencyCode)
			.toString();
	}
}
