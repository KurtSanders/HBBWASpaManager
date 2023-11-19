# HBBWASpaManager 
<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/hot-tub.png" width="50"> (forked from [richardpowellus/HBBWASpaManager](https://github.com/richardpowellus/HBBWASpaManager)

<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/Dashbaord Screenshot 1.jpg">

### Integrate your spa to the Hubitat environment with the following features:
1. View real-time temperature
2. Set heatingSetPoint temperature
2. Turn on/off lights, pumps, mister and aux devices
3. Set & automate High/Low & Rest/Ready modes to reduce energy usage
4. Control spa using Hubitat's Rules/WebCore

#### Requirements
- Spa equipped with a [Balboa Cloud Spa Controller](https://www.balboawatergroup.com/bwa)
- [Hubitat Hub](https://hubitat.com/)
- [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/)

### How to install BWA Spa Manager:
1. Connect the spa to using these steps: 
	* [BWA Cloud Spa Controller](https://www.balboawatergroup.com/getdoc.cfm?id=1623)
	* Create userid and password and verify BWA Cloud connectivity using *BWA Spa Control iOS/Android App*
2. Install Hubitat Package Manager (HPM) on your Hubitat Hub: 
	* [HPM Install Documentation](https://hubitatpackagemanager.hubitatcommunity.com/)
3. Search HPM for * **BWA Spa Manager**, Install and Configure the App
4. Separate devices will be created according to the custom configuration/features of your spa to control temperature, pumps, lights, rest/ready states, mister, etc.  

### Version Notes

#### v1.2.0 - Nov-11-2023

* V1.2.0 code stream now maintained by Kurt Sanders (Previous versions were maintained by [Richard Powell](https://github.com/richardpowellus/HBBWASpaManager))
* Changed namespace to 'kurtsanders' in app and drivers
* Moved hardcoded logging values in driver to UI preferences and added logging expire timeout logic.
* Added 'Switch Capability' in device parent for:
	* Value: On (TempRange: high, ReadyMode: Ready)
	* Value: Off (TempRange: low, ReadyMode: Rest)
* Added capability "Actuator" and attribute "ReadyMode", "enum", ["Ready", "Rest"] with command "setReadyMode"
* Added attribute "TempRange", "enum", ["low", "high"] and command "setTempRange"
* Added app and drivers to [HPM Public Install App](https://hubitatpackagemanager.hubitatcommunity.com/) for easier install and updates
