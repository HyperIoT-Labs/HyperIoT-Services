#!/usr/bin/env python3
"""a simple sensor data generator that sends to an MQTT broker via paho"""
import sys
import json
import time
import random
from copy import deepcopy
from threading import Thread
from datetime import datetime

import paho.mqtt.client as mqtt

class SensorThread (Thread):
   def __init__(self, name,mqttConfig,miscConfig,sensorConfig):
      Thread.__init__(self)
      self.name = name;
      self.sensorConfig = sensorConfig
      self.mqttConfig = mqttConfig;
      self.miscConfig = miscConfig;
      self.duration = miscConfig["duration"]
      self.waitBeforeSendSec = sensorConfig["waitBeforeSendSec"]
      self.mqttc = mqtt.Client();
      self.outlierConf = {};
      self.sendConf = {};
   def run(self):
       if self.miscConfig["verbose"] :
           print ("Thread '" + self.name + "' started...")
           print ("Connecting to MQTT: ",self.mqttConfig["host"]," Port: ",self.mqttConfig["port"]," User: ", "Basic Topic: ",self.mqttConfig["topic"])

       if self.sensorConfig["username"]:
           self.mqttc.username_pw_set(self.sensorConfig["username"], self.sensorConfig["password"])
       self.mqttc.connect(self.mqttConfig["host"], self.mqttConfig["port"])
       timestampFieldName = "timestamp"
       timestampInUnixSeconds = False
       if "timestamp-field-name" in self.miscConfig:
           timestampFieldName = self.miscConfig["timestamp-field-name"]
       if "timestamp-unix-in-seconds" in self.miscConfig:
            timestampInUnixSeconds = self.miscConfig["timestamp-unix-in-seconds"]
       stop = False;
       sendingOutlier = {};
       sendingOutlierStartTime = {};
       startTime = datetime.now();
       while not stop:
           now = datetime.now(); 
           timeDiff = (now - startTime).total_seconds();
           if timeDiff > self.duration :
               stop = True;
           else :
               if self.sensorConfig["active"]:
                   timePassedBy = int(timeDiff);
                   sendData(self.mqttc,self.mqttConfig["topic"],self.name,self.sensorConfig,timestampFieldName,timestampInUnixSeconds,self.miscConfig["verbose"],self.miscConfig["dry-run"],timePassedBy,self.outlierConf,self.sendConf);
           time.sleep(self.waitBeforeSendSec)
       if self.miscConfig["verbose"]:
           print("\nThread " + self.name +" Terminated")
           

def generate(mqttConfig,miscConfig,sensors):
    keys = list(sensors.keys())
    threads = [];
    for k in keys:
        sensorConfig = sensors[k]
        if sensorConfig["active"] :
            print("Starting thread for sensor: "+k)
            thread = SensorThread(k, mqttConfig,miscConfig, sensorConfig)
            threads.append(thread)
        else :
            print("\nSkippinkg sensor: "+k+" it's not active!")

    for t in threads:
        t.start();

    for t in threads :
        t.join();
        
def sendData(mqttc,baseTopic,sensorName,sensor,timestampFieldName,timestampInUnixSeconds,verbose,dryRun,timePassedBy,outlierConf,sendConf):
        # Fields can be present inside the json config that are not sent to the server
        packet_id = sensorName.rsplit('/', 1)[-1]
        data = {}
        timeVal = time.time();
        if timestampInUnixSeconds:
            data[timestampFieldName]= int(round(timeVal))
        else :
            data[timestampFieldName]= int(round(timeVal*1000))
        
        prepareJson(data,sensor["data"],outlierConf,timePassedBy,sensorName,sendConf)
                
        payload = json.dumps(data)
        if verbose:
            print("\n%s: %s" % (baseTopic + sensorName, payload))
        
        if not dryRun:
            mqttc.publish(baseTopic + sensorName, payload)

def prepareJson(data,jsonObject,outlierConf,timePassedBy,sensorName,sendConf):
    skipDataInModel = ["-range","-outlier-duration-secs","-temperature-outlier-every-secs","-outlier-every-secs","-outlier"];
    for key, value in jsonObject.items():
            if(not is_json_value(value)):
                sendOutlier = False;
                if not any(skipKey in key for skipKey in skipDataInModel ):
                    if not key in outlierConf:
                        outlierConf[key] = {
                            "sendingOutlier":False,
                            "sendingOutlierStartTime":0
                        }
                    if not outlierConf[key]["sendingOutlier"] and key+'-outlier-every-secs' in jsonObject :
                        timePassedByModulo = timePassedBy % jsonObject[key+"-outlier-every-secs"];
                        if timePassedBy > 0 and timePassedByModulo == 0:
                            outlierConf[key]["sendingOutlier"] = True
                            outlierConf[key]["sendingOutlierStartTime"] = datetime.now();
                            print ("\n\n#######################\nstart sending outlier for "+sensorName+" "+key+"!\n#########################\n\n")
                    if outlierConf[key]["sendingOutlier"]:
                        outlierTimePassedBy = (datetime.now() - outlierConf[key]["sendingOutlierStartTime"]).total_seconds();
                        if outlierTimePassedBy >= jsonObject[key+"-outlier-duration-secs"]:
                            outlierConf[key]["sendingOutlier"] = False
                            outlierConf[key]["sendingOutlierStartTime"] = 0
                            print ("stop sending outlier for "+sensorName+" timeout of "+sensorName+" "+key+"!\n#########################\n\n")
                    if key + "-range" in jsonObject:
                        if (not outlierConf[key]["sendingOutlier"]) or (outlierConf[key]["sendingOutlier"] and key + "-outlier" not in jsonObject ):
                            min_val, max_val = jsonObject.get(key + "-range", [0, 100])
                            val = random.uniform(min_val, max_val)
                            data[key] = val
                        else :
                            data[key] = jsonObject.get(key + "-outlier", 0)
                    if key + "-cyclic-vector" in jsonObject:
                        if not key+"-cyclic-vector-index" in sendConf:
                            sendConf[key+"-cyclic-vector-index"] = 0;
                        i = sendConf[key+"-cyclic-vector-index"];
                        data[key] = jsonObject[key][i];
                        i = (i+1) % len(jsonObject[key]);
                        sendConf[key+"-cyclic-vector-index"] = i;
                    if not key+'-outlier' in jsonObject and not key+'-range' in jsonObject and not key+'-cyclic-vector' in jsonObject:
                        data[key] = value
            else:
                data[key] = {}
                prepareJson(data[key],value,outlierConf,timePassedBy,sensorName,sendConf)


def is_json_value(value):
    if isinstance(value, (dict)):
        return True
    try:
        json.loads(value)
        return True
    except (json.JSONDecodeError, TypeError):
        return False


def main(config_path):
    """main entry point, load and validate config and call generate"""
    try:
        with open(config_path) as handle:
            config = json.load(handle)
            mqtt_config = config.get("mqtt", {})
            misc_config = config.get("misc", {})
            sensors = config.get("sensors")
            if not sensors:
                print("no sensors specified in config, nothing to do")
                return

            generate(mqtt_config,misc_config,sensors)
    except IOError as error:
        print("Error opening config file '%s'" % config_path, error)

if __name__ == '__main__':
    if len(sys.argv) == 2:
        main(sys.argv[1])
    else:
        print("usage %s config.json" % sys.argv[0])
