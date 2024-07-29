# HyperIoT Storm Topology Manager

HyperIoT System and REST API for managing Storm topologies.

## Prerequisites

This module require [Apache Storm](http://storm.apache.org/) to be installed on the same host.

## Configuration

The only configuration required is the path to *Storm* base folder which point by default to `/opt/hyperiot/apache-storm-1.2.2`.

This path can be customized by adding the configuration property `it.acsoftware.hyperiot.stormmanager.storm.path` to the
`etc/it.acsoftware.hyperiot.cfg` file located in the **Karaf** folder.
