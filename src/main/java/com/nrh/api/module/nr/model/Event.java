package com.nrh.api.module.nr.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Event {
  private Date timestamp;
  private String eventType;
  private Map<String,String> sAttributes = new HashMap<String, String>();
  private Map<String,Double> dAttributes = new HashMap<String, Double>();
  private Map<String,Integer> iAttributes = new HashMap<String, Integer>();


  public Event(String eventType) {
    this.eventType = eventType;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void addStringAttribute(String name, String value) {
    if (value != null) {
      sAttributes.put(name, value);
    }
  }

  public void addIntAttribute(String name, Integer value) {
    if (value != null) {
      iAttributes.put(name, value);
    }
  }

  public void addDoubleAttribute(String name, Double value) {
    if (value != null) {
      dAttributes.put(name, value);
    }
  }

  public JSONObject toJSON() {
    JSONObject jEvent = new JSONObject();
    if (timestamp != null) { jEvent.put("timestamp", timestamp.getTime() ); }
    jEvent.put("eventType", eventType);
    
    // Add all the string attributes
    for (String name : sAttributes.keySet()) {
      jEvent.put(name, sAttributes.get(name));
    }

    // Add all the integer attributes
    for (String name : iAttributes.keySet()) {
      jEvent.put(name, iAttributes.get(name));
    }

    // Add all the double attributes
    for (String name : dAttributes.keySet()) {
      jEvent.put(name, dAttributes.get(name));
    }
    
    return jEvent;
  }
}