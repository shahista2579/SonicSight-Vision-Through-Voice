package org.tensorflow.lite.examples.detection;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import java.util.ArrayList;

public class DBHandler extends SQLiteOpenHelper {

    // creating a constant variables for our database.
    // below variable is for our database name.
    private static final String DB_NAME = "testdata";
    public static ArrayList<String> arrayList = new ArrayList<>();
    public static ArrayList<Double> arraysum = new ArrayList<>();
    // below int is our database version
    private static final int DB_VERSION = 1;
    static String date1;

    public static String getDate() {
        return date1;
    }

    static String error;

    public static String getError() {
        return error;
    }

    // below variable is for our table name.
    private static final String TABLE_NAME = "hello";

    // below variable is for our id column.

    // below variable is for our course name column
    private static final String NAME_COL = "name";
    private static final String NAME_NUM = "count";
    private static final String ID = "ID";


    private String[] hello;


    // creating a constructor for our database handler.
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase db) {
        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        String query2 = "create table reminder (" +
                "id integer primary key autoincrement,title text,date text,time text,finaldate default current_timestamp)";
        db.execSQL(query2);
        String query3 = "create table login (" +
                "id integer primary key autoincrement,name text,age integer,contact integer, username text,password text)";
        String query = "create table details (" + "id integer primary key autoincrement,username text,location text)";
        db.execSQL(query3);
        db.execSQL(query);
        // at last we are calling a exec sql
        // method to execute above sql query


    }

    public void DeleteDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "delete from details";
        db.execSQL(query);
    }

    // this method is use to add new course to our sqlite database.
    public void addLogindetails(String name, int age, String contact, String username, String password) {
        /** on below line we are creating a variable for
         *our sqlite database and calling writable method
         * as we are writing data in our database.
         * */

        SQLiteDatabase db = this.getWritableDatabase();

        // on below line we are creating a
        // variable for content values.
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put("name", name);

        values.put("age", age);
        values.put("contact", contact);
        values.put("username", username);

        values.put("password", password);


        // after adding all values we are passing
        // content values to our table.
        db.insertOrThrow("login", null, values);
    }

    public void addDetails(String username, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("location", location);
        db.insertOrThrow("details", null, values);
    }

    // we have created a new method for reading all the courses.

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public Boolean DeleteAllReminder() {
        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL(" DELETE FROM reminder ");
        return true;
    }

    public String addreminder(String title, String date, String time) {
        //LocalDate dateObj = LocalDate.now();
        try {
            SQLiteDatabase database = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", title);                                                          //Inserts  data into sqllite database
            contentValues.put("date", date);
            contentValues.put("time", time);
            float result = database.insert("reminder", null, contentValues);    //returns -1 if data successfully inserts into database

            if (result == -1) {
                return "Failed";
            } else {
                return "Successfully inserted";
            }

        } catch (Exception e) {
            return e.getMessage();
        }

    }

    public Cursor readallreminders() {
        SQLiteDatabase database = this.getReadableDatabase();
        String query = "select * from reminder order by id desc";
        //Sql query to  retrieve  data from the database
        return database.rawQuery(query, null);
    }

    public Boolean deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = " DELETE FROM reminder WHERE (finaldate) < DATE('now') ";
        db.execSQL(sql);
        return true;
    }

    public Boolean deleteData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(" DELETE FROM " + TABLE_NAME);
        db.close();
        return true;
    }

    // this method is use to add new course to our sqlite database.
    public void addNewCourse(String courseName, int num) {

        /** on below line we are creating a variable for
         *our sqlite database and calling writable method
         * as we are writing data in our database.*/

        SQLiteDatabase db = this.getWritableDatabase();

        // on below line we are creating a
        // variable for content values.
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(NAME_COL, courseName);

        values.put(NAME_NUM, num);

        // after adding all values we are passing
        // content values to our table.
        db.insert(TABLE_NAME, null, values);

        // at last we are closing our
        // database after adding database.


    }
}
