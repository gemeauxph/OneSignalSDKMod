package top.test.onesignal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import top.oneconnectapi.onesignal.InAppMessagingHelpers;
import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.InAppMessageTable;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.NotificationTable;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestInAppMessageInternal;
import top.oneconnectapi.onesignal.OSOutcomeEvent;
import top.oneconnectapi.onesignal.ShadowOneSignalDbHelper;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.influence.domain.OSInfluenceChannel;
import top.oneconnectapi.onesignal.influence.domain.OSInfluenceType;
import top.oneconnectapi.onesignal.outcomes.data.MockOSCachedUniqueOutcomeTable;
import top.oneconnectapi.onesignal.outcomes.data.MockOSOutcomeEventsTable;
import top.oneconnectapi.onesignal.outcomes.OSOutcomeEventDB;
import top.oneconnectapi.onesignal.outcomes.data.OSOutcomeTableProvider;
import top.oneconnectapi.onesignal.outcomes.domain.OSCachedUniqueOutcomeName;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.HashMap;
import java.util.List;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger.OSTriggerKind;
import static top.test.onesignal.TestHelpers.getAllNotificationRecords;
import static top.test.onesignal.TestHelpers.getAllOutcomesRecords;
import static top.test.onesignal.TestHelpers.getAllOutcomesRecordsDBv5;
import static top.test.onesignal.TestHelpers.getAllUniqueOutcomeNotificationRecordsDB;
import static top.test.onesignal.TestHelpers.getAllUniqueOutcomeNotificationRecordsDBv5;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.OSTrigger;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowOneSignalDbHelper;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.outcomes.OSOutcomeEventDB;
import top.oneconnectapi.onesignal.outcomes.data.MockOSCachedUniqueOutcomeTable;
import top.oneconnectapi.onesignal.outcomes.data.MockOSOutcomeEventsTable;
import top.oneconnectapi.onesignal.outcomes.domain.OSCachedUniqueOutcomeName;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
                ShadowOneSignalDbHelper.class
        },
        sdk = 26
)
@RunWith(RobolectricTestRunner.class)
public class DatabaseRunner {

    private static final String INTEGER_PRIMARY_KEY_TYPE = " INTEGER PRIMARY KEY";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String FLOAT_TYPE = " FLOAT";
    private static final String TIMESTAMP_TYPE = " TIMESTAMP";
    private static final String COMMA_SEP = ",";

    private MockOneSignalDBHelper dbHelper;
    private OSOutcomeTableProvider outcomeTableProvider;

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;
        TestHelpers.beforeTestSuite();
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();

        outcomeTableProvider = new OSOutcomeTableProvider();
        dbHelper = new MockOneSignalDBHelper(ApplicationProvider.getApplicationContext());
    }

    @After
    public void afterEachTest() throws Exception {
        TestHelpers.afterTestCleanup();
    }

    @AfterClass
    public static void afterEverything() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
    }

    @Test
    public void shouldUpgradeDbFromV2ToV3() {
        // 1. Init DB as version 2 and add one notification record
        ShadowOneSignalDbHelper.DATABASE_VERSION = 2;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        writableDatabase.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(OneSignalPackagePrivateHelper.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, 1);
        writableDatabase.insertOrThrow(OneSignalPackagePrivateHelper.NotificationTable.TABLE_NAME, null, values);
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
        writableDatabase.setVersion(2);
        writableDatabase.close();

        // 2. Clear the cache of the DB so it reloads the file.
        ShadowOneSignalDbHelper.restSetStaticFields();
        ShadowOneSignalDbHelper.ignoreDuplicatedFieldsOnUpgrade = true;

        // 3. Opening the DB will auto trigger the update.
        HashMap<String, Object> notif = TestHelpers.getAllNotificationRecords(dbHelper).get(0);

        long createdTime = (Long) notif.get(OneSignalPackagePrivateHelper.NotificationTable.COLUMN_NAME_CREATED_TIME);
        long expireTime = (Long) notif.get(OneSignalPackagePrivateHelper.NotificationTable.COLUMN_NAME_EXPIRE_TIME);
        assertEquals(createdTime + (72L * (60 * 60)), expireTime);
    }

    @Test
    public void shouldUpgradeDbFromV3ToV4() throws Exception {
        // 1. Init DB as version 3
        ShadowOneSignalDbHelper.DATABASE_VERSION = 3;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSOutcomeEventsTable.TABLE_NAME + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(3);
        writableDatabase.close();

        OSOutcomeEvent event = new OSOutcomeEvent(OSInfluenceType.UNATTRIBUTED, new JSONArray().put("notificationId"), "name", 0, 0);
        ContentValues values = new ContentValues();
        values.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_INFLUENCE_TYPE, event.getSession().toString().toLowerCase());
        values.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS, event.getNotificationIds().toString());
        values.put(MockOSOutcomeEventsTable.COLUMN_NAME_NAME, event.getName());
        values.put(MockOSOutcomeEventsTable.COLUMN_NAME_TIMESTAMP, event.getTimestamp());
        values.put(MockOSOutcomeEventsTable.COLUMN_NAME_WEIGHT, event.getWeight());

        // 3. Clear the cache of the DB so it reloads the file.
        ShadowOneSignalDbHelper.restSetStaticFields();
        ShadowOneSignalDbHelper.ignoreDuplicatedFieldsOnUpgrade = true;

        // 4. Opening the DB will auto trigger the update.
        List<OSOutcomeEvent> events = TestHelpers.getAllOutcomesRecordsDBv5(dbHelper);

        assertEquals(events.size(), 0);

        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        // 5. Table now must exist
        writableDatabase.insert(MockOSOutcomeEventsTable.TABLE_NAME, null, values);
        writableDatabase.close();

        List<OSOutcomeEventDB> outcomeEvents = TestHelpers.getAllOutcomesRecords(dbHelper);

        assertEquals(outcomeEvents.size(), 1);
    }

    private static final String SQL_CREATE_OUTCOME_REVISION1_ENTRIES =
            "CREATE TABLE outcome (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "session TEXT," +
                    "notification_ids TEXT, " +
                    "name TEXT, " +
                    "timestamp TIMESTAMP, " +
                    "params TEXT " +
                    ")";

    private static final String SQL_CREATE_UNIQUE_OUTCOME_REVISION1_ENTRIES =
            "CREATE TABLE " + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V1 + " (" +
                    MockOSCachedUniqueOutcomeTable.ID + INTEGER_PRIMARY_KEY_TYPE + COMMA_SEP +
                    MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NOTIFICATION_ID + TEXT_TYPE + COMMA_SEP +
                    MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME + TEXT_TYPE +
                    ");";

    @Test
    public void shouldUpgradeDbFromV4ToV5() {
        // 1. Init DB as version 4
        ShadowOneSignalDbHelper.DATABASE_VERSION = 4;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v4
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION1_ENTRIES);

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2 + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(4);
        writableDatabase.close();

        OSCachedUniqueOutcomeName notification = new OSCachedUniqueOutcomeName("outcome", "notificationId", OSInfluenceChannel.NOTIFICATION);
        ContentValues values = new ContentValues();
        values.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_TYPE, notification.getChannel().toString());
        values.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_INFLUENCE_ID, notification.getInfluenceId());
        values.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME, notification.getName());

        // 3. Clear the cache of the DB so it reloads the file.
        ShadowOneSignalDbHelper.restSetStaticFields();
        ShadowOneSignalDbHelper.ignoreDuplicatedFieldsOnUpgrade = true;

        // 4. Opening the DB will auto trigger the update.
        List<OSCachedUniqueOutcomeName> notifications = TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper);
        assertEquals(notifications.size(), 0);

        // 5. Table now must exist
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        writableDatabase.insert(MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2, null, values);
        writableDatabase.close();

        List<OSCachedUniqueOutcomeName> uniqueOutcomeNotifications = TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper);

        assertEquals(1, uniqueOutcomeNotifications.size());
    }

    private static final String SQL_CREATE_OUTCOME_REVISION2_ENTRIES =
            "CREATE TABLE outcome (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "session TEXT," +
                    "notification_ids TEXT, " +
                    "name TEXT, " +
                    "timestamp TIMESTAMP, " +
                    "weight FLOAT " +
                    ")";


    @Test
    public void shouldUpgradeDbFromV5ToV6() {
        // 1. Init outcome table as version 5
        ShadowOneSignalDbHelper.DATABASE_VERSION = 5;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v5
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION1_ENTRIES);
        writableDatabase.execSQL(SQL_CREATE_UNIQUE_OUTCOME_REVISION1_ENTRIES);

        // Insert one outcome record so we can test migration keeps it later on
        ContentValues values = new ContentValues();
        values.put("name", "a");
        writableDatabase.insertOrThrow("outcome", null, values);
        writableDatabase.setVersion(5);
        writableDatabase.close();

        // 2. restSetStaticFields so the db reloads and upgrade is done to version 6
        ShadowOneSignalDbHelper.restSetStaticFields();

        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // 3. Ensure the upgrade kept our existing record
        Cursor cursor = writableDatabase.query(
                "outcome",
                null,
                null,
                null,
                null,
                null,
                null
        );
        assertTrue(cursor.moveToFirst());
        assertEquals("a", cursor.getString(cursor.getColumnIndex("name")));

        // 4. Ensure new weight column exists
        values = new ContentValues();
        values.put("weight", 1);
        long successful = writableDatabase.insert("outcome", null, values);
        assertFalse(successful == -1);

        // 5. Ensure params column does NOT exists
        values = new ContentValues();
        values.put("params", 1);
        successful = writableDatabase.insert("outcome", null, values);
        writableDatabase.close();
        assertEquals(-1, successful);
    }

    @Test
    public void shouldUpgradeDbFromV6ToV7() throws JSONException {
        // 1. Init DB as version 6
        ShadowOneSignalDbHelper.DATABASE_VERSION = 6;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v6
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION2_ENTRIES);
        writableDatabase.execSQL(SQL_CREATE_UNIQUE_OUTCOME_REVISION1_ENTRIES);

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + OneSignalPackagePrivateHelper.InAppMessageTable.TABLE_NAME + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(6);
        writableDatabase.close();

        // Create an IAM
        final OneSignalPackagePrivateHelper.OSTestInAppMessageInternal inAppMessage = InAppMessagingHelpers.buildTestMessageWithSingleTrigger(
                OSTrigger.OSTriggerKind.SESSION_TIME,
                null,
                OneSignalPackagePrivateHelper.OSTestTrigger.OSTriggerOperator.NOT_EXISTS.toString(),
                null);

        inAppMessage.setDisplayedInSession(true);

        ContentValues values = new ContentValues();
        values.put(OneSignalPackagePrivateHelper.InAppMessageTable.COLUMN_NAME_MESSAGE_ID, inAppMessage.getMessageId());
        values.put(OneSignalPackagePrivateHelper.InAppMessageTable.COLUMN_NAME_DISPLAY_QUANTITY, inAppMessage.getRedisplayStats().getDisplayQuantity());
        values.put(OneSignalPackagePrivateHelper.InAppMessageTable.COLUMN_NAME_LAST_DISPLAY, inAppMessage.getRedisplayStats().getLastDisplayTime());
        values.put(OneSignalPackagePrivateHelper.InAppMessageTable.COLUMN_CLICK_IDS, inAppMessage.getClickedClickIds().toString());
        values.put(OneSignalPackagePrivateHelper.InAppMessageTable.COLUMN_DISPLAYED_IN_SESSION, inAppMessage.isDisplayedInSession());

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();

        // 4. Opening the DB will auto trigger the update.
        List<OneSignalPackagePrivateHelper.OSTestInAppMessageInternal> savedInAppMessages = TestHelpers.getAllInAppMessages(dbHelper);
        assertEquals(savedInAppMessages.size(), 0);

        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        // 5. Table now must exist
        writableDatabase.insert(OneSignalPackagePrivateHelper.InAppMessageTable.TABLE_NAME, null, values);
        writableDatabase.close();

        List<OneSignalPackagePrivateHelper.OSTestInAppMessageInternal> savedInAppMessagesAfterCreation = TestHelpers.getAllInAppMessages(dbHelper);

        assertEquals(savedInAppMessagesAfterCreation.size(), 1);
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal savedInAppMessage = savedInAppMessagesAfterCreation.get(0);
        assertEquals(savedInAppMessage.getRedisplayStats().getDisplayQuantity(), inAppMessage.getRedisplayStats().getDisplayQuantity());
        assertEquals(savedInAppMessage.getRedisplayStats().getLastDisplayTime(), inAppMessage.getRedisplayStats().getLastDisplayTime());
        assertEquals(savedInAppMessage.getClickedClickIds().toString(), inAppMessage.getClickedClickIds().toString());
        assertEquals(savedInAppMessage.isDisplayedInSession(), inAppMessage.isDisplayedInSession());
    }

    @Test
    public void shouldUpgradeDbFromV7ToV8CacheUniqueOutcomeTable() throws JSONException {
        // 1. Init DB as version 7
        ShadowOneSignalDbHelper.DATABASE_VERSION = 7;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v7
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION2_ENTRIES);
        writableDatabase.execSQL(SQL_CREATE_UNIQUE_OUTCOME_REVISION1_ENTRIES);

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2 + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);

        // Set data to check that modification on table keep data
        OSCachedUniqueOutcomeName cachedOutcomeBeforeUpdate = new OSCachedUniqueOutcomeName("outcome", "notificationId", OSInfluenceChannel.NOTIFICATION);
        ContentValues uniqueOutcomeValuesBeforeUpdate = new ContentValues();
        uniqueOutcomeValuesBeforeUpdate.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NOTIFICATION_ID, cachedOutcomeBeforeUpdate.getInfluenceId());
        uniqueOutcomeValuesBeforeUpdate.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME, cachedOutcomeBeforeUpdate.getName());

        writableDatabase.insert(MockOSCachedUniqueOutcomeTable.TABLE_NAME_V1, null, uniqueOutcomeValuesBeforeUpdate);

        List<OSCachedUniqueOutcomeName> cachedOutcomesBeforeUpdate = TestHelpers.getAllUniqueOutcomeNotificationRecordsDBv5(dbHelper);
        assertEquals(1, cachedOutcomesBeforeUpdate.size());

        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        writableDatabase.setVersion(7);
        writableDatabase.close();

        ContentValues uniqueOutcomeValues = new ContentValues();
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_INFLUENCE_ID, cachedOutcomeBeforeUpdate.getInfluenceId());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_TYPE, cachedOutcomeBeforeUpdate.getChannel().toString());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME, cachedOutcomeBeforeUpdate.getName());

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();
        ShadowOneSignalDbHelper.ignoreDuplicatedFieldsOnUpgrade = true;

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        List<OSCachedUniqueOutcomeName> uniqueOutcomeNotifications = TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper);
        assertEquals(1, uniqueOutcomeNotifications.size());
        assertEquals(cachedOutcomeBeforeUpdate.getInfluenceId(), uniqueOutcomeNotifications.get(0).getInfluenceId());
        assertEquals(cachedOutcomeBeforeUpdate.getChannel(), uniqueOutcomeNotifications.get(0).getChannel());
        assertEquals(cachedOutcomeBeforeUpdate.getName(), uniqueOutcomeNotifications.get(0).getName());
    }

    @Test
    public void shouldUpgradeDbFromV7ToV8OutcomesTable() {
        // 1. Init DB as version 7
        ShadowOneSignalDbHelper.DATABASE_VERSION = 7;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v7
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION2_ENTRIES);
        writableDatabase.execSQL(SQL_CREATE_UNIQUE_OUTCOME_REVISION1_ENTRIES);

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2 + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);

        // Set data to check that modification on table keep data
        OSOutcomeEventDB outcomeEventDB = new OSOutcomeEventDB(OSInfluenceType.DIRECT, OSInfluenceType.INDIRECT,
                "iam_id", "notificationId", "outcome_outcome", 1234, (float) 1234);

        ContentValues outcomeValues = new ContentValues();
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS, outcomeEventDB.getNotificationIds().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_SESSION, outcomeEventDB.getNotificationInfluenceType().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NAME, outcomeEventDB.getName());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_WEIGHT, outcomeEventDB.getWeight());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_TIMESTAMP, outcomeEventDB.getTimestamp());

        writableDatabase.insert(MockOSOutcomeEventsTable.TABLE_NAME, null, outcomeValues);

        List<OSOutcomeEvent> outcomesSavedBeforeUpdate = TestHelpers.getAllOutcomesRecordsDBv5(dbHelper);
        assertEquals(1, outcomesSavedBeforeUpdate.size());

        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();
        writableDatabase.setVersion(7);
        writableDatabase.close();

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();
        ShadowOneSignalDbHelper.ignoreDuplicatedFieldsOnUpgrade = true;

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        List<OSOutcomeEventDB> outcomesSaved = TestHelpers.getAllOutcomesRecords(dbHelper);
        assertEquals(1, outcomesSaved.size());
        OSOutcomeEventDB outcomeSaved = outcomesSaved.get(0);

        assertEquals(outcomeEventDB.getName(), outcomeSaved.getName());
        assertEquals(outcomeEventDB.getWeight(), outcomeSaved.getWeight());
        assertEquals(outcomeEventDB.getTimestamp(), outcomeSaved.getTimestamp());
        assertEquals(outcomeEventDB.getNotificationIds(), outcomeSaved.getNotificationIds());
        assertEquals(outcomeEventDB.getNotificationInfluenceType(), outcomeSaved.getNotificationInfluenceType());
        assertEquals(new JSONArray(), outcomeSaved.getIamIds());
        assertEquals(OSInfluenceType.UNATTRIBUTED, outcomeSaved.getIamInfluenceType());
    }

    @Test
    public void shouldUpgradeDbFromV3ToV8UniqueOutcomeTable() {
        // 1. Init DB as version 3
        ShadowOneSignalDbHelper.DATABASE_VERSION = 3;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2 + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(3);
        writableDatabase.close();

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        OSCachedUniqueOutcomeName uniqueOutcomeName = new OSCachedUniqueOutcomeName("outcome", "notificationId", OSInfluenceChannel.NOTIFICATION);
        ContentValues uniqueOutcomeValues = new ContentValues();
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_INFLUENCE_ID, uniqueOutcomeName.getInfluenceId());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_TYPE, uniqueOutcomeName.getChannel().toString());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME, uniqueOutcomeName.getName());

        writableDatabase.insert(MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2, null, uniqueOutcomeValues);

        List<OSCachedUniqueOutcomeName> uniqueOutcomeNotifications = TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper);
        assertEquals(1, uniqueOutcomeNotifications.size());
        assertEquals(uniqueOutcomeName.getInfluenceId(), uniqueOutcomeNotifications.get(0).getInfluenceId());
        assertEquals(uniqueOutcomeName.getChannel(), uniqueOutcomeNotifications.get(0).getChannel());
        assertEquals(uniqueOutcomeName.getName(), uniqueOutcomeNotifications.get(0).getName());
    }

    @Test
    public void shouldUpgradeDbFromV3ToV8OutcomeTable() {
        // 1. Init DB as version 3
        ShadowOneSignalDbHelper.DATABASE_VERSION = 3;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSOutcomeEventsTable.TABLE_NAME + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(3);
        writableDatabase.close();

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        OSOutcomeEventDB outcomeEventDB = new OSOutcomeEventDB(OSInfluenceType.DIRECT, OSInfluenceType.INDIRECT,
                "iam_id", "notificationId", "outcome_outcome", 1234, (float) 1234);

        ContentValues outcomeValues = new ContentValues();
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS, outcomeEventDB.getNotificationIds().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_IAM_IDS, outcomeEventDB.getIamIds().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_INFLUENCE_TYPE, outcomeEventDB.getNotificationInfluenceType().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_IAM_INFLUENCE_TYPE, outcomeEventDB.getIamInfluenceType().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NAME, outcomeEventDB.getName());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_WEIGHT, outcomeEventDB.getWeight());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_TIMESTAMP, outcomeEventDB.getTimestamp());

        writableDatabase.insert(MockOSOutcomeEventsTable.TABLE_NAME, null, outcomeValues);

        List<OSOutcomeEventDB> outcomesSaved = TestHelpers.getAllOutcomesRecords(dbHelper);
        assertEquals(1, outcomesSaved.size());
        OSOutcomeEventDB outcomeSaved = outcomesSaved.get(0);

        assertEquals(outcomeEventDB.getName(), outcomeSaved.getName());
        assertEquals(outcomeEventDB.getWeight(), outcomeSaved.getWeight());
        assertEquals(outcomeEventDB.getTimestamp(), outcomeSaved.getTimestamp());
        assertEquals(outcomeEventDB.getNotificationIds(), outcomeSaved.getNotificationIds());
        assertEquals(outcomeEventDB.getNotificationInfluenceType(), outcomeSaved.getNotificationInfluenceType());
        assertEquals(outcomeEventDB.getIamIds(), outcomeSaved.getIamIds());
        assertEquals(outcomeEventDB.getIamInfluenceType(), outcomeSaved.getIamInfluenceType());
    }

    @Test
    public void shouldUpgradeDbFromV4ToV8UniqueOutcomeTable() {
        // 1. Init DB as version 4
        ShadowOneSignalDbHelper.DATABASE_VERSION = 4;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v4
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION2_ENTRIES);

        Cursor cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name='" + MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2 + "'", null);

        boolean exist = false;
        if (cursor != null) {
            exist = cursor.getCount() > 0;
            cursor.close();
        }
        // 2. Table must not exist
        assertFalse(exist);
        writableDatabase.setVersion(4);
        writableDatabase.close();

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        OSCachedUniqueOutcomeName uniqueOutcomeName = new OSCachedUniqueOutcomeName("outcome", "notificationId", OSInfluenceChannel.NOTIFICATION);
        ContentValues uniqueOutcomeValues = new ContentValues();
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_INFLUENCE_ID, uniqueOutcomeName.getInfluenceId());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_CHANNEL_TYPE, uniqueOutcomeName.getChannel().toString());
        uniqueOutcomeValues.put(MockOSCachedUniqueOutcomeTable.COLUMN_NAME_NAME, uniqueOutcomeName.getName());

        writableDatabase.insert(MockOSCachedUniqueOutcomeTable.TABLE_NAME_V2, null, uniqueOutcomeValues);

        List<OSCachedUniqueOutcomeName> uniqueOutcomeNotifications = TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper);
        assertEquals(1, uniqueOutcomeNotifications.size());
        assertEquals(uniqueOutcomeName.getInfluenceId(), uniqueOutcomeNotifications.get(0).getInfluenceId());
        assertEquals(uniqueOutcomeName.getChannel(), uniqueOutcomeNotifications.get(0).getChannel());
        assertEquals(uniqueOutcomeName.getName(), uniqueOutcomeNotifications.get(0).getName());
    }

    @Test
    public void shouldUpgradeDbFromV4ToV8OutcomeTable() {
        // 1. Init DB as version 4
        ShadowOneSignalDbHelper.DATABASE_VERSION = 4;
        SQLiteDatabase writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        // Create table with the schema we had in DB v4
        writableDatabase.execSQL(SQL_CREATE_OUTCOME_REVISION1_ENTRIES);
        Cursor cursor = writableDatabase.query(MockOSOutcomeEventsTable.TABLE_NAME, null, null, null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            String[] columns = cursor.getColumnNames();
            exist = columns.length == 6;
            cursor.close();
        }
        // 2. Table must not exist
        assertTrue(exist);
        writableDatabase.setVersion(4);
        writableDatabase.close();

        // 3. Clear the cache of the DB so it reloads the file and next getSQLiteDatabaseWithRetries will auto trigger the update
        ShadowOneSignalDbHelper.restSetStaticFields();

        // 4. Opening the DB will auto trigger the update to DB version 8.
        writableDatabase = dbHelper.getSQLiteDatabaseWithRetries();

        OSOutcomeEventDB outcomeEventDB = new OSOutcomeEventDB(OSInfluenceType.DIRECT, OSInfluenceType.INDIRECT,
                "iam_id", "notificationId", "outcome_outcome", 1234, (float) 1234);

        ContentValues outcomeValues = new ContentValues();
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS, outcomeEventDB.getNotificationIds().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_IAM_IDS, outcomeEventDB.getIamIds().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NOTIFICATION_INFLUENCE_TYPE, outcomeEventDB.getNotificationInfluenceType().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_IAM_INFLUENCE_TYPE, outcomeEventDB.getIamInfluenceType().toString());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_NAME, outcomeEventDB.getName());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_WEIGHT, outcomeEventDB.getWeight());
        outcomeValues.put(MockOSOutcomeEventsTable.COLUMN_NAME_TIMESTAMP, outcomeEventDB.getTimestamp());

        dbHelper.insert(MockOSOutcomeEventsTable.TABLE_NAME, null, outcomeValues);

        List<OSOutcomeEventDB> outcomesSaved = TestHelpers.getAllOutcomesRecords(dbHelper);
        assertEquals(1, outcomesSaved.size());
        OSOutcomeEventDB outcomeSaved = outcomesSaved.get(0);

        assertEquals(outcomeEventDB.getName(), outcomeSaved.getName());
        assertEquals(outcomeEventDB.getWeight(), outcomeSaved.getWeight());
        assertEquals(outcomeEventDB.getTimestamp(), outcomeSaved.getTimestamp());
        assertEquals(outcomeEventDB.getNotificationIds(), outcomeSaved.getNotificationIds());
        assertEquals(outcomeEventDB.getNotificationInfluenceType(), outcomeSaved.getNotificationInfluenceType());
        assertEquals(outcomeEventDB.getIamIds(), outcomeSaved.getIamIds());
        assertEquals(outcomeEventDB.getIamInfluenceType(), outcomeSaved.getIamInfluenceType());
    }

}
