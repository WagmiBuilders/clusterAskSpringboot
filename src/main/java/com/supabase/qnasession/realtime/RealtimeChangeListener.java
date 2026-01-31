package com.supabase.qnasession.realtime;

import java.util.Map;

/**
 * Interface for handling realtime database changes from Supabase
 */
public interface RealtimeChangeListener {
    
    /**
     * Called when a new record is inserted
     * @param table The table name
     * @param record The new record data
     */
    void onInsert(String table, Map<String, Object> record);
    
    /**
     * Called when a record is updated
     * @param table The table name
     * @param newRecord The updated record data
     * @param oldRecord The previous record data
     */
    void onUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord);
    
    /**
     * Called when a record is deleted
     * @param table The table name
     * @param oldRecord The deleted record data
     */
    void onDelete(String table, Map<String, Object> oldRecord);
}
