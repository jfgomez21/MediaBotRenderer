package mediabot.renderer.mpv.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MpvRequestModel {
	private List<Object> command;

	@JsonProperty("request_id")
	private Long requestId;

	public Long getRequestId(){
	    return requestId;
	}
	
	public void setRequestId(Long requestId){
	    this.requestId = requestId;
	}

	public void setCommand(List<Object> command){
		this.command = command;
	}

	public List<Object> getCommand(){
		if(command == null){
			command = new ArrayList<>();
		}

		return command;
	}

	public void addCommands(Object ... values){
		for(Object value : values){
			getCommand().add(value);
		}
	}
}
