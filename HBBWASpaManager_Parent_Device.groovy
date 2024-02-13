/*
 *  Hubitat BWA Spa Manager
 *  -> Parent Device Driver
 *
 *  Copyright 2023/2024 Kurt Sannders
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
 */
#include kurtsanders.SanderSoft-Library
#include kurtsanders.BWA-Library

@Field static String DEVICE_NAME_PREFIX = "HB BWA SPA"
@Field static String PARENT_DEVICE_NAME = "HB BPA SPA Parent"
@Field static final String VERSION = "1.3.1"

@Field static final List VALID_SPA_BYTE_ARRAY = [-1, -81]
@Field static String THERMOSTAT_CHILD_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static String SWITCH_CHILD_DEVICE_NAME = "HB BWA SPA Switch"
@Field static String UNKNOWN = "unknown"
@Field static final List READYMODES = ["Ready", "Rest"]
@Field static final List TEMPRANGES = ["low", "high"]

metadata {
    definition (name: PARENT_DEVICE_NAME, namespace: NAMESPACE, author: AUTHOR_NAME) {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "TemperatureMeasurement"

        attribute "deviceId", "string"
        attribute "blowerState", "enum", ["off","low","medium","high"]
        attribute "blowerPump", "enum", ["true","false"]
        attribute "heatingSetpoint", "number"
        attribute "online", "enum", ["Online","Offline"]
        attribute "pollingInterval", "string"
        attribute "ReadyMode", "enum", READYMODES
        attribute "spaStatus", "string"
        attribute "spaTime", "string"
        attribute "spaTimeLocalDeltaMins", "number"
        attribute "is24HourTime", "enum", ["true","false"]
        attribute "temperature", "number"
        attribute "TempRange", "enum", TEMPRANGES
        attribute "thermostatOperatingState", "string"
        attribute "thermostatSetpoint", "number"
        attribute "updated_at", "string"
        attribute "wifiState", "enum", ["WiFi OK","WiFi Spa Not Communicating","WiFi Startup","WiFi Prime","WiFi Hold","WiFi Panel","WiFi Unnknown"]

        command "setReadyMode"
        command "setSpaToLocalTime"
        command "setTempRange"
    }
}

preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
    input name: "autoSpaTimeSync", type: "bool", title: "Automatically sync spa time to local hub time"
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
	logInfo "Preferences Updated..."
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

void setSpaToLocalTime() {
    def timeData = nowFormatted("HH:mm")
    logDebug "setSpaToLocalTime(): SystemLocalTime = ${timeData}"
    sendCommand("SystemTime", timeData)
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
/*  sendCommand parameters and example values
    SetTemp / 69
    TempUnits / C
    TempUnits / F
    TimeFormat / 12
    TimeFormat / 24
    SystemTime / 01:49
    Filters / <base64 string of bytes dictating the filter cycle>
    Request / <panel request, such as list filter cycles??>
*/
    parent.sendCommand(device.currentValue("deviceId"), action, data)
    runIn(2, refresh)
}

def parseDeviceData(Map results) {
    results.each {name, value ->
        logDebug ("name: ${name}, value ${value}")
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

    // Blower Pump
    // TODO: Support Blower properly. It's not a "Switch" device type.
    // fetchChild(true, ???, "Blower", BUTTON_MAP.Blower)
    sendEvent(name:"blowerPump",value: spaConfiguration["Blower"]?"true":"false" )

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
    logTrace "encodedData= ${encodedData}"
    if (encodedData==null) return
    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    byte[] decoded = encodedData.decodeBase64()

    // Check for a valid SPA array prefix
    if (decoded[1..2] != VALID_SPA_BYTE_ARRAY) {
        if (encodedData != null) {
            logWarn "BWA Cloud Spa Error: encodedData '${encodedData}' is NOT a valid SPA panel data.   Is the Spa Online?"
        } else {
            logWarn "BWA Cloud Spa Error: SPA panel data was (null).  Is the Spa Online?"
            encodedData = "Spa BWA cloud spa data was null"
        }
        sendEvent(name: "online", value: "Offline")
        sendEvent(name: "spaStatus", value: UNKNOWN)
        sendEvent(name: "updated_at", value: "Error: ${encodedData} at: ${now}")
        // Send events to Thermostat child device
        def thermostatChildDevice = fetchChild(false, "Thermostat", "Thermostat")
        if (thermostatChildDevice != null) {
            thermostatChildDevice.sendEvents([
                [name: "temperature", value: -1 ],
                [name: "thermostatMode", value: "heat"],
                [name: "thermostatOperatingState", value: "idle"]
            ])
        }
        // Reset child switch devices switch and speed currentValues when spa is offline
        logDebug "Resetting switch child devices capabilities and attribute state(s) due to spa 'Offline'"
        def switchDevices = getChildDevices()
        switchDevices.each {
            if (it.typeName == "HB BWA SPA Switch") {
                if (it.currentValue('speed')=="${UNKNOWN}") {
                    logDebug "Reset device: '${it.label}' speed to ${UNKNOWN}"
                    it.sendEvent(name: "speed",  value: UNKNOWN, descriptionText: "Auto-reset speed to 'unkown' by spa 'offline'")
                }
                if (it.currentValue('switch')=='on') {
                    logDebug "Reset device: '${it.label}' switch to 'off'"
                    it.sendEvent(name: "switch", value: "off"  , descriptionText: "Auto-reset switch to 'off' by spa 'offline'")
                }
            }
        }
        return false
    }

    // Get Spa Time
    String currentSpaTimeHour = decoded[7].toString().padLeft(2,"0")
    String currentSpaTimeMinute = decoded[8].toString().padLeft(2,"0")
    String is24HourTime = (decoded[13] & 2) != 0 ? "true" : "false"
    String spaTime = Date.parse("HHmm", "${currentSpaTimeHour}${currentSpaTimeMinute}").format((is24HourTime.toBoolean())?"HH:mm":"h:mm a")

    // Get delta mins from spa internal clock to hub's localtime
    use(TimeCategory){
        def locaTime = Date.parse("HH:mm",nowFormatted("HH:mm"))
        logTrace "locaTime = ${locaTime}"
        def spaHHmm = Date.parse("HH:mm","${currentSpaTimeHour}:${currentSpaTimeMinute}")
        logTrace "spaHHmm = ${spaHHmm}"
        def duration = TimeCategory.minus(spaHHmm, locaTime)
        logTrace "duration= ${duration}"
        Integer durationMins = duration.hours*60 + duration.minutes
        logTrace "durationMins= ${durationMins}"
        sendEvent(name: "spaTimeLocalDeltaMins", value: "${durationMins}")
    }

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
            [name: "heatingSetpoint", value: targetTemperature, unit: temperatureScale],
            [name: "thermostatSetpoint", value: actualTemperature, unit: temperatureScale]
        ])
        thermostatChildDevice.sendEvents([
            [name: "thermostatMode", value: "heat"],
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

    // Spa Pumps
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
    if (device.currentValue("blowerPump")) {
        sendEvent(name: "blowerState", value: blowerState)
    } else {
        logDebug "blowerPump currentValue is ${device.currentValue("blowerPump")}, blowerState will not be set/updated"
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
        wifiState = "WiFi OK"
        break
        case 16:
        wifiState = "WiFi Spa Not Communicating"
        break
        case 32:
        wifiState = "WiFi Startup"
        break
        case 48:
        wifiState = "WiFi Prime"
        break
        case 64:
        wifiState = "WiFi Hold"
        break
        case 80:
        wifiState = "WiFi Panel"
        break
        default:
            wifiState = "Unknown"
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


    // Create events for the parent spa device
    sendEvent(name: "spaStatus",                 value: "${heatMode} ${isHeating ? "heating to ${targetTemperature}°${temperatureScale}" : "not heating"}")
    sendEvent(name: "ReadyMode",                 value: "${heatMode}")
    sendEvent(name: "TempRange",                 value: "${heatingMode}")
    sendEvent(name: "updated_at",                value: "Updated at: ${now}")
    sendEvent(name: "online",                    value: "Online")
    sendEvent(name: "wifiState",                 value: "${wifiState}")
    sendEvent(name: "temperature",               value: actualTemperature, unit: temperatureScale)
    sendEvent(name: "heatingSetpoint",           value: targetTemperature, unit: temperatureScale)
    sendEvent(name: "thermostatSetpoint",        value: actualTemperature, unit: temperatureScale)
    sendEvent(name: "thermostatOperatingState",  value: isHeating ? "heating" : "idle")
    sendEvent(name: "spaTime",  value: "${spaTime}" )
    sendEvent(name: "is24HourTime",  value: "${is24HourTime}" )

    if (device.currentValue("ReadyMode") == "Ready" && device.currentValue("TempRange") == "high") {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }

    logTrace ("<br><ol><li>Actual Temperature   : ${actualTemperature}"
              + "<li>Current Time Hour          : ${currentSpaTimeHour}"
              + "<li>Current Time Minute        : ${currentSpaTimeMinute}"
              + "<li>Is 24-Hour Time            : ${is24HourTime}"
              + "<li>Temperature Scale          : ${temperatureScale}"
              + "<li>Target Temperature         : ${targetTemperature}"
              + "<li>Filter Mode                : ${filterMode}"
              + "<li>Accessibility Type         : ${accessibilityType}"
              + "<li>Heating Mode               : ${heatingMode}"
              + "<li>lightState[1]              : ${lightState[1]}"
              + "<li>lightState[2]              : ${lightState[2]}"
              + "<li>Heat Mode                  : ${heatMode}"
              + "<li>Is Heating                 : ${isHeating}"
              + "<li>pumpState[1]               : ${pumpState[1]}"
              + "<li>pumpState[2]               : ${pumpState[2]}"
              + "<li>pumpState[3]               : ${pumpState[3]}"
              + "<li>pumpState[4]               : ${pumpState[4]}"
              + "<li>pumpState[5]               : ${pumpState[5]}"
              + "<li>pumpState[6]               : ${pumpState[6]}"
              + "<li>blowerState                : ${blowerState}"
              + "<li>misterState                : ${misterState}"
              + "<li>auxState[1]                : ${auxState[1]}"
              + "<li>auxState[2]                : ${auxState[2]}"
              + "<li>pumpStateStatus            : ${pumpStateStatus}"
              + "<li>wifiState                  : ${wifiState}</ol>"
             )
    return true
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
    logInfo "BWA Cloud Refresh Requested"
    parent.pollChildren(override=true)
}