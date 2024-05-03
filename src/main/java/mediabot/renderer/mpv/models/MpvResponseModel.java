package mediabot.renderer.mpv.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpvResponseModel {
	private long id;	
	private String error;
	private String event;
	private Object data;
	private String name;
	
	public long getId(){
	    return id;
	}
	
	public void setId(long id){
	    this.id = id;
	}
	
	public String getError(){
	    return error;
	}
	
	public void setError(String error){
	    this.error = error;
	}
	
	public String getEvent(){
	    return event;
	}
	
	public void setEvent(String event){
	    this.event = event;
	}
	
	public Object getData(){
	    return data;
	}
	
	public void setData(Object data){
	    this.data = data;
	}
	
	public String getName(){
	    return name;
	}
	
	public void setName(String name){
	    this.name = name;
	}

	@JsonIgnore
	public boolean isSuccess(){
		return error == null || "success".equals(error);
	}
}
