package com.kenny.openimgur.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.UploadedPhoto;
import com.kenny.openimgur.util.DBContracts.ProfileContract;
import com.kenny.openimgur.util.DBContracts.TopicsContract;
import com.kenny.openimgur.util.DBContracts.UploadContract;
import com.kenny.openimgur.util.DBContracts.UserContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/25/14.
 */
public class SqlHelper extends SQLiteOpenHelper {
    private static final String TAG = "SqlHelper";

    private static final int DB_VERSION = 3;

    private static final String DB_NAME = "open_imgur.db";

    private static SQLiteDatabase mReadableDatabase;

    private static SQLiteDatabase mWritableDatabase;

    public SqlHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(UserContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(ProfileContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(UploadContract.CREATE_TABLE_SQL);
        sqLiteDatabase.execSQL(TopicsContract.CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // V2 Added uploads Table
        // V3 Added topics Table
        onCreate(db);
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if (mReadableDatabase == null || !mReadableDatabase.isOpen()) {
            mReadableDatabase = super.getReadableDatabase();
        }

        return mReadableDatabase;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if (mWritableDatabase == null || !mWritableDatabase.isOpen()) {
            mWritableDatabase = super.getWritableDatabase();
        }

        return mReadableDatabase;
    }

    /**
     * Inserts the user to the database
     *
     * @param user
     */
    public void insertUser(@NonNull ImgurUser user) {
        LogUtil.v(TAG, "Inserting user " + user.toString());
        // Wipe any users before we add the new one in
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DBContracts.UserContract.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();
        values.put(UserContract._ID, user.getId());
        values.put(UserContract.COLUMN_NAME, user.getUsername());
        values.put(UserContract.COLUMN_ACCESS_TOKEN, user.getAccessToken());
        values.put(UserContract.COLUMN_REFRESH_TOKEN, user.getRefreshToken());
        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, user.getAccessTokenExpiration());
        values.put(UserContract.COLUMN_CREATED, user.getCreated());
        values.put(UserContract.COLUMN_PRO_EXPIRATION, user.getProExpiration());
        values.put(UserContract.COLUMN_REPUTATION, user.getReputation());
        db.insert(UserContract.TABLE_NAME, null, values);
    }

    /**
     * Updates the user's information
     *
     * @param user
     */
    public void updateUserInfo(@NonNull ImgurUser user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserContract._ID, user.getId());
        values.put(UserContract.COLUMN_NAME, user.getUsername());
        values.put(UserContract.COLUMN_ACCESS_TOKEN, user.getAccessToken());
        values.put(UserContract.COLUMN_REFRESH_TOKEN, user.getRefreshToken());
        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, user.getAccessTokenExpiration());
        values.put(UserContract.COLUMN_CREATED, user.getCreated());
        values.put(UserContract.COLUMN_PRO_EXPIRATION, user.getProExpiration());
        values.put(UserContract.COLUMN_REPUTATION, user.getReputation());
        db.update(UserContract.TABLE_NAME, values, null, null);
    }

    /**
     * Returns the currently logged in User
     *
     * @return User, or null if no one is logged in
     */
    @Nullable
    public ImgurUser getUser() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(UserContract.TABLE_NAME, null, null, null, null, null, null);
        ImgurUser user = null;

        if (cursor.moveToFirst()) {
            LogUtil.v(TAG, "User present");
            user = new ImgurUser(cursor, true);
        } else {
            LogUtil.v(TAG, "No user present");
        }

        cursor.close();
        return user;

    }

    /**
     * Updates the users tokens
     *
     * @param accessToken
     * @param refreshToken
     * @param expiration
     */
    public void updateUserTokens(String accessToken, String refreshToken, long expiration) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(3);

        values.put(UserContract.COLUMN_ACCESS_TOKEN_EXPIRATION, expiration);
        values.put(UserContract.COLUMN_ACCESS_TOKEN, accessToken);
        values.put(UserContract.COLUMN_REFRESH_TOKEN, refreshToken);

        db.update(UserContract.TABLE_NAME, values, null, null);
    }

    /**
     * Clears the user from the database
     */
    public void onUserLogout() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(UserContract.TABLE_NAME, null, null);
    }

    /**
     * Returns a user based on the username
     *
     * @param username
     * @return Profile of user, or null if none exists
     */
    @Nullable
    public ImgurUser getUser(String username) {
        ImgurUser user = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(String.format(ProfileContract.SEARCH_USER_SQL, username), null);

        if (cursor.moveToFirst()) {
            user = new ImgurUser(cursor, false);
        }

        cursor.close();
        return user;
    }

    /**
     * Inserts a new profile into the database for caching purposes
     *
     * @param profile
     */
    public void insertProfile(@NonNull ImgurUser profile) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(5);
        values.put(ProfileContract._ID, profile.getId());
        values.put(ProfileContract.COLUMN_USERNAME, profile.getUsername());
        values.put(ProfileContract.COLUMN_BIO, profile.getBio());
        values.put(ProfileContract.COLUMN_REP, profile.getReputation());
        values.put(ProfileContract.COLUMN_LAST_SEEN, profile.getLastSeen());
        values.put(ProfileContract.COLUMN_CREATED, profile.getCreated());
        db.insertWithOnConflict(ProfileContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Inserts an uploaded photo into the database
     *
     * @param photo
     */
    public void insertUploadedPhoto(ImgurPhoto photo) {
        if (photo == null || TextUtils.isEmpty(photo.getLink())) {
            LogUtil.w(TAG, "Null photo can not be inserted");
            return;
        }

        LogUtil.v(TAG, "Inserting Uploaded photo: " + photo.getLink());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(3);
        values.put(UploadContract.COLUMN_URL, photo.getLink());
        values.put(UploadContract.COLUMN_DELETE_HASH, photo.getDeleteHash());
        values.put(UploadContract.COLUMN_DATE, System.currentTimeMillis());
        db.insert(UploadContract.TABLE_NAME, null, values);
    }

    /**
     * Returns all upload photos from device
     *
     * @param newestFirst
     * @return
     */
    public List<UploadedPhoto> getUploadedPhotos(boolean newestFirst) {
        List<UploadedPhoto> photos = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(String.format(UploadContract.GET_UPLOADS_SQL, newestFirst ? "DESC" : "ASC"), null);

        while (cursor.moveToNext()) {
            photos.add(new UploadedPhoto(cursor));
        }

        cursor.close();
        return photos;
    }

    /**
     * Deletes the given photo from the Uploaded Photos table
     *
     * @param photo
     */
    public void deleteUploadedPhoto(UploadedPhoto photo) {
        if (photo == null) return;

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(String.format(UploadContract.DELETE_PHOTO_SQL, photo.getId()));
    }

    /**
     * Adds a list of topics into the database
     *
     * @param topics
     */
    public void addTopics(List<ImgurTopic> topics) {
        if (topics == null || topics.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(3);

        for (ImgurTopic topic : topics) {
            values.clear();
            values.put(TopicsContract.COLUMN_TOPIC_ID, topic.getId());
            values.put(TopicsContract.COLUMN_NAME, topic.getName());
            values.put(TopicsContract.COLUMN_DESC, topic.getDescription());
            db.insertWithOnConflict(TopicsContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Returns a list of all the cached topics
     *
     * @return
     */
    public List<ImgurTopic> getTopics() {
        List<ImgurTopic> topics = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(TopicsContract.GET_TOPICS_SQL, null);

        while (cursor.moveToNext()) {
            topics.add(new ImgurTopic(cursor));
        }

        cursor.close();
        return topics;
    }

    /**
     * Returns a single topic given its id
     *
     * @param id
     * @return
     */
    public ImgurTopic getTopic(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(String.format(TopicsContract.GET_TOPIC_SQL, id), null);
        ImgurTopic topic = null;

        if (cursor.moveToFirst()) {
            topic = new ImgurTopic(cursor);
        }

        cursor.close();
        return topic;
    }

    @Override
    public synchronized void close() {
        if (mReadableDatabase != null) {
            mReadableDatabase.close();
            mReadableDatabase = null;
        }

        if (mWritableDatabase != null) {
            mWritableDatabase.close();
            mWritableDatabase = null;
        }

        super.close();
    }
}
