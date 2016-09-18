package com.jatin.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by Jatin Jha on 2014/11/19.
 */
public class WeatherProvider extends ContentProvider {
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private com.jatin.sunshine.data.WeatherDbHelper mOpenHelper;

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + com.jatin.sunshine.data.WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME +
                        "." + com.jatin.sunshine.data.WeatherContract.LocationEntry._ID);
    }

    private static final String sLocationSettingSelection =
            com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME+
                    "." + com.jatin.sunshine.data.WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
    private static final String sLocationSettingWithStartDateSelection =
            com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME+
                    "." + com.jatin.sunshine.data.WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    com.jatin.sunshine.data.WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String sLocationSettingAndDaySelection =
            com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME +
                    "." + com.jatin.sunshine.data.WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    com.jatin.sunshine.data.WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = com.jatin.sunshine.data.WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = com.jatin.sunshine.data.WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, startDate};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = com.jatin.sunshine.data.WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = com.jatin.sunshine.data.WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, date},
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = com.jatin.sunshine.data.WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, com.jatin.sunshine.data.WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, com.jatin.sunshine.data.WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, com.jatin.sunshine.data.WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, com.jatin.sunshine.data.WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, com.jatin.sunshine.data.WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new com.jatin.sunshine.data.WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location/*"
            case LOCATION_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        com.jatin.sunshine.data.WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
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
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return com.jatin.sunshine.data.WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return com.jatin.sunshine.data.WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return com.jatin.sunshine.data.WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return com.jatin.sunshine.data.WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return com.jatin.sunshine.data.WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER: {
                long _id = db.insert(com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = com.jatin.sunshine.data.WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                long _id = db.insert(com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = com.jatin.sunshine.data.WeatherContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(
                        com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(
                        com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
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
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case WEATHER:
                rowsUpdated = db.update(com.jatin.sunshine.data.WeatherContract.WeatherEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(com.jatin.sunshine.data.WeatherContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
