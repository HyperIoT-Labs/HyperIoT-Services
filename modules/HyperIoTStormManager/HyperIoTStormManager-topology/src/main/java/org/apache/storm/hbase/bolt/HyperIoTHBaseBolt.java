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

package org.apache.storm.hbase.bolt;

import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Author Francesco Salerno
 * This class is a customized version for HBaseBolt
 *
 * We need to customize the execute's method of this class because we need that this method
 * throw an exception when there is an hbase server failure.
 * In this way we create a condition that we can use to implement a strategy to recover from this event.
 * For the moment the strategy is to use a DLQ to store message for a certain period of time and retry
 * the operation after hbase is online.
 */
public class HyperIoTHBaseBolt extends HBaseBolt {

    private static final Logger LOG = LoggerFactory.getLogger(HyperIoTHBaseBolt.class);

    public HyperIoTHBaseBolt(String tableName, HBaseMapper mapper) {
        super(tableName, mapper);
    }

    @Override
    public void execute(Tuple tuple) {
        try {
            LOG.debug("HyperIoTHBaseBolt start execute method");
            if (batchHelper.shouldHandle(tuple)) {
                LOG.debug("HyperIoTHBaseBolt start batchHelper.shouldHandle(tuple)");
                byte[] rowKey = this.mapper.rowKey(tuple);
                ColumnList cols = this.mapper.columns(tuple);
                List<Mutation> mutations = hBaseClient.constructMutationReq(rowKey, cols, writeToWAL? Durability.SYNC_WAL : Durability.SKIP_WAL);
                batchMutations.addAll(mutations);
                batchHelper.addBatch(tuple);
                LOG.debug("HyperIoTHBaseBolt end batchHelper.shouldHandle(tuple)");
            }
            if (batchHelper.shouldFlush()) {
                LOG.debug("HyperIoTHBaseBolt start batchHelper.shouldFlush");
                this.hBaseClient.batchMutate(batchMutations);
                LOG.debug("HyperIoTHBaseBolt acknowledging tuples after batchMutate");
                batchHelper.ack();
                batchMutations.clear();
                LOG.debug("HyperIoTHBaseBolt end batchHelper.shouldFlush");
            }
        } catch(Exception e){
            LOG.debug("HyperIoTHBaseBolt Exception happen, exception class is {}",e.getClass().getName());
            batchHelper.fail(e);
            batchMutations.clear();
            throw new RuntimeException("Tuple's sending to HBase failed in HyperIoTHBaseBolt");
        }
        LOG.debug("HyperIoTHBaseBolt finish Execute without Exception ");
    }
}
