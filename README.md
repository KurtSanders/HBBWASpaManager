# HB BWA SpaManager (Cloud Control & Direct Local TCP)
<img src="https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/hot-tub.png" width="50"> (forked from [richardpowellus/HBBWASpaManager](https://github.com/richardpowellus/HBBWASpaManager)

### Version Notes

* [Change-log & Version Release Features](https://github.com/KurtSanders/HBBWASpaManager/wiki/Features-by-Version)

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
- Spa equipped with a [Balboa Cloud Spa Controller](https://www.amazon.com/Balboa-Water-Group-50350-07-Systems/dp/B0C89NLVCW/ref=sr_1_1?crid=1FLAWP3J3MUQP&dib=eyJ2IjoiMSJ9.3Uf1IdmEfGQFHeyD_LYQy1uF3Q_OyGZBz-9T0A4Du4UCVRb6lTdEGwR7xncq7IuCGIXiwvQCwPgsaIpqDM-7DM1ou8GSc1Ty7NjgJeE18TUdmd6VyaX6pTE-2GSoZ1gmaMi-QrIgUkCU82tEL-V73f6_fI6VKiWVnqkTR0IPM-a_4QMUAOW5pYq8rjF1Ww-aAtIzgsY5vMTLSgMqY85mvgW0weQCd_LaMdWBO6b4XSc._fD5IJd7n4BqA4eBVjncSH3Np0lqbCB82zh4IUAXsws&dib_tag=se&keywords=balboa+spa+module&qid=1741981337&sprefix=balboa+spa+module%2Caps%2C129&sr=8-1)
- [Hubitat Hub](https://hubitat.com/)
- [Hubitat Package Manager](https://hubitatpackagemanager.hubitatcommunity.com/)

### How to install BWA Spa Manager: (Select the desired connection type below)
#### Cloud Control (Uses BWA's cloud server)

1. Connect the spa to using these steps: 
	* [BWA Cloud Spa Controller](https://www.hottuboutpost.com/bwa-wifi-module-for-balboa-bp-series-spa-packs-iphone-android-app-50350/)
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

### HPM Install/Upgrade FAQs:

#### New Installs:
* Install via HPM are highly recommended.  Search for **BWA Spa Manager** and select the **WA Spa Manager - Local App** version.

#### Previous Versions of this App:
* If you try to upgrade a previous version via HPM and notice that your hub has multiple versions of the device drivers or libraries listed below, use HPM to uninstall the app.
* If HPM displays an error with removing this app, use HPM's '**Package Manager Settings**' option and then select ' **Un-Match a Package** to make HPM remove any of it's cached information of the previous versions of this app.   
* Check the hub's drivers code and library code views for residual files listed below, If they exists, manually delete ALL/ANY of the application's drivers, app and libraries files that might still be installed.  This will allow HPM to install as a fresh new install.
* Perform a new install via HPM.  Search for BWA Spa Manager and select the Local version to install.  HPM will automatically launch the Application and create the required spa control devices.
