package com.smarttrack.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final Integer DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "devicesManager";
    private static final String TABLE_NAME = "devicesTable";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "ADDRESS";
    private static final String COL_3 = "NAME";
    private static final String COL_4 = "EDITNAME";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, ADDRESS VARCHAR NOT NULL UNIQUE, NAME VARCHAR, EDITNAME VARCHAR)");
        String CREATE_DEVICESTABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_2 + " VARCHAR NOT NULL UNIQUE, "
                + COL_3 + " VARCHAR, " + COL_4 + " VARCHAR" + ")";
        db.execSQL(CREATE_DEVICESTABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertDevice (String address, String name, String editName){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_2, address);
        cv.put(COL_3, name);
        cv.put(COL_4, editName);
        long result = db.insert(TABLE_NAME, null, cv);
        if(result == -1)
            return false;
        else
            return true;
    }

    public Cursor readDevice (String address){
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_4};
        String[] selectionArgs = {address};
        //Cursor cursor = db.rawQuery("SELECT EDITNAME FROM " + TABLE_NAME + " WHERE ADDRESS = " + "'" + address + "'", null);
        Cursor cursor = db.query(TABLE_NAME, columns, "ADDRESS = ?", selectionArgs, null, null, null, null);
        return cursor;
    }

    public boolean updateDevice (String address, String editname){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_4, editname);
        db.update(TABLE_NAME, cv, "ADDRESS = ?", new String[] {address});
        return true;
    }
}
