package rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

public class RpcHelper {
	// Writes a JSONArray to http response.
	public static void writeJsonArray(HttpServletResponse response, JSONArray array) throws IOException{
		try {
		PrintWriter out = response.getWriter();	
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		out.print(array);
		out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    // Writes a JSONObject to http response.
	public static void writeJsonObject(HttpServletResponse response, JSONObject obj) throws IOException {
		try {
		PrintWriter out = response.getWriter();	
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		out.print(obj);
		out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// Parses a JSONObject from http request.
	/**
	 * 		{
		    user_id = “1111”,
		    favorite = [
		        “abcd”,
		        “efgh”,
		    ]
			}
	 * 发送http post 请求
	 * 
	 */
	public static JSONObject readJSONObject(HttpServletRequest request) {
	   	   StringBuilder sBuilder = new StringBuilder();
	   	   try (BufferedReader reader = request.getReader()) {
	   		 String line = null;
	   		 while((line = reader.readLine()) != null) {
	   			 sBuilder.append(line);
	   		 }
	   		 return new JSONObject(sBuilder.toString());
	   		 
	   	   } catch (Exception e) {
	   		 e.printStackTrace();
	   	   }
	   	 
	   	  return new JSONObject();
     }

}
