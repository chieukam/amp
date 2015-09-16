package de.adorsys.amp.camel.gcm;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Message.Builder;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import de.adorsys.amp.camel.gcm.GCMMessage.GCMNotification;
import de.adorsys.amp.camel.gcm.GCMResults.GCMResult;

public class GCMService {
	
	private static final Logger LOG = LoggerFactory.getLogger(GCMService.class);
	
	final String GOOGLE_API_KEY = "AIzaSyAju6wApRVHMXbg7qJ1QpEAYPu3TikPook";
	
	static {
		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			
			@Override
			public String writeValue(Object value) {
				try {
					return om.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new GCMException("Connection problem sending GCM Message", e);
				}
			}
			
			@Override
			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return om.readValue(value, valueType);
				} catch (IOException e) {
					throw new GCMException("Connection problem sending GCM Message", e);
				}
			}
		});
	}
	
	public void sendNotification(Map<String, String> data, String registrationId) throws UnknownRegistrationIdException, NotRegisteredException {
		Sender sender = new Sender(GOOGLE_API_KEY);
		Builder mb = new Message.Builder()
		.collapseKey("message")
		.timeToLive(3)
		.delayWhileIdle(true);
		
		Set<Entry<String, String>> entries = data.entrySet();
		for (Entry<String, String> entry : entries) {
			mb.addData(entry.getKey(), entry.getValue());
		}
		
		Message message = mb
		.build();
		
		try {
			Result result = sender.send(message, registrationId, 3);
			if ("InvalidRegistration".equals(result.getErrorCodeName())){
				throw new UnknownRegistrationIdException(registrationId);
			} else if ("NotRegistered".equals(result.getErrorCodeName())) {
				throw new NotRegisteredException(registrationId);
			}
			LOG.debug(result.toString());
		} catch (IOException e) {
			throw new GCMException("Connection problem sending GCM Message", e);
		}
	}
	
	public void sendNotification2(Map<String, String> data, String... registrationIds) throws UnknownRegistrationIdException, NotRegisteredException {
		GCMMessage gcmMessage = new GCMMessage();
		gcmMessage.setData(data);
		gcmMessage.setRegistrationIds(registrationIds);
		GCMNotification notification = new GCMNotification();
		notification.setTitle("My Message");
		notification.setBody("This is a TAN");
		notification.setIcon("myicon");
		gcmMessage.setNotification(notification);
		try {
			HttpResponse<GCMResults> results = Unirest.post("https://gcm-http.googleapis.com/gcm/send")
					  .header("Authorization", "key=" + GOOGLE_API_KEY)
					  .header("Content-Type", "application/json")
					  .header("accept", "application/json")
					  .body(gcmMessage)
					  .asObject(GCMResults.class);
			List<GCMResult> resultList = results.getBody().getResults();
			for (GCMResult gcmResult : resultList) {
				if ("InvalidRegistration".equals(gcmResult.getError())){
					throw new UnknownRegistrationIdException(registrationIds[0]);
				} else if ("NotRegistered".equals(gcmResult.getError())) {
					throw new NotRegisteredException(registrationIds[0]);
				}
			}
		} catch (UnirestException e) {
			throw new GCMException("Connection problem sending GCM Message", e);
		}
	}

}