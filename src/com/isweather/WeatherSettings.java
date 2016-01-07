package com.isweather;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

public class WeatherSettings extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		Spinner spinner = (Spinner) findViewById(R.id.settings_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.city_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		SharedPreferences settings = getSharedPreferences(ISWeather.PREFS_NAME, 0);
		String city = settings.getString(WeatherService.CITY, "Sofia");
		spinner.setSelection(adapter.getPosition(city));
		
	}
	
	public void saveButton(View view) {
		Spinner spinner = (Spinner) findViewById(R.id.settings_spinner);
		String slectedCity = spinner.getSelectedItem().toString();
		Intent intent = new Intent("city-name");

		intent.putExtra(WeatherService.CITY, slectedCity);
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		finish();
	}
}