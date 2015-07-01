package id.phone.sdk.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.util.Hashtable;

import static id.phone.sdk.data.DatabaseContract.CountryEntry;

/**
 * Created by azubchenko on 4/6/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //version of database, needed for possible feature upgrade
    private static final int DATABASE_VERSION = 3;

    private static final String DATABASE_DEBUG_NAME = Environment.getExternalStorageDirectory() + "/phone.id/phoneid.db";
    private static final String DATABASE_RELEASE_NAME = "phoneid.db";

    private static DatabaseHelper sInstance = null;

    private DatabaseHelper(Context context) {
        super(context, getDatabase(), null, DATABASE_VERSION);
    }

    synchronized static public DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context);
        }
        return sInstance;
    }

    private static String getDatabase() {
        return DATABASE_RELEASE_NAME;
    }

    /**
     * Help method to convert cursor to the Hashtable
     *
     * @param result
     * @return
     */
    static public Hashtable<String, String> getHashtable(Cursor result) {
        Hashtable<String, String> resultArr = new Hashtable<>();
        String[] fields = result.getColumnNames();
        for (String field : fields) {
            String value = result.getString(result.getColumnIndex(field));
            if (value != null) {
                resultArr.put(field, value);
            }
        }

        return resultArr;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CountryEntry.TABLE_NAME);

        // Create country table
        final String SQL_CREATE_COUNTRY_TABLE = "CREATE TABLE " + CountryEntry.TABLE_NAME + " (" +
                CountryEntry._ID + " INTEGER," +
                CountryEntry.COLUMN_NAME + " TEXT NOT NULL DEFAULT '', " +
                CountryEntry.COLUMN_ISO + " TEXT NOT NULL DEFAULT ''" +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_COUNTRY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        // TODO: for next release of database it will have to be changed for something better
        // For now we are just removing old database, and creating new one

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CountryEntry.TABLE_NAME);

        onCreate(sqLiteDatabase);
    }

}
