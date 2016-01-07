package com.isweather;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.Messenger;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.util.Log;
import android.database.Cursor;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ISWeather extends Activity {
	private ServiceConnection sConn;
	private Messenger messenger;
	
	private TableRow rowTitle;
	private TableRow rowDayLabels;
	private TableRow rowDays;
	private TableRow rowHighs;
	private TableRow rowLows;
	private TableRow rowNights;
	private TableRow rowConditions;
	
	public static final String PREFS_NAME = "MyPrefsFile";
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.action_settings:
				Intent intent = new Intent(this, WeatherSettings.class);
				startActivity(intent);
				break;
			case R.id.action_update:
				updateCity();
				break;
		}
		return true;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String city = intent.getStringExtra(WeatherService.CITY);
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(WeatherService.CITY, city);
			editor.apply();
			updateCity();
		}
	};

	private void updateCity() {
		if(messenger==null)
			return;
		try {
			Message msg = Message.obtain(null, WeatherService.UPDATE_CITY);
			Bundle b = new Bundle();
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String city = settings.getString(WeatherService.CITY, "Sofia");
			b.putString(WeatherService.CITY, city);
			msg.setData(b);
			msg.replyTo = new Messenger(new ResponseHandler());
			messenger.send(msg);
		} catch (RemoteException e) {                   
			e.printStackTrace();
		}
	}

	class ResponseHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			int respCode = msg.what;
			switch (respCode) {
				case WeatherService.UPDATE_CITY_RESPONSE:
					String city = msg.getData().getString("city");
					int status = msg.getData().getInt("status");
					handleUpdateCity(city, status);
					Toast.makeText(ISWeather.this, city + " resp: " + status, Toast.LENGTH_SHORT).show();
					break;
			}
			
		}
		
		private void handleUpdateCity(String city, int status) {
			if(status == 0) {
				Cursor c = getContentResolver().query(WeatherProvider.CONTENT_URI, null, WeatherProvider.NAME + " = '"+city+"'", null, WeatherProvider._ID);
				int limit = rowDayLabels.getVirtualChildCount();
				int cur = 1;
				TextView title = (TextView)rowTitle.getVirtualChildAt(0);
				
				title.setText(city);
				if (c.moveToFirst()) {
					do{
						TextView dayLabel = (TextView)rowDayLabels.getVirtualChildAt(cur);
						dayLabel.setText(c.getString(c.getColumnIndex(WeatherProvider.DATE)));
						
						TextView days = (TextView)rowDays.getVirtualChildAt(cur);
						days.setText(c.getDouble(c.getColumnIndex(WeatherProvider.DAY)) + "°C");
						
						TextView dayHigh = (TextView)rowHighs.getVirtualChildAt(cur);
						dayHigh.setText(c.getDouble(c.getColumnIndex(WeatherProvider.MAX)) + "°C");
						
						TextView dayLow = (TextView)rowLows.getVirtualChildAt(cur);
						dayLow.setText(c.getDouble(c.getColumnIndex(WeatherProvider.MIN)) + "°C");
						
						TextView nights = (TextView)rowNights.getVirtualChildAt(cur);
						nights.setText(c.getDouble(c.getColumnIndex(WeatherProvider.NIGHT)) + "°C");
						
						//FIX ME
						URLConnection con = null;
						InputStream is = null;
						try {
							ImageView dayConditions = (ImageView)rowConditions.getVirtualChildAt(cur);
							URL url = new URL("http://openweathermap.org/img/w/" + c.getString(c.getColumnIndex(WeatherProvider.ICON)) + ".png");
							con = url.openConnection();
							con.setConnectTimeout(5000);
							con.setReadTimeout(5000);
							is = con.getInputStream();
							Drawable image = Drawable.createFromStream(is, "http://openweathermap.org/img/w/");
							dayConditions.setImageDrawable(image);
						} catch (Exception e) {
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (IOException e) {
								}
							}
						}
						cur++;
					} while (c.moveToNext() && cur < limit);
					c.close();
				}
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onDestroy();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                messenger = null;
            }
 
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                messenger = new Messenger(service);
				updateCity();
            }
        };
        bindService(new Intent(this, WeatherService.class), sConn, Context.BIND_AUTO_CREATE);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("city-name"));
		TableLayout table = new TableLayout(this);
		table.setStretchAllColumns(true);
		table.setShrinkAllColumns(true);
		
		rowTitle = new TableRow(this);
		rowTitle.setGravity(Gravity.CENTER_HORIZONTAL);
		
		rowDayLabels = new TableRow(this);
		rowDays = new TableRow(this);
		rowHighs = new TableRow(this);
		rowLows = new TableRow(this);
		rowNights = new TableRow(this);
		rowConditions = new TableRow(this);
		rowConditions.setGravity(Gravity.CENTER);
		
		TextView empty = new TextView(this);

		TextView title = new TextView(this);
		
		title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.SERIF, Typeface.BOLD);
		
		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.span = 6;
		
		rowTitle.addView(title, params);
		
		TextView daysLabel = new TextView(this);
		daysLabel.setText("Day");
		daysLabel.setTypeface(Typeface.DEFAULT_BOLD);
		
		TextView highsLabel = new TextView(this);
		highsLabel.setText("Day High");
		highsLabel.setTypeface(Typeface.DEFAULT_BOLD);
		
		TextView lowsLabel = new TextView(this);
		lowsLabel.setText("Day Low");
		lowsLabel.setTypeface(Typeface.DEFAULT_BOLD);

		TextView nightsLabel = new TextView(this);
		nightsLabel.setText("Night");
		nightsLabel.setTypeface(Typeface.DEFAULT_BOLD);
		
		TextView conditionsLabel = new TextView(this);
		conditionsLabel.setText("Conditions");
		conditionsLabel.setTypeface(Typeface.DEFAULT_BOLD);
		
		rowDayLabels.addView(empty);
		rowDays.addView(daysLabel);
		rowHighs.addView(highsLabel);
		rowLows.addView(lowsLabel);
		rowNights.addView(nightsLabel);
		
		rowConditions.addView(conditionsLabel);
		for(int i = 0; i < WeatherService.days_limit; ++i) {
			TextView dayLabel = new TextView(this);
			TextView days = new TextView(this);
			TextView dayHigh = new TextView(this);
			TextView dayLow = new TextView(this);
			TextView nights = new TextView(this);
			dayHigh.setGravity(Gravity.CENTER_HORIZONTAL);
			dayLow.setGravity(Gravity.CENTER_HORIZONTAL);
			
			dayLabel.setTypeface(Typeface.SERIF, Typeface.BOLD);
			rowDayLabels.addView(dayLabel);
			rowDays.addView(days);
			rowHighs.addView(dayHigh);
			rowLows.addView(dayLow);
			rowNights.addView(nights);
			
			ImageView dayConditions = new ImageView(this);
			rowConditions.addView(dayConditions);
		}

		table.addView(rowTitle);
        table.addView(rowDayLabels);
		table.addView(rowDays);
        table.addView(rowHighs);
        table.addView(rowLows);
		table.addView(rowNights);
        table.addView(rowConditions);

        setContentView(table);
    }
}
