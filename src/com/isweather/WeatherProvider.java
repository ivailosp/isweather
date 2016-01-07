package com.isweather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import java.util.HashMap;

public class WeatherProvider extends ContentProvider {
	static final String PROVIDER_NAME = "com.isweather.WeatherProvider";
	static final String URL = "content://" + PROVIDER_NAME + "/weather";
	static final Uri CONTENT_URI = Uri.parse(URL);
	
	static final String _ID = "_id";
	static final String NAME = "name";
	static final String DATE = "date";
  	static final String DAY = "day";
	static final String MIN = "min";
	static final String MAX = "max";
	static final String NIGHT = "night";
	static final String ICON = "icon";	
   
	private static HashMap<String, String> WEATHER_PROJECTION_MAP;

	static final int WEATHER = 1;
	static final int WEATHER_ID = 2;

	static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "weather", WEATHER);
		uriMatcher.addURI(PROVIDER_NAME, "weather/#", WEATHER_ID);
	}

	private SQLiteDatabase db;
	static final String DATABASE_NAME = "City";
	static final String WEATHER_TABLE_NAME = "weather";
	static final int DATABASE_VERSION = 1;
	static final String CREATE_DB_TABLE = 
		" CREATE TABLE " + WEATHER_TABLE_NAME +
		" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
		" name TEXT NOT NULL, " +
		" date DATETIME NOT NULL, " +
		" day REAL NOT NULL, " +
		" min REAL NOT NULL, " +
		" max REAL NOT NULL, " +
		" night REAL NOT NULL, " +
		" icon TEXT NOT NULL);";

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_DB_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " +  WEATHER_TABLE_NAME);
			onCreate(db);
		}
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
		return (db == null)? false:true;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)){
		case WEATHER:
			count = db.update(WEATHER_TABLE_NAME, values, selection, selectionArgs);
			break;

		case WEATHER_ID:
			count = db.update(WEATHER_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) + 
			(!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
			break;

		default: 
			throw new IllegalArgumentException("Unknown URI " + uri );
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)){
		case WEATHER:
			count = db.delete(WEATHER_TABLE_NAME, selection, selectionArgs);
			break;

		case WEATHER_ID:
			String id = uri.getPathSegments().get(1);
			count = db.delete(WEATHER_TABLE_NAME, _ID +  " = " + id + 
			(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;

		default: 
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowID = db.insert(WEATHER_TABLE_NAME, "", values);

		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to add a record into " + uri);
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(WEATHER_TABLE_NAME);

		switch (uriMatcher.match(uri)) {
		case WEATHER:
			qb.setProjectionMap(WEATHER_PROJECTION_MAP);
			break;

		case WEATHER_ID:
			qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (sortOrder == null || sortOrder == ""){
			sortOrder = DATE;
		}
		Cursor c = qb.query(db,	projection,	selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
}