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

package it.acsoftware.hyperiot.hproject.model.hbase.timeline;

/**
 * This enum contains column families of an HBase table, which is dedicated to timeline queries.
 * Remember: there is one table per project.
 */
public enum TimelineColumnFamily {

    YEAR("year", 0), MONTH("month", 1), DAY("day", 2), HOUR("hour", 3),
    MINUTE("minute", 4), SECOND("second", 5), MILLISECOND("millisecond", 6);

    private String name;
    private int order;

    TimelineColumnFamily(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

}
