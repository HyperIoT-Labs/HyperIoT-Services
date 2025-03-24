package it.acsoftware.hyperiot.storm.hdfs;

import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.task.TopologyContext;

import java.util.Map;

public class HyperIoTFileNameFormat implements FileNameFormat {

    private String componentId;
    private int taskId;
    private String path = "/storm";
    private String prefix = "";
    private String extension = ".txt";

    public HyperIoTFileNameFormat() {
    }

    public HyperIoTFileNameFormat withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public HyperIoTFileNameFormat withExtension(String extension) {
        this.extension = extension;
        return this;
    }

    public HyperIoTFileNameFormat withPath(String path) {
        this.path = path;
        return this;
    }

    public void prepare(Map<String, Object> conf, TopologyContext topologyContext) {
        this.componentId = topologyContext.getThisComponentId();
        this.taskId = topologyContext.getThisTaskId();
    }

    public String getName(long rotation, long timeStamp) {
        return this.prefix + this.componentId + "-" + this.taskId + "-" + System.currentTimeMillis() + "-" + rotation + this.extension;
    }

    public String getPath() {
        return this.path;
    }
}
