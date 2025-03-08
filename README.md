# HB BWA SpaManager (Cloud Control & Direct Local TCP)
<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/hot-tub.png" width="50"> (forked from [richardpowellus/HBBWASpaManager](https://github.com/richardpowellus/HBBWASpaManager)

<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/Cloud-Local-Connection-Image.jpg">

---

<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/HE%20dashboard-1.jpg">

* Dashboard and Tiles shown above have been customized by [Tile Master by BPTWorld apps](https://community.hubitat.com/t/release-tile-master-display-multiple-devices-that-can-be-controlled-from-the-tile/23140)

<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/readme.png" width="50">[Change-log & Version Release Features](https://github.com/KurtSanders/HBBWASpaManager/wiki/Features-by-Version)

<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/Help-Logo.png" width="50">[Hubitat Community Support Thread](https://community.hubitat.com/t/release-hb-bwa-spamanager/128842)

### Integrate your spa to the Hubitat environment with the following features:
1. View real-time spa temperature
2. Set spa heating SetPoint temperature
2. Turn on/off lights, pumps, mister, and aux devices
3. Set & automate High/Low & Rest/Ready modes to reduce energy usage
4. Set spa's internal clock to hubitat local time
4. Control spa using Hubitat's Rules/WebCore

#### Requirements
- Spa equipped with a [Balboa Cloud Spa Controller](https://www.balboawatergroup.com/bwa)
- [Hubitat Hub](https://hubitat.com/)
- [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/)

### How to install BWA Spa Manager: (Select the desired connection type below)
#### Cloud Control (Uses BWA's cloud server)

1. Connect the spa to using these steps: 
	* [BWA Cloud Spa Controller](https://www.balboawatergroup.com/getdoc.cfm?id=1623)
	* Create userid and password and verify BWA Cloud connectivity using *BWA Spa Control iOS/Android App*
2. Install Hubitat Package Manager (HPM) on your Hubitat Hub: 
	* [HPM Install Documentation](https://hubitatpackagemanager.hubitatcommunity.com/)
3. Search HPM for * **BWA Spa Manager - Cloud Control**, Install and Configure the App
4. Separate devices will be created according to your spa's custom configuration/features to control temperature, pumps, lights, rest/ready states, mister, etc.

#### Local Control by TCP (Hubitat Hub and Spa must be on the same network)

1. Install Hubitat Package Manager (HPM) on your Hubitat Hub:
	* [HPM Install Documentation](https://hubitatpackagemanager.hubitatcommunity.com/)
2. Select Install and search for * **BWA Spa Manager - Local Control**
3. Install the device and exit HPM
3. Manually create a Virtual Device by selecting '+ Add device' in the Hubitat devices view.  
4. Select 'Balboa Hot Tub Local Driver'
5. Name the created device and input your spa's static ip address in the device's preferences. 
6. Select 'Refresh' in the device command section to populate your hot tub's various sensor states.

### Version Notes

* [Change-log & Version Release Features](https://github.com/KurtSanders/HBBWASpaManager/wiki/Features-by-Version)
