# HBBWASpaManager 
<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/hot-tub.png" width="50">

### Integrate your spa to Hubitat
1. View & set spa temperature
2. Turn on/off lights, pumps and aux devices
3. Automate High/Low & Rest/Ready modes to reduce energy usage

### Hubitat App Name:
* BWA Spa Manager for Hubitat (forked from [richardpowellus/HBBWASpaManager](https://github.com/richardpowellus/HBBWASpaManager))

#### Requirements
- Spa equipped with a [Balboa Cloud Spa Controller](https://www.balboawatergroup.com/bwa)
- [Hubitat Hub](https://hubitat.com/)
- [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/)

#### Version 1.2.0 - 11-11-2023

* V1.2.0 code stream maintained by Kurt Sanders (Previous Versions maintained by [Richard Powell](https://github.com/richardpowellus/HBBWASpaManager))
* Moved hardcoded logging in driver to UI preferences and added expire timeout logic.
* Added 'Switch Capability' in device parent for:
	* Value: On (TempRange: high, ReadyMode: Ready)
	* Value: Off (TempRange: low, ReadyMode: Rest)
* Added capability "Actuator" and attribute "ReadyMode", "enum", ["Ready", "Rest"] with command "setReadyMode"
* Added attribute "TempRange", "enum", ["low", "high"] and command "setTempRange"
* Move hardcoded logging in app and drivers to app and device UI input fields for easier end user maintenance.
* Added app and drivers to [HPM Public Install App](https://hubitatpackagemanager.hubitatcommunity.com/)
