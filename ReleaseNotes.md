# HyperIoT 2.2.4 Release Notes

- Removing burgazzoli plugin and generating karaf features directly from the xml
- Upgrading compatibility to gradle 7 and 8
- Removing useless gradle task publishHIT and buildHIT
- Bug: Fixed require image view type on area persist
- Bug: Fixed returning single value json and not array when scanning for hpacket and results contains just one packet or device
- Update: Added /hyperiot/services/version end point which return current services version