package id.makeithappen.walkinglog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHandler extends SQLiteOpenHelper{
	
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "walkinglog";
 
    // Contacts table name
    private static final String TABLE_HISTORY = "history";
 
    // Contacts Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_DATE = "date";
	private static final String KEY_STEP = "step";
	private static final String KEY_DURATION = "duration";
	private static final String KEY_DISTANCE = "distance";
    private static final String KEY_CALORIE = "calorie";
	private static final String KEY_FREQUENCY = "frequency";
    
	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	// Creating Table
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_HISTORY_TABLE = "CREATE TABLE "+TABLE_HISTORY+"("+KEY_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
				+KEY_DATE+" DATE,"+KEY_STEP+" INTEGER,"+KEY_DURATION+" TEXT,"+KEY_DISTANCE+" REAL,"+KEY_CALORIE+" INTEGER,"
				+KEY_FREQUENCY+" INTEGER"+")";
        db.execSQL(CREATE_HISTORY_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
 
        // Create tables again
        onCreate(db);
	}

	// add new history
	public void addHistory(History history){
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_DATE, getDateTime()); // History Date
		values.put(KEY_STEP, history.getStep()); // History Step
		values.put(KEY_DURATION, history.getDuration()); // History Duration
		values.put(KEY_DISTANCE, history.getDistance()); // History Distance
		values.put(KEY_CALORIE, history.getCalorie()); // History Calorie
		values.put(KEY_FREQUENCY, history.getFrequency()); // History Frequency

		// Inserting Row
		db.insert(TABLE_HISTORY, null, values);
		db.close(); // Closing database connection
	}

	// Get date in "dd-MM-yyyy" format
	private String getDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"dd-MM-yyyy", Locale.getDefault());
		Date date = new Date();
		return dateFormat.format(date);
	}

	// Check is Table Empty
	public boolean isTableEmpty(){
		// Count Query
		String count = "SELECT COUNT(*) FROM " + TABLE_HISTORY;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(count, null);

		if(cursor != null){
			cursor.moveToFirst();
			int record = cursor.getInt(0);
			if(record > 0){
				return true;
			}
			cursor.close();
		}
		db.close();
		return false;
	}

	// Getting All History
	public List<History> getAllHistory() {
		List<History> historyList = new ArrayList<History>();
		// Select All Query
		String selectQuery = "SELECT  * FROM "+TABLE_HISTORY+" ORDER BY "+KEY_ID+" DESC";

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				History history = new History();
				history.setID(Integer.parseInt(cursor.getString(0)));
				history.setDate(cursor.getString(1));
				history.setStep(Integer.parseInt(cursor.getString(2)));
				history.setDuration(cursor.getString(3));
				history.setDistance(Double.parseDouble(cursor.getString(4)));
				history.setCalorie(Integer.parseInt(cursor.getString(5)));
				history.setFrequency(Integer.parseInt(cursor.getString(6)));
				// Adding history to list
				historyList.add(history);
			} while (cursor.moveToNext());
			cursor.close();
		}
		db.close();
		// return history list
		return historyList;
	}

	// Deleting single history
	public void deleteHistory(int id) {
		SQLiteDatabase db = this.getWritableDatabase();
		String deleteQuery = "DELETE FROM " + TABLE_HISTORY + " where " + KEY_ID + " = " +id;
		db.execSQL(deleteQuery);
		db.close();
	}
}
