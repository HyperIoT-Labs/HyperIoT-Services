# MQTT sensors data generator

A simple Python script to generate and publish random sensors value to a MQTT server topic.

## Install prerequisites

```
apt install python python-pip
pip install paho-mqtt
```

## Running the script

Before running the script edit the `config.json` file to set MQTT server endpoint/credentials and sensors data.

```
python mqttgen.py config.json
```

