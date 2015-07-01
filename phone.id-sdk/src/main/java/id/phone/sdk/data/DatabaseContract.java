package id.phone.sdk.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import id.phone.sdk.BuildConfig;

/**
 * Created by azubchenko on 4/6/2015.
 */
public class DatabaseContract {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_COUNTRY = CountryEntry.TABLE_NAME;

    public static final class CountryEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COUNTRY).build();
        public static final String CONTENT_TYPE = CONTENT_AUTHORITY + "/" + PATH_COUNTRY;
        public static final String TABLE_NAME = "country";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ISO = "iso";

        public static Uri buildCountryUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
