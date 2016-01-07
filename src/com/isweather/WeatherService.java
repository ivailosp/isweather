package com.isweather;

import android.app.Service;
import android.content.Intent;
import android.content.ContentValues;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Messenger;
import android.util.Log;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import java.util.Date;
import java.text.SimpleDateFormat;

public class WeatherService extends Service {
	private Messenger msg = new Messenger(new WeatherHanlder());

	static final int UPDATE_CITY = 0;
	static final int UPDATE_CITY_RESPONSE = 1;

	static final String CITY = "city";
	static final int days_limit = 5;
	
	public IBinder onBind(Intent intent) {
		return msg.getBinder();
	}

	class WeatherHanlder extends Handler {
		
		private static final String url_api = "77337022afed2791ed2e25617428d2a5";
		private static final String url_weather = "http://api.openweathermap.org/data/2.5/forecast/daily?cnt=" +
			days_limit + "&units=metric&appid=" + url_api + "&q=";
		@Override
		public void handleMessage(Message msg) {
			int msgType = msg.what;
			switch (msgType) {
				case UPDATE_CITY:
					try {
						String updateCity = msg.getData().getString(CITY);
						Message resp = Message.obtain(null, UPDATE_CITY_RESPONSE);
						Bundle bResp = new Bundle();
						bResp.putInt("status", handleCityUpdate(updateCity));
						bResp.putString(CITY, updateCity);
						resp.setData(bResp);
						msg.replyTo.send(resp);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					break;
				default:
					super.handleMessage(msg);
			}
		}
		
		private String readStream(InputStream inStream) {
			byte[] buffer = new byte[512];
			String rsp = new String();
			int ret;
			try {
				while((ret = inStream.read(buffer, 0, 512)) > 0) {
					rsp += new String(buffer, 0, ret);
				}
			} catch (IOException e) {
				e.printStackTrace();
				rsp = "";
			}
			return rsp;
		}
		
		private int handleCityUpdate(String city) {
			int iRc = -1;
			InputStream inStream = null;
			URLConnection con = null;
			try {
				URL url = new URL(url_weather + city);
				con = url.openConnection();
				con.setConnectTimeout(5000);
				con.setReadTimeout(5000);
				inStream = con.getInputStream();
				String json = readStream(inStream);
				iRc = parseData(city, json);
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
				iRc = -101;
			} catch (IOException e){
				e.printStackTrace();
				iRc = -102;
			} finally {
				try {
					if(inStream != null)
						inStream.close();
				} catch (IOException e) {
				}
			}
			return iRc;
		}
		
		private int parseData(String city, String json) {
			int iRc = -1;
			try {
				JSONObject obj = new JSONObject(json);
				if(obj.getInt("cnt") != days_limit) {
					iRc = -104;
				} else {
					JSONArray ar = obj.getJSONArray("list");
					getContentResolver().delete(WeatherProvider.CONTENT_URI, WeatherProvider.NAME + " = '"+city+"'",null);
					for(int i = 0; i < days_limit; ++i) {
						iRc = parseCityData(city, ar.getJSONObject(i));
						if(iRc != 0)
							break;
					}
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
				iRc = -103;
			}
			return iRc;
		}
		
		private int parseCityData(String city, JSONObject cityObj) {
			int iRc = -1;
			try {
				Date date = new Date(cityObj.getLong("dt") * 1000);
				double day = cityObj.getJSONObject("temp").getDouble("day");
				double min = cityObj.getJSONObject("temp").getDouble("min");
				double max = cityObj.getJSONObject("temp").getDouble("max");
				double night = cityObj.getJSONObject("temp").getDouble("night");
				String icon = cityObj.getJSONArray("weather").getJSONObject(0).getString("icon");
				ContentValues values = new ContentValues();
				values.put(WeatherProvider.NAME, city);
				SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
				values.put(WeatherProvider.DATE, sdf.format(date));
				values.put(WeatherProvider.DAY, day);
				values.put(WeatherProvider.MIN, min);
				values.put(WeatherProvider.MAX, max);
				values.put(WeatherProvider.NIGHT, night);
				values.put(WeatherProvider.ICON, icon);
				Uri uri = getContentResolver().insert(WeatherProvider.CONTENT_URI, values);
				iRc = 0;
			} catch (JSONException e) {
				e.printStackTrace();
				iRc = -105;
			}
			return iRc;
		}
	}
}