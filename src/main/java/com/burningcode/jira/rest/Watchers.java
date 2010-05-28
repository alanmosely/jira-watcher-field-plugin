package com.burningcode.jira.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="watcherInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class Watchers {
	@XmlAttribute
	private Long issueId;
	
	@XmlElements({
		@XmlElement(name="fieldIds", type=String.class)
	})
	private List<String> fieldIds;

	@XmlElement(name="watchers", type=Watcher.class)
	private List<Watcher> watchers;
	
	public Watchers(){
	}
	
	public Watchers(Long issueId, List<Watcher> watchers, List<String> fieldIds){		
		setWatchers(watchers);
		setIssueId(issueId);
		setWatcherFieldIds(fieldIds);
	}

	public Long getIssueId(){
		return issueId;
	}
	
	public void setIssueId(Long issueId){
		this.issueId = issueId;
	}
	
	public List<Watcher> getWatchers(){
		return watchers;
	}
	
	public void setWatchers(List<Watcher> watchers){
		this.watchers = watchers;
	}
	
	public List<String> getWatcherfieldIds(){
		return fieldIds;
	}
	
	public void setWatcherFieldIds(List<String> fieldIds){
		this.fieldIds = fieldIds;
	}
}
