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

package it.acsoftware.hyperiot.storm.hdfs.partitioner;

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import org.apache.hadoop.fs.Path;
import org.apache.storm.hdfs.common.Partitioner;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

/**
 * This class tells to Storm where it must save hdfs file
 */
@SuppressWarnings("unused")
public class HyperiotPartitioner implements Partitioner, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getILoggerFactory().getLogger(HyperiotPartitioner.class.getName());
    /**
     * File directory depth
     */
    private String depth;

    private static String day(long packetId, Calendar calendar) {
        return packetId + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR +
            (calendar.get(Calendar.MONTH) + 1) + Path.SEPARATOR +
            calendar.get(Calendar.DAY_OF_MONTH) + Path.SEPARATOR;
    }

    @Override
    public String getPartitionPath(Tuple tuple) {
        log.debug("Get partition path with depth {}", depth);
        HPacket hPacket = (HPacket)tuple.getValueByField("packet");
        long packetId = hPacket.getId();
        log.debug("HPacket ID to save: {}", packetId);
        long timestamp = (long) hPacket.getFieldsMap().get(hPacket.getTimestampField()).getFieldValue();
        log.debug("HPacket timestamp in Unix Epoch time: {}", timestamp);
        Calendar calendar = Calendar.getInstance();
        String path = null;
        try {
            Method method = HyperiotPartitioner.class.getDeclaredMethod(depth, long.class, Calendar.class);
            path = (String) method.invoke(null, packetId, calendar);
            log.debug("Path which save HPacket to: {}", path);
        } catch (NoSuchMethodException e) {
            log.error("No such method");
            e.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String hour(long packetId, Calendar calendar) {
        return packetId + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR +
            (calendar.get(Calendar.MONTH) + 1) + Path.SEPARATOR +
            calendar.get(Calendar.DAY_OF_MONTH) + Path.SEPARATOR +
            calendar.get(Calendar.HOUR_OF_DAY) + Path.SEPARATOR;
    }

    public static String month(long packetId, Calendar calendar) {
        return packetId + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR +
            (calendar.get(Calendar.MONTH) + 1) + Path.SEPARATOR;
    }

    public static String quarter(long packetId, Calendar calendar) {
        int quarter = calendar.get(Calendar.MONTH) < 4 ? 1 : calendar.get(Calendar.MONTH) < 8 ? 2 : 3;
        return packetId + Path.SEPARATOR +
            quarter + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR;
    }

    public static String semester(long packetId, Calendar calendar) {
        int semester = calendar.get(Calendar.MONTH) < 6 ? 1 : 2;
        return packetId + Path.SEPARATOR +
            semester + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR;
    }

    /**
     * Set file directory depth
     * @param depth File directory depth
     * @return this Partitioner
     */
    public HyperiotPartitioner withDepth(String depth) {
        this.depth = depth;
        return this;
    }

    public static String year(long packetId, Calendar calendar) {
        return packetId + Path.SEPARATOR +
            calendar.get(Calendar.YEAR) + Path.SEPARATOR;
    }

}
