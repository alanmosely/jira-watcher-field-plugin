package com.burningcode.jira.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="watcher")
@XmlAccessorType(XmlAccessType.FIELD)
public class Watcher {
	
	private String username;
	private String displayName;

	public Watcher(){
	}
	
	public Watcher(String username, String displayName){
		setUsername(username);
		setDisplayName(displayName);
	}
	
	public String getUsername(){
		return username;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public String getDisplayName(){
		return displayName;
	}
	
	public void setDisplayName(String displayName){
		this.displayName = displayName;
	}

}
