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

package it.acsoftware.hyperiot.hproject.model.hbase;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class supports queries for timeline.
 * Given a lower bound timestamp and an upper bound one, it contains the following information:
 * - total number of HPacket instances registered between the timestamps;
 * - the first "delta" instances encountered between the timestamps, where delta refers to pagination delta, and
 *   it must be lower than HBase max scan page size;
 * - the first and last timestamps delimiting the retrieved page, so the client can request the following page,
 *   having the next timestamp (last timestamp of previous page)
 */
@SuppressWarnings("unused")
public class HPacketCount {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long hPacketId;
    private long totalCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long hDeviceId;

    public HPacketCount() {
        totalCount = 0L;
    }

    public Long getHPacketId() {
        return hPacketId;
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public void setHPacketId(Long hPacketId) {
        this.hPacketId = hPacketId;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public Long gethDeviceId() {
        return hDeviceId;
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public void sethDeviceId(Long hDeviceId) {
        this.hDeviceId = hDeviceId;
    }
}
