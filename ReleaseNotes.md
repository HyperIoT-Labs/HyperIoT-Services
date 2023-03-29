# HyperIoT 2.2.4 Release Notes

- Removing burgazzoli plugin and generating karaf features directly from the xml
- Upgrading compatibility to gradle 7 and 8
- Removing useless gradle task publishHIT and buildHIT
- Bug: Fixed require image view type on area persist
- Bug: Fixed returning single value json and not array when scanning for hpacket and results contains just one packet or device
- Bug: Added png format to area image
- Update: Added default area view type - IMAGE
- Update: Added /hyperiot/services/version end point which return current services version
- Update: Adding new column family HBase in order to support file streaming and store them inside HBase
- Update: Exposing packet attachments download from rest services
- Update: Disabling user registration from property