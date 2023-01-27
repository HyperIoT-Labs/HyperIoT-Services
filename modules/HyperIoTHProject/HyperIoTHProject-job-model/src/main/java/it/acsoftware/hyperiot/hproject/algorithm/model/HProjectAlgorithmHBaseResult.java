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

package it.acsoftware.hyperiot.hproject.algorithm.model;

import java.util.HashMap;
import java.util.Map;

public class HProjectAlgorithmHBaseResult {

    private Map<String, Map<String, Map<String, String>>> rows;

    public Map<String, Map<String, Map<String, String>>> getRows() {
        return rows;
    }

    public void setRows(Map<String, Map<String, Map<String, String>>> rows) {
        this.rows = rows;
    }

    public void setRowsFromOriginalMap(Map<byte[], Map<byte[], Map<byte[], byte[]>>> rows) {
        this.rows = new HashMap<>();
        for (byte[] byteRowKey : rows.keySet()) {
            String rowKey = new String(byteRowKey);
            this.rows.put(rowKey, new HashMap<>());
            for (byte[] byteColumnFamily : rows.get(byteRowKey).keySet()) {
                String columnFamily = new String(byteColumnFamily);
                this.rows.get(rowKey).put(columnFamily, new HashMap<>());
                for (byte[] byteColumn : rows.get(byteRowKey).get(byteColumnFamily).keySet()) {
                    String column = new String(byteColumn);
                    String value = new String(rows.get(byteRowKey).get(byteColumnFamily).get(byteColumn));
                    this.rows.get(rowKey).get(columnFamily).put(column, value);
                }

                // add timestamp information

                // why lastIndexOf('_')? At the time of this code version, hbase row key contains project ID,
                // algorithm name and reversed timestamp. Take the latter, and get its original value
                long reversedTimestamp = Long.parseLong(rowKey.substring(rowKey.lastIndexOf('_') + 1));
                long originalTimestamp = Long.MAX_VALUE - reversedTimestamp;
                this.rows.get(rowKey).get(columnFamily).put("timestamp", String.valueOf(originalTimestamp));
            }
        }
    }

}
