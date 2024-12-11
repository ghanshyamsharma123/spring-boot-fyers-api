package com.fyers.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.fyers.api.controller.CandleController;
import com.fyers.api.entity.Candle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fyers.api.exception.CommandException;

public class FyersHistoryData {

    private static final String HISTORICAL_URL = "https://api-t1.fyers.in/data/history?symbol=%s&resolution=%s&date_format=%s&range_from=%s&range_to=%s&cont_flag=%s";
    private static final Logger logger = Logger.getLogger(FyersHistoryDataAccess.class.getName());
    private static final HttpClient client = HttpClient.newHttpClient(); // Reuse HttpClient
    private String APPID;
    private String SECRET;
    private String accessToken;
    
    public FyersHistoryData(String appid, String secret) {
        this.APPID = appid;
        this.SECRET = secret;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public List<Candle> getHistoryData(String instrument, String candlePeriod, String dateFormat, String startDate, String endDate, String cont) throws CommandException {
        validateToken();
        validateInput(instrument, candlePeriod, dateFormat, startDate, endDate, cont);

        String url = String.format(HISTORICAL_URL, instrument, candlePeriod, dateFormat, startDate, endDate, cont);
        HttpResponse<String> response = getHttpResponse(url);

        if (response != null && response.statusCode() == 200) {
            return parseJsonResponse(response);
        } else {
            logger.log(Level.SEVERE, "Failed to fetch data or received non-200 response.");
        }
		return null;
    }

    private void validateToken() throws CommandException {
        if (StringUtils.isEmpty(this.accessToken)) {
            throw new CommandException("Access token is not generated.");
        }
    }

    private void validateInput(String instrument, String candlePeriod, String dateFormat, String startDate, String endDate, String cont) throws CommandException {
        if (StringUtils.isAnyEmpty(instrument, candlePeriod, dateFormat, startDate, endDate, cont)) {
            throw new CommandException("Invalid input: One or more parameters are empty.");
        }
    }

    private HttpResponse<String> getHttpResponse(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", APPID + ":" + accessToken)
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred while sending GET request: ", e);
            return null;
        }
    }

    private void parseJsonResponse(String responseBody) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candlesJsonArray = jsonObject.getAsJsonArray("candles");

            List<Candle> candles = gson.fromJson(candlesJsonArray, List.class); // Directly parse to List of Candles
            candles.forEach(System.out::println); // Print each candle (You can replace this with other logic)
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing JSON response: ", e);
        }
    }
    
    private static List<Candle> parseJsonResponse(HttpResponse<String> response) {
        try {
        	List<Candle> candles = new ArrayList<>();
        	JsonObject jsonObjectGoogle = JsonParser.parseString(response.body()).getAsJsonObject();
        	Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonPrettyPrint = gson.toJson(jsonObjectGoogle);
            System.out.println(jsonPrettyPrint);
        	Iterator<JsonElement> elementI = jsonObjectGoogle.get("candles").getAsJsonArray().iterator();
    		while(elementI.hasNext()) {
    			JsonElement next = elementI.next();
    			JsonArray d = next.getAsJsonArray();
    			candles.add(new Candle(
    					d.get(0).getAsLong(), 
    					d.get(1).getAsFloat(),
    					d.get(2).getAsFloat(),
    					d.get(3).getAsFloat(),
    					d.get(4).getAsFloat(),
    					d.get(5).getAsLong()
    				));
    		}
    		return candles;
//    		for(Candle c : candles) {
//    			System.out.println(c);
//    		}

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing JSON response: ", e);
        }
		return null;
    }
}