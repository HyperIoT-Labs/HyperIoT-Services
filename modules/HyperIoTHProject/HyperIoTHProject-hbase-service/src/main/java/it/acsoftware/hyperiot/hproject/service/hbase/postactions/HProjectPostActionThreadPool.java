/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.hproject.service.hbase.postactions;

import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component(service = HProjectPostActionThreadPool.class, immediate = true)
public class HProjectPostActionThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectPostActionThreadPool.class.getName());
    private static final String CREATING_TABLE_MESSAGE = "Creating table {}";
    private static final String TABLE_CREATION_ERROR_MESSAGE = "Error during HBase table creations";
    private static final String DISABLING_TABLE_MESSAGE = "Disabling table {}";
    private static final String DROPPING_TABLE_MESSAGE = "Dropping table {}";

    /**
     * Injecting the HBaseConnectorSystemApi
     */
    private HBaseConnectorSystemApi hBaseConnectorSystemService;

    /**
     * Remove HBase tables related to this HProject (see asynchronousPostSaveAction for knowledge about HBase tables)
     *
     * @param hProjectId HProject ID
     */
    private void asynchronousPostRemoveAction(long hProjectId) {
        disableHBaseTable(getHPacketTableName(hProjectId));
        disableHBaseTable(getTimelineTableName(hProjectId));
        disableHBaseTable(getErrorTableName(hProjectId));
        disableHBaseTable(getEventTableName(hProjectId));
        disableHBaseTable(getAlarmTableName(hProjectId));
        disableHBaseTable(getEventRuleTableName(hProjectId));
        dropHBaseTable(getHPacketTableName(hProjectId));
        dropHBaseTable(getTimelineTableName(hProjectId));
        dropHBaseTable(getErrorTableName(hProjectId));
        dropHBaseTable(getEventTableName(hProjectId));
        dropHBaseTable(getAlarmTableName(hProjectId));
        dropHBaseTable(getEventRuleTableName(hProjectId));
    }

    private void disableHBaseTable(String tableName) {
        try {
            LOGGER.debug(DISABLING_TABLE_MESSAGE, tableName);
            hBaseConnectorSystemService.disableTable(tableName);
        } catch (IOException e) {
            LOGGER.error("Error during HBase table removing", e);
        }
    }

    private void dropHBaseTable(String tableName) {
        try {
            LOGGER.debug(DROPPING_TABLE_MESSAGE, tableName);
            hBaseConnectorSystemService.dropTable(tableName);
        } catch (IOException e) {
            LOGGER.error("Error during HBase table removing", e);
        }
    }

    /**
     * For each hproject, there are six tables:
     * - <HPROJECT_TABLE_NAME_PREFIX><hprojectID> contains HPackets related to this project, in Avro format
     * - <TIMELINE_TABLE_NAME_PREFIX><hprojectID> contains HPacket counting, for timeline queries
     * - <ERROR_TABLE_NAME_PREFIX><hprojectID> contains errors (why we could not save hpacket instances)
     * - <EVENT_TABLE_NAME_PREFIX><hprojectID> contains events
     * - <ALARM_TABLE_NAME_PREFIX><hprojectID> contains alarms
     * - <EVENT_RULE_STATE_TABLE_NAME_PREFIX><hprojectID> contain information about the state of the rule related to event (generically event and alarm event).
     *
     * @param hProjectId HProject ID
     */
    private void asynchronousPostSaveAction(long hProjectId) {
        try {
            this.createHPacketTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }

        try {
            this.createTimelineTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }

        try {
            this.createErrorTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }

        try {
            this.createEventTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }

        try {
            this.createAlarmTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }

        try {
            this.createEventRuleTable(hProjectId);
        } catch (Exception e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }
    }

    private void createHBaseTable(String tableName, List<String> columnFamilies) {
        try {
            LOGGER.debug(CREATING_TABLE_MESSAGE, tableName);
            hBaseConnectorSystemService.createTable(tableName, columnFamilies);
        } catch (IOException e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }
    }

    /**
     * Create HBase tables, if they do not exist, related to this HProject (see asynchronousPostSaveAction for knowledge about HBase tables)
     *
     * @param hProjectId HProject ID
     */
    private void asynchronousPostUpdateAction(long hProjectId) {
        try {
            createTables(hProjectId);
            LOGGER.debug("Post update actions of HProject with id {} executed", hProjectId);
        } catch (IOException e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }
    }

    private String getHPacketTableName(long hProjectId) {
        return HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createHPacketTable(long hProjectId) throws IOException {
        String avroTableName = getHPacketTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(avroTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, avroTableName);
            List<String> avroColumnFamilies = new ArrayList<>();
            avroColumnFamilies.add(HProjectHBaseConstants.HPACKET_COLUMN_FAMILY);
            avroColumnFamilies.add(HProjectHBaseConstants.HPACKET_ATTACHMENTS_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(avroTableName, avroColumnFamilies);
        }
    }

    private String getTimelineTableName(long hProjectId) {
        return HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createTimelineTable(long hProjectId) throws IOException {
        String timelineTableName = getTimelineTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(timelineTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, timelineTableName);
            List<String> timelineColumnFamilies = getTimelineColumnFamilies();
            hBaseConnectorSystemService.createTable(timelineTableName, timelineColumnFamilies);
        }
    }

    private String getErrorTableName(long hProjectId) {
        return HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createErrorTable(long hProjectId) throws IOException {
        String errorTableName = getErrorTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(errorTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, errorTableName);
            List<String> tableErrorColumnFamilies = new ArrayList<>();
            tableErrorColumnFamilies.add(HProjectHBaseConstants.ERROR_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(errorTableName, tableErrorColumnFamilies);
        }
    }

    private String getEventTableName(long hProjectId) {
        return HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createEventTable(long hProjectId) throws IOException {
        String avroEventTableName = getEventTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(avroEventTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, avroEventTableName);
            List<String> eventColumnFamilies = new ArrayList<>();
            eventColumnFamilies.add(HProjectHBaseConstants.EVENT_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(avroEventTableName, eventColumnFamilies);
        }
    }

    private String getAlarmTableName(long hProjectId) {
        return HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createAlarmTable(long hProjectId) throws IOException {
        String alarmTableName = getAlarmTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(alarmTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, alarmTableName);
            List<String> alarmColumnFamilies = new ArrayList<>();
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY);
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY);
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(alarmTableName, alarmColumnFamilies);
        }
    }

    private String getEventRuleTableName(long hProjectId) {
        return HProjectHBaseConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX + hProjectId;
    }

    private void createEventRuleTable(long hProjectId) throws IOException {
        String eventRuleStateTableName = getEventRuleTableName(hProjectId);
        if (!hBaseConnectorSystemService.tableExists(eventRuleStateTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, eventRuleStateTableName);
            List<String> eventRuleStateColumnFamilies = new ArrayList<>();
            eventRuleStateColumnFamilies.add(HProjectHBaseConstants.RULE_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(eventRuleStateTableName, eventRuleStateColumnFamilies);
        }
    }

    private void createTables(long hProjectId)
            throws IOException {
        this.createHPacketTable(hProjectId);
        this.createTimelineTable(hProjectId);
        this.createErrorTable(hProjectId);
        this.createEventTable(hProjectId);
        this.createAlarmTable(hProjectId);
        this.createEventRuleTable(hProjectId);
    }

    private List<String> getTimelineColumnFamilies() {
        List<String> timelineColumnFamilies = new ArrayList<>();
        timelineColumnFamilies.add(HProjectHBaseConstants.YEAR_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.MONTH_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.DAY_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.HOUR_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.MINUTE_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.SECOND_COLUMN_FAMILY);
        timelineColumnFamilies.add(HProjectHBaseConstants.MILLISECOND_COLUMN_FAMILY);
        return timelineColumnFamilies;
    }


    /**
     * Execute HProject post remove action in asynchronous way
     *
     * @param hProjectId HProject ID
     */
    public void runPostRemoveAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostRemoveAction(hProjectId);
        hBaseConnectorSystemService.executeTask(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     * Execute HProject post save action in asynchronous way
     *
     * @param hProjectId HProject ID
     */
    public void runPostSaveAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostSaveAction(hProjectId);
        hBaseConnectorSystemService.executeTask(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     * Execute HProject post update action in asynchronous way
     *
     * @param hProjectId HProject ID
     */
    public void runPostUpdateAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostUpdateAction(hProjectId);
        hBaseConnectorSystemService.executeTask(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     * @param hBaseConnectorSystemService HBaseConnectorSystemApi service
     */
    @Reference
    public void setHBaseConnectorSystemService(HBaseConnectorSystemApi hBaseConnectorSystemService) {
        this.hBaseConnectorSystemService = hBaseConnectorSystemService;
    }


}
