package it.acsoftware.hyperiot.algorithm.service.postaction;

import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This class deletes jar of Algorithm, which is on HDFS, and drops its HBase table
 *
 * @param <T>
 */
@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.algorithm.model.Algorithm"})
public class AlgorithmPostRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmPostRemoveAction.class.getName());
    private AlgorithmUtil algorithmUtil;
    private HadoopManagerSystemApi hadoopManagerSystemApi;
    private HBaseConnectorSystemApi hBaseConnectorSystemApi;

    @Override
    public void execute(T entity) {
        Runnable r = () -> {
            Algorithm algorithm = (Algorithm) entity;
            AlgorithmPostRemoveAction.this.ereaseHBaseTable(algorithm);
            AlgorithmPostRemoveAction.this.removeAlgorithmJarFromHDFS(algorithm);
        };
        hBaseConnectorSystemApi.executeTask(r);
    }

    private void ereaseHBaseTable(Algorithm algorithm) {
        try {
            // ... and drop HBase table
            LOGGER.debug("Drop HBase table of Algorithm with ID {}", algorithm.getId());
            String tableName = "algorithm_" + algorithm.getId();
            hBaseConnectorSystemApi.disableTable(tableName);
            hBaseConnectorSystemApi.dropTable(tableName);
            LOGGER.debug("HBase table {} has been dropped", tableName);
        } catch (IOException e) {
            throw new HyperIoTRuntimeException(e);
        }
    }

    private void removeAlgorithmJarFromHDFS(Algorithm algorithm) {
        try {
            LOGGER.debug("Removing jar from HDFS {}", algorithm.getAlgorithmFileName());
            if(algorithm.getAlgorithmFileName() != null && !algorithm.getAlgorithmFileName().isEmpty())
                hadoopManagerSystemApi.deleteFile(algorithm.getAlgorithmFileName());
            LOGGER.debug("Jar {} removed ", algorithm.getAlgorithmFileName());
        } catch (Throwable e) {
            throw new HyperIoTRuntimeException(e);
        }
    }

    @Reference
    protected void setAlgorithmUtil(AlgorithmUtil algorithmUtil) {
        this.algorithmUtil = algorithmUtil;
    }

    @Reference
    protected void setHadoopManagerSystemApi(HadoopManagerSystemApi hadoopManagerSystemApi) {
        this.hadoopManagerSystemApi = hadoopManagerSystemApi;
    }

    @Reference
    protected void setHBaseConnectorSystemApi(HBaseConnectorSystemApi hBaseConnectorSystemApi) {
        this.hBaseConnectorSystemApi = hBaseConnectorSystemApi;
    }

}
