package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "yTkfPYEy3cAT60Msqmg5CGC3v897Vbpm";
	
	private static final String EMBEDDED = "_embedded";
	private static final String EVENTS = "events";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String URL_STR = "url";
	private static final String RATING = "rating";
	private static final String DISTANCE = "distance";
	private static final String VENUES = "venues";
	private static final String ADDRESS = "address";
	private static final String LINE1 = "line1";
	private static final String LINE2 = "line2";
	private static final String LINE3 = "line3";
	private static final String CITY = "city";
	private static final String IMAGES = "images";
	private static final String CLASSIFICATIONS = "classifications";
	private static final String SEGMENT = "segment";

	//help send HTTP request to TicketMaster API and get response
	public List<Item> search(double lat, double lon, String keyword) {
		//Encode keyword in URL since it may contain special char space-> 20% because http separate by space
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;					
		}
		try {
			keyword = java.net.URLEncoder.encode(keyword,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Convert lat/lon to geo Hash
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
		
		//Make URL "apikey=12345&geoPoint=abcd&keyword=music&radius=50"  %s 替换后面的
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY,geoHash,keyword,50);

		try {
			//open a HTTP connection between your Java application and TicketMaster based URL
			HttpURLConnection connection = 
					(HttpURLConnection)new URL(URL + "?" + query).openConnection();
			
			//Set request method to GET
			connection.setRequestMethod("GET");
			
			//send request to TicketMaster and get response, response code could be returned directly 
			//response body is saved in inputStream of connection   请求是同步的 发了请求立即得到
			//异步用回掉函数实现，
			int responseCode = connection.getResponseCode(); //responseCode such as 200
			//异步通过回掉实现
			System.out.println("\nSending 'GET' request to URL:" + URL + "?" + query);
			System.out.println("Response code" + responseCode);
			
			//Now read response body to get events data
			// java 对stream如何操作
			/**
			 * BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				in.close(); //直接写close， 如果reader和close直接抛出异常，reader执行不到close了 用try 
				try 

			 */
			StringBuilder response = new StringBuilder();
			try (BufferedReader in = new BufferedReader(  //bufferRead要close try 包起来自动关闭 bufferedReader 实现closable
					new InputStreamReader(connection.getInputStream()))) {
			     String inputLine;
					while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
				}
			}
				//json object can turn text(.json) to java class
				JSONObject obj = new JSONObject(response.toString());  
				if (obj.isNull("_embedded")) {
					return new ArrayList<>();
				}
				
				JSONObject embedded = obj.getJSONObject("_embedded");
				JSONArray events = embedded.getJSONArray("events");
				
				return getItemList(events);
		   } catch (Exception e) {
			e.printStackTrace();
		   }
		return new ArrayList<>();
	}
	
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull(EMBEDDED)) {
			JSONObject embedded = event.getJSONObject(EMBEDDED);
			
			if (!embedded.isNull(VENUES)) {
				JSONArray venues = embedded.getJSONArray(VENUES);
				
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					
					StringBuilder sb = new StringBuilder();
					
					if (!venue.isNull(ADDRESS)) {
						JSONObject address = venue.getJSONObject(ADDRESS);
						
						if (!address.isNull(LINE1)) {
							sb.append(address.getString(LINE1));
						}
						if (!address.isNull(LINE2)) {
							sb.append('\n');
							sb.append(address.getString(LINE2));
						}
						if (!address.isNull(LINE3)) {
							sb.append('\n');
							sb.append(address.getString(LINE3));
						}
					}
					
					if (!venue.isNull(CITY)) {
						JSONObject city = venue.getJSONObject(CITY);
						
						if (!city.isNull(NAME)) {
							sb.append('\n');
							sb.append(city.getString(NAME));
						}
					}
					
					String addr = sb.toString();
					if (!addr.equals("")) {
						return addr;
					}
				}
			}
		}
		return "";

	}
	
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull(IMAGES)) {
			JSONArray array = event.getJSONArray(IMAGES);
			for (int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull(URL_STR)) {
					return image.getString(URL_STR);
				}
			}
		}
		return "";

	}
	
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();

		if (!event.isNull(CLASSIFICATIONS)) {
			JSONArray classifications = event.getJSONArray(CLASSIFICATIONS);
			
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				
				if (!classification.isNull(SEGMENT)) {
					JSONObject segment = classification.getJSONObject(SEGMENT);
					
					if (!segment.isNull(NAME)) {
						categories.add(segment.getString(NAME));
					}
				}
			}
		}

		return categories;

	}

	//json arry --> list what we need (parse)
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			
			if (!event.isNull(NAME)) {
				builder.setName(event.getString(NAME));
			}
			if (!event.isNull(ID)) {
				builder.setItemId(event.getString(ID));
			}
			if (!event.isNull(URL_STR)) {
				builder.setUrl(event.getString(URL_STR));
			}
			
			if (!event.isNull(DISTANCE)) {
				builder.setDistance(event.getDouble(DISTANCE));
			}
			
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}
		
		return itemList;
	}
	
	// find if search return is correct for debugging
/**	private void queryAPI(double lat, double lon) {
		//JSONArray events  = search(lat, lon, null);
		List<Item> itemList = search(lat, lon, "music");
		try { // comment 了可以看到URL
//			for (int i = 0; i < events.length();i++) {
//				JSONObject event = events.getJSONObject(i);
//				System.out.println(event);
//			}
			for (Item item : itemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
**/	
	/* * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		//tmApi.queryAPI(29.682684, -95.295410);
	}

			
}
