package com.spillman.common;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Account {
	public String accountName;
	public String accountGUID;
	
	public Account(String name, String guid) {
		this.accountName = name;
		this.accountGUID = guid;
	}
	
	public String toString() {
		return new ToStringBuilder(this)
			.append("name", accountName)
			.append("GUID", accountGUID)
			.toString();
	}
}
