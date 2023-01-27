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
        String avroTableName = HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + hProjectId;
        String timelineTableName = HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + hProjectId;
        String errorTableName = HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + hProjectId;
        String avroEventTableName = HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + hProjectId;
        String alarmTableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
        String eventRuleStateTableName = HProjectHBaseConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX + hProjectId;
        disableHBaseTable(avroTableName);
        disableHBaseTable(timelineTableName);
        disableHBaseTable(errorTableName);
        disableHBaseTable(avroEventTableName);
        disableHBaseTable(alarmTableName);
        disableHBaseTable(eventRuleStateTableName);
        dropHBaseTable(avroTableName);
        dropHBaseTable(timelineTableName);
        dropHBaseTable(errorTableName);
        dropHBaseTable(avroEventTableName);
        dropHBaseTable(alarmTableName);
        dropHBaseTable(eventRuleStateTableName);
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
        String avroTableName = HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + hProjectId;
        String timelineTableName = HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + hProjectId;
        String errorTableName = HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + hProjectId;
        String avroEventTableName = HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + hProjectId;
        String alarmTableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
        String eventRuleStateTableName = HProjectHBaseConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX + hProjectId;
        List<String> avroColumnFamilies = new ArrayList<>();
        avroColumnFamilies.add(HProjectHBaseConstants.HPACKET_COLUMN_FAMILY);
        List<String> eventColumnFamilies = new ArrayList<>();
        eventColumnFamilies.add(HProjectHBaseConstants.EVENT_COLUMN_FAMILY);
        List<String> timelineColumnFamilies = getTimelineColumnFamilies();
        List<String> tableErrorColumnFamilies = new ArrayList<>();
        tableErrorColumnFamilies.add(HProjectHBaseConstants.ERROR_COLUMN_FAMILY);
        List<String> alarmColumnFamilies = new ArrayList<>();
        alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY);
        alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY);
        alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY);
        List<String> eventRuleStateFamilies = new ArrayList<>();
        eventRuleStateFamilies.add(HProjectHBaseConstants.RULE_COLUMN_FAMILY);
        createHBaseTable(avroTableName, avroColumnFamilies);
        createHBaseTable(timelineTableName, timelineColumnFamilies);
        createHBaseTable(errorTableName, tableErrorColumnFamilies);
        createHBaseTable(avroEventTableName, eventColumnFamilies);
        createHBaseTable(alarmTableName, alarmColumnFamilies);
        createHBaseTable(eventRuleStateTableName, eventRuleStateFamilies);
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
        String avroTableName = HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + hProjectId;
        String timelineTableName = HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + hProjectId;
        String errorTableName = HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + hProjectId;
        String avroEventTableName = HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + hProjectId;
        String alarmTableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
        String eventRuleStateTableName = HProjectHBaseConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX + hProjectId;
        try {
            createTables(avroTableName, timelineTableName, errorTableName, avroEventTableName,
                    alarmTableName, eventRuleStateTableName);
            LOGGER.debug("Post update actions of HProject with id {} executed", hProjectId);
        } catch (IOException e) {
            LOGGER.error(TABLE_CREATION_ERROR_MESSAGE, e);
        }
    }

    private void createTables(String avroTableName, String timelineTableName, String errorTableName,
                              String avroEventTableName, String alarmTableName, String eventRuleStateTableName)
            throws IOException {
        if (!hBaseConnectorSystemService.tableExists(avroTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, avroTableName);
            List<String> avroColumnFamilies = new ArrayList<>();
            avroColumnFamilies.add(HProjectHBaseConstants.HPACKET_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(avroTableName, avroColumnFamilies);
        }
        if (!hBaseConnectorSystemService.tableExists(timelineTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, timelineTableName);
            List<String> timelineColumnFamilies = getTimelineColumnFamilies();
            hBaseConnectorSystemService.createTable(timelineTableName, timelineColumnFamilies);
        }
        if (!hBaseConnectorSystemService.tableExists(errorTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, errorTableName);
            List<String> tableErrorColumnFamilies = new ArrayList<>();
            tableErrorColumnFamilies.add(HProjectHBaseConstants.ERROR_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(errorTableName, tableErrorColumnFamilies);
        }
        if (!hBaseConnectorSystemService.tableExists(avroEventTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, avroEventTableName);
            List<String> eventColumnFamilies = new ArrayList<>();
            eventColumnFamilies.add(HProjectHBaseConstants.EVENT_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(avroEventTableName, eventColumnFamilies);
        }
        if (!hBaseConnectorSystemService.tableExists(alarmTableName)) {
            LOGGER.debug(CREATING_TABLE_MESSAGE, alarmTableName);
            List<String> alarmColumnFamilies = new ArrayList<>();
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY);
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY);
            alarmColumnFamilies.add(HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(alarmTableName, alarmColumnFamilies);
        }
        if(! hBaseConnectorSystemService.tableExists(eventRuleStateTableName)){
            LOGGER.debug(CREATING_TABLE_MESSAGE, eventRuleStateTableName);
            List<String> eventRuleStateColumnFamilies = new ArrayList<>();
            eventRuleStateColumnFamilies.add(HProjectHBaseConstants.RULE_COLUMN_FAMILY);
            hBaseConnectorSystemService.createTable(eventRuleStateTableName, eventRuleStateColumnFamilies);
        }
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
