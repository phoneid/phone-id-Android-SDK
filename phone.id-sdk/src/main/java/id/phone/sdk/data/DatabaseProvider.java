package id.phone.sdk.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import id.phone.sdk.data.model.Country;

import static id.phone.sdk.data.DatabaseContract.CountryEntry;

/**
 * Created by azubchenko on 4/6/2015.
 */
public class DatabaseProvider extends ContentProvider {

    private static final String TAG = DatabaseProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher URI_MATCHER = buildUriMatcher();
    private static final int COUNTRY = 100;
    private static final int COUNTRY_ID = 101;

    private DatabaseHelper mOpenHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DatabaseContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, DatabaseContract.PATH_COUNTRY, COUNTRY);
        matcher.addURI(authority, DatabaseContract.PATH_COUNTRY + "/#", COUNTRY_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = DatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (URI_MATCHER.match(uri)) {
            case COUNTRY: {
                retCursor = mOpenHelper.getReadableDatabase().query(CountryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case COUNTRY_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(CountryEntry.TABLE_NAME, projection, CountryEntry._ID + " = '" + ContentUris.parseId(uri) + "'", null, null, null, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = URI_MATCHER.match(uri);

        switch (match) {
            case COUNTRY:
            case COUNTRY_ID:
                return CountryEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        Uri returnUri;

        switch (match) {
            case COUNTRY: {
                long _id = db.insert(CountryEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = CountryEntry.buildCountryUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);

        switch (match) {
            case COUNTRY: {
                db.beginTransaction();
                //TODO: remove this line
//                db.delete(CountryEntry.TABLE_NAME, null, null);
                int count = 0;
                try {
                    if (values != null) for (ContentValues value : values) {
                        long _id = db.insert(CountryEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            count++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return count;
            }
            default:
                return super.bulkInsert(uri, values);

        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        int rowsDeleted;
        switch (match) {
            case COUNTRY:
                rowsDeleted = db.delete(CountryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        int rowsUpdated;

        switch (match) {
            case COUNTRY:
                rowsUpdated = db.update(CountryEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    /**
     * @param context   - context
     * @param countryId - id of country, that should be check
     * @return - null if country is not existing, it other case Country object
     */
    public static Country getCountry(Context context, long countryId) {
        Country result = null;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CountryEntry.CONTENT_URI, null, CountryEntry._ID + " = ?", new String[]{String.valueOf(countryId)}, null);

            if (cursor != null && !cursor.isClosed() && cursor.moveToFirst()) {
                result = Country.newInstance(DatabaseHelper.getHashtable(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    /**
     * Method to get count of the records in the specific table
     *
     * @param context
     * @param uri
     * @param where
     * @param args
     * @return
     */
    public static int checkCount(Context context, Uri uri, String where, String[] args) {
        int result = 0;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"count(*) _COUNT"}, where, args, null);

            if (cursor != null && !cursor.isClosed() && cursor.moveToFirst()) {
                result = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

}
