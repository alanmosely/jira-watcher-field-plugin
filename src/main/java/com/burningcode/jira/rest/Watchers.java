package com.burningcode.jira.rest;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.*;

import com.atlassian.jira.issue.MutableIssue;
import com.opensymphony.user.User;

@XmlRootElement(name = "issueInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class Watchers {
	@XmlAttribute
	private String issueKey;
	
	@XmlElement(name="watchers")
	private String[] watcherList;
	
	public Watchers(){
	}
	
	public Watchers(MutableIssue issue, Collection<User> watcherList){
		ArrayList<String> watchers = new ArrayList<String>();
		for(User user : watcherList){
			watchers.add(user.getName());
		}
		setWatchers(watchers.toArray(new String[watchers.size()]));
		setIssueKey(issue.getKey());
	}

	public String getIssueKey(){
		return issueKey;
	}
	
	public void setIssueKey(String issueKey){
		this.issueKey = issueKey;
	}
	
	public String[] getWatchers(){
		return watcherList;
	}
	
	public void setWatchers(String[] watchers){
		this.watcherList = watchers;
	}
}
