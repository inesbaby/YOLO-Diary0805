package com.example.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserSummary {
	/* 沒有使用UserProfile */

	private String username;
	private String email;
	//@JsonIgnore
	private byte[] selfieData;
	

	public UserSummary() {
	}

	public UserSummary(String username) {
		this.username = username;
	}

	public UserSummary(String username, String email) {
		this.username = username;
		this.email = email;
	}

	public UserSummary(String username, String email, byte[] selfieData) {
		this.username = username;
		this.email = email;
		this.selfieData = selfieData;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public byte[] getSelfieData() {
		return selfieData;
	}

	public void setSelfieData(byte[] selfieData) {
		this.selfieData = selfieData;
	}

	

}
