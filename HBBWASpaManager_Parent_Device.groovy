/*
 *  Hubitat BWA Spa Manager
 *  -> Parent Device Driver
 *
 *  Copyright 2023 Kurt Sannders
 *   based on work Copyright 2020 Richard Powell that he did for Hubitat
 *
 *  Copyright 2020 Richard Powell
 *   based on work Copyright 2020 Nathan Spencer that he did for SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  CHANGE HISTORY
 *  VERSION     DATE            NOTES
 *  0.9.0       2020-01-30      Initial release with basic access and control of spas
 *  1.0.0       2020-01-31      Updated UI and icons as well as switch functionality that can be controlled with
 *                              Alexa. Added preference for a "Default Temperature When Turned On"
 *  1.1.0       2020-06-03      Additional functionality for aux, temperature range, and heat modes
 *  1.1.1       2020-07-26      Adjusted icons to better match functionality for aux, temperature range and heat modes
 *                              and removed duplicate tile declaration
 *  1.1.2b      2020-09-17      Modified / validated to work on Hubitat
 *  1.1.3       2020-10-11      Major rewrite of this driver to work with Hubitat's Parent-Child device driver model
 *  1.1.4       2020-10-11      Support the remaining device types except Blower, more code clean-up
 *  1.2.0       2023-11-11      V1.2.0 code stream maintained by Kurt Sanders
 *                              Moved hardcoded logging in driver to UI preferences and added expire timeout logic.
 *                              Added 'Switch Capability' in device parent for:
 *                                  Value: On (TempRange: high, ReadyMode: Ready)
 *                                  Value: Off (TempRange: low, ReadyMode: Rest)
 *                              Added capability "Actuator" and attribute "ReadyMode", "enum", ["Ready", "Rest"] with command "setReadyMode"
 *                              Added attribute "TempRange", "enum", ["low", "high"] and command "setTempRange"
 *                              Move hardcoded logging in app and drivers to app and device UI input fields for easier end user maintenance.
 *                              Added app and drivers to HPM Public Install App
 */

import groovy.transform.Field
import groovy.time.TimeCategory

@Field static String NAMESPACE = "kurtsanders"

@Field static String DEVICE_NAME_PREFIX = "HB BWA SPA"
@Field static String PARENT_DEVICE_NAME = "HB BPA SPA Parent"
@Field static final String VERSION = "1.2.0"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/balboa-spa-controller-app/18194/45"

@Field static String THERMOSTAT_CHILD_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static String SWITCH_CHILD_DEVICE_NAME = "HB BWA SPA Switch"


metadata {
    definition (name: PARENT_DEVICE_NAME, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Refresh"
        capability "Configuration"
        capability "Actuator"
        capability "Switch"

        /* This is a list of attributes sent to us right after we successfully login
         * to Balboa and pull details about Spas linked to the user's account.
         *
         * Hubitat requires attributes to be defined in order for sendEvent(...) to
         * be able to update that attribute.
         */
        attribute "create_user_id", "string"
        attribute "deviceId", "string" // renamed from "device_id"
        attribute "update_user_id", "string"
        attribute "updated_at", "string"
        attribute "__v", "string"
        attribute "active", "string"
        attribute "created_at", "string"
        attribute "_id", "string"

        // Additional attributes
        attribute "spaStatus", "string"
        attribute "ReadyMode", "enum", ["Ready", "Rest"]
        attribute "TempRange", "enum", ["low", "high"]

        command "setReadyMode"
        command "setTempRange"
        command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
                                [name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
    }
}

@Field static Map PUMP_BUTTON_MAP = [
    1: 4, // Pump 1 maps to Balboa API Button #4
    2: 5, // Pump 2 maps to Balboa API Button #5 etc.
    3: 6,
    4: 7,
    5: 8,
    6: 9]

@Field static Map LIGHT_BUTTON_MAP = [
    1: 17, // Light 1 maps to Balboa API Button #17 etc.
    2: 18]

@Field static Map AUX_BUTTON_MAP = [
    1: 22, // Aux 1 maps to Balboa API Button #22 etc.
    2: 23
    ]

@Field static Map BUTTON_MAP = [
    Blower: 12,
    Mister: 14,
    Aux1: 22,
    Aux2: 23,
    TempRange: 80,
    HeatMode: 81]

def installed() {
}

def updated() {
	logDebug "updated..."
	checkLogLevel()
}

def on() {
    if (device.currentValue("ReadyMode") == "Rest") {
        setReadyMode()
    }
    if (device.currentValue("TempRange") == "low") {
        setTempRange()
    }
}

def off() {
    if (device.currentValue("ReadyMode") == "Ready") {
        setReadyMode()
    }
    if (device.currentValue("TempRange") == "high") {
        setTempRange()
    }
}

void setReadyMode() {
    logDebug "In parent setReadyMode..."
    ReadyMode = device.currentValue("ReadyMode")
    logDebug "Device current mode is ${ReadyMode}"
    text_mode = ReadyMode == "Ready" ? "Rest" : "Ready"
    sendEvent(name: "ReadyMode", value: text_mode)
    logDebug "Setting mode to ${text_mode}"
    sendCommand("Button", BUTTON_MAP.HeatMode)
}

void setTempRange() {
    logDebug "In parent setTempRange..."
    TempRange = device.currentValue("TempRange")
    logDebug "Device current heating mode is ${TempRange}"
    temp_range = TempRange == "low" ? "high" : "high"
    sendEvent(name: "TempRange", value: temp_range)
    logDebug "Setting hTemp Range to ${temp_range}"
    sendCommand("Button", BUTTON_MAP.TempRange)
}

def sendCommand(action, data) {
    parent.sendCommand(device.currentValue("deviceId"), action, data)
    runIn(2, refresh)
}

def parseDeviceData(Map results) {
    results.each {name, value ->
        sendEvent(name: name, value: value, displayed: true)
    }
}

def createChildDevices(spaConfiguration) {
    // Thermostat
    fetchChild(true, "Thermostat", "Thermostat")

    /* The incoming spaConfiguration has a list of all the possible add-on devices like
       pumps, lights, etc. mapped to a boolean indicating whether or not this particular
       hot tub actually has that specific device installed on it.

       Iterate through all the possible add-on devices and if the hot tub we're working
       with actually has that device installed on it then we will go ahead and create a
       child device for it (passing "true" as the first parameter to fetchChild(...) will
       have it go and create a device if it doesn't exist already.
    */

    // Pumps
    spaConfiguration.each { k, v ->
        if (k.startsWith("Pump") && v == true) {
            def pumpNumber = k[4].toInteger()
            fetchChild(true, "Switch", "Pump ${pumpNumber}", PUMP_BUTTON_MAP[pumpNumber])
        }
    }

    // Lights
    spaConfiguration.each { k, v ->
        if (k.startsWith("Light") && v == true) {
            def lightNumber = k[5].toInteger()
            fetchChild(true, "Switch", "Light ${lightNumber}", LIGHT_BUTTON_MAP[lightNumber])
        }
    }

    // Blower
    if (spaConfiguration["Blower"] == true) {
        // TODO: Support Blower properly. It's not a "Switch" device type.
        //fetchChild(true, ???, "Blower", BUTTON_MAP.Blower)
    }

    // Aux
    spaConfiguration.each { k, v ->
        if (k.startsWith("Aux") && v == true) {
            def lightNumber = k[3].toInteger()
            fetchChild(true, "Switch", "Aux ${lightNumber}", AUX_BUTTON_MAP[lightNumber])
        }
    }

    // Mister
    if (spaConfiguration["Mister"] == true) {
        fetchChild(true, "Switch", "Mister", BUTTON_MAP.Mister)
    }
}

def parsePanelData(encodedData) {
    byte[] decoded = encodedData.decodeBase64()
    logDebug "==> decoded (${decoded.size()} members => ${decoded}"

    def is24HourTime = (decoded[13] & 2) != 0 ? true : false
    def currentTimeHour = decoded[7]
    def currentTimeMinute = decoded[8]

    def temperatureScale = (decoded[13] & 1) == 0 ? "F" : "C"
    def actualTemperature = decoded[6]

    def targetTemperature = decoded[24]
    def isHeating = (decoded[14] & 48) != 0
    def heatingMode = (decoded[14] & 4) == 4 ? "high" : "low"
    def heatMode
    switch (decoded[9]) {
        case 0:
            heatMode = "Ready"
            break;
        case 1:
            heatMode = "Rest"
            break;
        case 2:
            heatMode = "Ready in Rest"
            break;
        default:
            heatMode = "None"
    }

    // Send events to Thermostat child device
    def thermostatChildDevice = fetchChild(false, "Thermostat", "Thermostat")
    if (thermostatChildDevice != null) {
        thermostatChildDevice.sendEventsWithUnits([
            [name: "temperature", value: actualTemperature, unit: temperatureScale],
            [name: "heatingSetpoint", value: targetTemperature, unit: temperatureScale]
        ])
        thermostatChildDevice.sendEvents([
            [name: "thermostatMode", value: isHeating ? "heat" : "off"],
            [name: "thermostatOperatingState", value: isHeating ? "heating" : "idle"],
        ])
    }

    def filtermode
    switch (decoded[13] & 12) {
        case 4:
            filterMode = "Filter 1"
            break;
        case 8:
            filterMode = "Filter 2"
            break;
        case 12:
            filterMode = "Filter 1 & 2"
            break;
        case 0:
        default:
            filterMode = "Off"
    }

    def accessibilityType
    switch (decoded[13] & 48) {
        case 16:
            accessibilityType = "Pump Light"
            break;
        case 32:
        case 42:
            accessibilityType = "None"
            break;
        default:
            accessibilityType = "All"
    }

    // Pumps
    def pumpState = []
    pumpState[0] = null
    def pump1ChildDevice = fetchChild(false, "Switch", "Pump 1")
    if (pump1ChildDevice != null) {
        switch (decoded[15] & 3) { // Pump 1
            case 1:
            	pumpState[1] = "low"
                break
            case 2:
            	pumpState[1] = "high"
                break
            default:
            	pumpState[1] = "off"
        }
        pump1ChildDevice.parse(pumpState[1])
    }
    def pump2ChildDevice = fetchChild(false, "Switch", "Pump 2")
    if (pump2ChildDevice != null) {
        switch (decoded[15] & 12) { // Pump 2
            case 4:
                pumpState[2] = "low"
                break
            case 8:
                pumpState[2] = "high"
                break
            default:
                pumpState[2] = "off"
        }
        pump2ChildDevice.parse(pumpState[2])
    }
    def pump3ChildDevice = fetchChild(false, "Switch", "Pump 3")
    if (pump3ChildDevice != null) {
        switch (decoded[15] & 48) { // Pump 3
            case 16:
            	pumpState[3] = "low"
                break
            case 32:
            	pumpState[3] = "high"
                break
            default:
            	pumpState[3] = "off"
        }
        pump3ChildDevice.parse(pumpState[3])
    }
    def pump4ChildDevice = fetchChild(false, "Switch", "Pump 4")
    if (pump4ChildDevice != null) {
        switch (decoded[15] & 192) {
            case 64:
            	pumpState[4] = "low"
                break
            case 128:
            	pumpState[4] = "high"
                break
            default:
            	pumpState[4] = "off"
        }
        pump4ChildDevice.parse(pumpState[4])
    }
    def pump5ChildDevice = fetchChild(false, "Switch", "Pump 5")
    if (pump5ChildDevice != null) {
        switch (decoded[16] & 3) {
            case 1:
            	pumpState[5] = "low"
                break
            case 2:
            	pumpState[5] = "high"
                break
            default:
            	pumpState[5] = "off"
        }
        pump5ChildDevice.parse(pumpState[5])
    }
    def pump6ChildDevice = fetchChild(false, "Switch", "Pump 6")
    if (pump6ChildDevice != null) {
        switch (decoded[16] & 12) {
            case 4:
            	pumpState[6] = "low"
                break
            case 8:
            	pumpState[6] = "high"
                break
            default:
            	pumpState[6] = "off"
        }
        pump6ChildDevice.parse(pumpState[6])
    }

    // TODO: Support Blower properly. It's not a switch device type
    switch (decoded[17] & 12) {
        case 4:
        	blowerState = "low"
            break
        case 8:
        	blowerState = "medium"
            break
        case 12:
        	blowerState = "high"
            break
        default:
        	blowerState = "off"
    }

    // Lights
    def lightState = []
    lightState[0] = null
    def light1ChildDevice = fetchChild(false, "Switch", "Light 1")
    if (light1ChildDevice != null) {
        lightState[1] = (decoded[18] & 3) != 0
        light1ChildDevice.parse(lightState[1])
    }
    def light2ChildDevice = fetchChild(false, "Switch", "Light 2")
    if (light2ChildDevice != null) {
        lightState[2] = (decoded[18] & 12) != 0
        light2ChildDevice.parse(lightState[2])
    }

    // Mister
    def misterChildDevice = fetchChild(false, "Switch", "Mister")
    def misterState = null
    if (misterChildDevice != null) {
        misterState = (decoded[19] & 1) != 0
        misterChildDevice.parse(misterState)
    }

    // Aux
    def auxState = []
    auxState[0] = null
    def aux1ChildDevice = fetchChild(false, "Switch", "Aux 1")
    if (aux1ChildDevice != null) {
        auxState[1] = (decoded[19] & 8) != 0
        aux1ChildDevice.parse(auxState[1])
    }
    def aux2ChildDevice = fetchChild(false, "Switch", "Aux 2")
    if (aux2ChildDevice != null) {
        auxState[2] = (decoded[19] & 16) != 0
        aux2ChildDevice.parse(auxState[2])
    }

    def wifiState
    switch (decoded[16] & 240) {
    	case 0:
        	wifiState = "OK"
            break
        case 16:
        	wifiState = "Spa Not Communicating"
            break
        case 32:
        	wifiState = "Startup"
            break
        case 48:
        	wifiState = "Prime"
            break
        case 64:
        	wifiState = "Hold"
            break
        case 80:
        	wifiState = "Panel"
            break
    }

    def pumpStateStatus
    if (decoded[15] < 1 && decoded[16] < 1 && (decoded[17] & 3) < 1) {
    	pumpStateStatus = "Off"
    } else {
    	pumpStateStatus = isHeating ? "Low Heat" : "Low"
    }

    if (actualTemperature == 255) {
    	actualTemperature = device.currentValue("temperature") * (temperatureScale == "C" ? 2.0F : 1)
    }

    if (temperatureScale == "C") {
    	actualTemperature /= 2.0F
    	targetTemperature /= 2.0F
    }

    logDebug ("Actual Temperature: ${actualTemperature}\n"
                + "Current Time Hour: ${currentTimeHour}\n"
                + "Current Time Minute: ${currentTimeMinute}\n"
                + "Is 24-Hour Time: ${is24HourTime}\n"
                + "Temperature Scale: ${temperatureScale}\n"
                + "Target Temperature: ${targetTemperature}\n"
                + "Filter Mode: ${filterMode}\n"
                + "Accessibility Type: ${accessibilityType}\n"
                + "Heating Mode: ${heatingMode}\n"
                + "lightState[1]: ${lightState[1]}\n"
                + "lightState[2]: ${lightState[2]}\n"
                + "Heat Mode: ${heatMode}\n"
                + "Is Heating: ${isHeating}\n"
                + "pumpState[1]: ${pumpState[1]}\n"
                + "pumpState[2]: ${pumpState[2]}\n"
                + "pumpState[3]: ${pumpState[3]}\n"
                + "pumpState[4]: ${pumpState[4]}\n"
                + "pumpState[5]: ${pumpState[5]}\n"
                + "pumpState[6]: ${pumpState[6]}\n"
                + "blowerState: ${blowerState}\n"
                + "misterState: ${misterState}\n"
                + "auxState[1]: ${auxState[1]}\n"
                + "auxState[2]: ${auxState[2]}\n"
                + "pumpStateStatus: ${pumpStateStatus}\n"
                + "wifiState: ${wifiState}\n"
             )

    sendEvent(name: "spaStatus", value: "${heatMode}\n${isHeating ? "heating to ${targetTemperature}°${temperatureScale}" : "not heating"}")
    sendEvent(name: "ReadyMode", value: "${heatMode}")
    sendEvent(name: "TempRange", value: "${heatingMode}")
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    sendEvent(name: "updated_at", value: "${now}")


    if (device.currentValue("ReadyMode") == "Ready" && device.currentValue("TempRange") == "high") {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
}

def fetchChild(createIfDoesntExist, String type, String name, Integer balboaApiButtonNumber = 0) {
    String thisId = device.id
    def childDeviceName = "${thisId}-${name}"
    logTrace "childDeviceName: '${childDeviceName}"

    def cd = getChildDevice(childDeviceName)
    if (!cd && createIfDoesntExist) {
        def driverName = "${DEVICE_NAME_PREFIX} ${type}"

        logInfo "Adding Child Device. Driver: '${driverName}', Name: '${childDeviceName}'"
        cd = addChildDevice(NAMESPACE, driverName, childDeviceName, [name: "${device.displayName} {$name}", isComponent: true])

        // Switches will need to know their respective Balboa API Button IDs
        if (type == "Switch" && balboaApiButtonNumber > 0) {
            cd.setBalboaAPIButtonNumber(balboaApiButtonNumber)
        }
    }
    return cd
}

void refresh() {
    logDebug "BWA Cloud Refresh Requested"
    parent.pollChildren()
}

/*******************************************************************
 *** Preference Helpers ***
/*******************************************************************/

String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtHelpInfo(String str) {
	String info = "${PARENT_DEVICE_NAME} v${VERSION}"
	String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
	String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
	String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

/*******************************************************************
 ***** Logging Functions
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
*/

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:"3", type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"30", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
		logMsg += " for ${LOG_TIMES[levelInfo.time]}"
		runIn(60*levelInfo.time, logsOff)
	}
	logInfo(logMsg)
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
	Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
	Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
	Integer level = settings.logLevel as Integer ?: 3
	Integer time = settings.logLevelTime as Integer ?: 0
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	logWarn "Debug logging toggle disabled..."
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		device.updateSetting("logLevel",[value:"2", type:"enum"])
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${device.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=1) log.warn "${device.displayName}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=2) log.info "${device.displayName}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=3) log.debug "${device.displayName}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=4) log.trace "${device.displayName}: ${msg}"
}