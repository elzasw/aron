package cz.aron.transfagent.peva.jsoncomponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONComponent {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String type;

	private Object data;

	public JSONComponent(String type, Object data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public Object getData() {
		return data;
	}

	public String serializeToString() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
	}

}
