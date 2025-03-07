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
@Field static final String VERSION = "2.0.1"

@Field static final List VALID_SPA_BYTE_ARRAY = [-1, -81]
@Field static String THERMOSTAT_CHILD_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static String SWITCH_CHILD_DEVICE_NAME = "HB BWA SPA Switch"
@Field static String UNKNOWN = "unknown"
@Field static final List READYMODES = ["Ready", "Rest"]
@Field static final List TEMPRANGES = ["high", "low"]
@Field static final List COMBOMODES = ["Ready & High Temp", "Ready & Low Temp", "Rest & High Temp", "Rest & Low Temp"]

metadata {
    definition (name: PARENT_DEVICE_NAME, namespace: NAMESPACE, author: AUTHOR_NAME) {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
//        capability "TemperatureMeasurement"
        capability "Momentary"

        attribute "blowerState", "enum", ["off","low","medium","high"]
        attribute "blowerPump", "enum", ["true","false"]
        attribute "cloudStatus", "string"
        attribute "cloudResponseTime", "string"
        attribute "filtermode", "enum", ["off", "cycle 1", "cycle 2", "cycle 1 and 2"]
        attribute "heatingSetpoint", "number"
        attribute "is24HourTime", "enum", ["true","false"]
        attribute "online", "enum", ["Online","Offline"]
        attribute "pollingInterval", "string"
        attribute "ReadyMode", "enum", READYMODES
        attribute "spaPaneliFrame", "string"
        attribute "spaPanelHTML", "string"
        attribute "spaStatus", "string"
        attribute "spaTime", "string"
        attribute "spaTimeLocalDeltaMins", "number"
        attribute "temperature", "number"
        attribute "TempRange", "enum", TEMPRANGES
        attribute "thermostatOperatingState", "string"
        attribute "thermostatSetpoint", "number"
        attribute "updated_at", "string"
        attribute "wifiState", "enum", ["WiFi OK","WiFi Spa Not Communicating","WiFi Startup","WiFi Prime","WiFi Hold","WiFi Panel","WiFi Unnknown"]

        command "setReadyMode"    , [[name:"Set Ready Mode*",      type:"ENUM", description:"Set Ready/Rest Mode of the Spa", constraints:READYMODES]]
        command "setTempRange"    , [[name:"Set Temp Range*",      type:"ENUM", description:"Set Temperature Range of the Spa", constraints:TEMPRANGES]]
        command "setComboModes"   , [[name:"Set Spa Combo Modes*", type:"ENUM", description:"Set Ready/Rest & Temperature Ranges of the Spa", constraints:COMBOMODES]]
        command "push"   , [[name:"Refresh Spa Panel", description:"Refresh Spa Panel"]]

        command "setSpaToLocalTime"
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
    input name: "spaPanelDisplay", type: "bool", defaultValue: 'false', title: fmtTitle("Create Spa Panel Table for Dashboard"), description: fmtDesc("An HTML Matrix Table for HE dashboard")
}

/* BWA API Map ***************************************************
    sendCommand parameters and example values
    SetTemp / 69
    TempUnits / C
    TempUnits / F
    TimeFormat / 12
    TimeFormat / 24
    SystemTime / 01:49
    Filters / <base64 string of bytes dictating the filter cycle>
    Request / <panel request, such as list filter cycles??>
*************************************************************** */

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
    setLogLevel("Debug", "30 Minutes")
    logInfo "Setting Inital logging level to 'Debug' for 30 minutes"
    logInfo "Setting HTML spaPanelDisplay Table to Off.  You can enable this preference setting in the ${PARENT_DEVICE_NAME} child device."
    device.updateSetting('spaPanelDisplay', [type: "bool", value: 'false'])
}

def updated() {
	logInfo "Preferences Updated..."
    logDebug "spaPanelDisplay Bool= ${spaPanelDisplay}"
    if (!spaPanelDisplay) {
        logInfo "HTML table attribute remoted from device"
        device.deleteCurrentState('spaPaneliFrame')
        device.deleteCurrentState('spaPanelHTML')
    }
	checkLogLevel()
}

def on() {
    logDebug "Switch On Detected:  Checking Spa States"
    if (device.currentValue("ReadyMode") == "Rest") {
        setReadyMode('Ready')
    } else {
        logErr "Spa already in Ready mode, command ignored"
    }
    if (device.currentValue("TempRange") == "low") {
        setTempRange('high')
    } else {
        logErr "Spa already in High Temp mode, command ignored"
    }
}

def off() {
    logDebug "Switch off Detected:  Checking Spa States"
    if (device.currentValue("ReadyMode") == "Ready") {
        setReadyMode('Rest')
    } else {
        logErr "Spa already in Rest mode, command ignored"
    }
    if (device.currentValue("TempRange") == "high") {
        setTempRange('low')
    } else {
        logErr "Spa already in low Temp mode, command ignored"
    }
}

void setSpaToLocalTime() {
    def timeData = nowFormatted("HH:mm")
    logInfo "setSpaToLocalTime(): SystemLocalTime = ${timeData}"
    spaCmdsQueue('add',"SystemTime",timeData)
}

def setComboModes(comboMode) {
    logDebug "setComboModes(${comboMode})"
    if(comboMode.contains('Ready')) setReadyMode('Ready') else setReadyMode('Rest')
    if(comboMode.contains('high'))  setTempRange('high')  else setTempRange('low')
}


def spaCmdsQueue(cmd, action=null, data=null) {
    logDebug "In parent spaCmdsQueue(${cmd}, ${action}, ${data})"
    switch (cmd) {
        case 'add':
        if (!state.spaQueue) state.spaQueue = []
        state.spaQueue << ["${action}": data]
        logDebug "state.spaQueue= ${state.spaQueue}"
        sendEvent(name: 'cloudStatus', value: "Builing spa command queue with item: #${state.spaQueue.size()}...")
        break
        case 'delete':
        state.remove('spaQueue')
        break
    }
    runIn(5,sendSpaQueueCommand, [overwrite: true])
}

void setReadyMode(text_mode) {
    logDebug "In parent setReadyMode..."
    def ReadyMode = device.currentValue("ReadyMode")
    logDebug "Device current mode is ${ReadyMode}"
    if (!text_mode) {
        text_mode = ReadyMode == "Ready" ? "Rest" : "Ready"
    }
    if (ReadyMode != text_mode) {
        sendEvent(name: "ReadyMode", value: text_mode)
        logDebug "Setting new ReadyMode to '${text_mode}'"
        spaCmdsQueue('add',"Button",BUTTON_MAP.HeatMode)
    } else {
        sendEvent(name: "cloudStatus", value: "Spa is already in '${ReadyMode}' mode, command ignored")
    }
}

def sendSpaQueueCommand() {
    logDebug "sendQueueCommand(): state.spaQueue=${state.spaQueue}"
    sendEvent(name: 'cloudStatus', value: "${state.spaQueue.size()} spa command${state.spaQueue.size()>1?'s':''} posted.. waiting for BWA cloud response")
    def response = parent.sendCommand(state.spaQueue)
    logInfo "parent.sendCommand(state.spaQueue): response=${response.success}"
    sendEvent(name: 'cloudStatus', value: "${response.success?"Commands Received: OK":"Commands Received: Error"}")
    // Reset the spaQueue
    state.spaQueue = []
    if (response.success==true) {
        // Refresh panel
        logInfo "Refreshing panel at ${new Date()}"
        refresh()
    } else {
        // Refresh panel
        logInfo "Refresh panel aborted, BWA server responded => response=${response}"
        response.properties.each {
            logDebug "sendSpaQueueCommand(http response=${response.status}): ${it}"
        }
    }

    //    logDebug "Getting Spa Panel Refresh in 5 secs"
    //    runIn(5, "refresh", [overwrite: true])
}

void setTempRange(temp_range) {
    logDebug "In parent setTempRange..."
    def TempRange = device.currentValue("TempRange")
    logDebug "Device current heating mode is ${TempRange}"
    if (!temp_range) {
        temp_range = TempRange == "low" ? "high" : "high"
    }
    if (TempRange != temp_range) {
        sendEvent(name: "TempRange", value: temp_range)
        logDebug "Setting new TempRange to '${temp_range}'"
        spaCmdsQueue('add',"Button",BUTTON_MAP.TempRange)
    } else {
        sendEvent(name: "cloudStatus", value: "Spa is already in '${TempRange}' mode, , command ignored")
    }
}

def sendCommand(action, data, howMany=1) {
    logDebug "sendCommand(${action}, ${data}, ${howMany})"
    for(int i = 0;i<howMany;i++) {
        spaCmdsQueue('add',"${action}",data)
    }
}

def parseDeviceData(Map results) {
    results.each {name, value ->
        logTrace ("name: ${name}, value ${value}")
        sendEvent(name: name, value: value, displayed: true)
        sendEvent(name: name, value: value)
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
            if (pumpNumber == 0) {
                // Circ pump is not user controllable but has on/off read only status
                fetchChild(true, "Switch", "Pump ${pumpNumber}", PUMP_BUTTON_MAP[pumpNumber])
            } else {
                // Pump with user controllable power levels
                fetchChild(true, "Pump", "Pump ${pumpNumber}", PUMP_BUTTON_MAP[pumpNumber])
            }
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
    if (spaConfiguration?.Blower) sendEvent(name:"blowerPump",value: "true" ) {
        device.deleteCurrentState("blowerPump")
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
        sendEvent(name: "cloudStatus", value: "Error: ${encodedData} at: ${now}")
        // Send events to Thermostat child device
        def thermostatChildDevice = fetchChild(false, "Thermostat", "Thermostat")
        if (thermostatChildDevice != null) {
            thermostatChildDevice.sendEvents([
                [name: "temperature", value: -1 ],
                [name: "thermostatMode", value: "heat"],
                [name: "thermostatOperatingState", value: "idle"]
            ])
        }
        return false
    }

    sendEvent(name: 'cloudStatus', value:"Panel state was successfully updated}")

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
        filterMode = "cycle 1"
        break;
        case 8:
        filterMode = "cycle 2"
        break;
        case 12:
        filterMode = "cycle 1 & 2"
        break;
        case 0:
        default:
            filterMode = "off"
    }
    sendEvent(name: "filterMode", value: filterMode)

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
    //Use acceleration - ENUM ["inactive", "active"]
    def pump0ChildDevice = fetchChild(false, "Switch", "Pump 0")
    if (pump0ChildDevice != null) {
        switch (decoded[17] & 1) { // Pump 0
            case 1:
            pumpState[0] = "active"
            break
            default:
                pumpState[0] = "inactive"
        }
        pump0ChildDevice.parse(pumpState[0])
    }

    def pump1ChildDevice = fetchChild(false, "Pump", "Pump 1")
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
    def pump2ChildDevice = fetchChild(false, "Pump", "Pump 2")
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
    def pump3ChildDevice = fetchChild(false, "Pump", "Pump 3")
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
    def pump4ChildDevice = fetchChild(false, "Pump", "Pump 4")
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
    def pump5ChildDevice = fetchChild(false, "Pump", "Pump 5")
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
    def pump6ChildDevice = fetchChild(false, "Pump", "Pump 6")
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

    // As far as I know, blower is not controllable, just report state in parent child device?
    if (spaConfiguration?.Blower)  {
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
        sendEvent(name: "blowerState", value: blowerState)
    }

    // Lights
    def lightState = []
    lightState[0] = null
    lightState[1] = null
    def light1ChildDevice = fetchChild(false, "Switch", "Light 1")
    if (light1ChildDevice != null) {
        lightState[1] = ((decoded[18] & 3) != 0)?'on':'off'
        light1ChildDevice.parse(lightState[1])
        logTrace "lightState[1]= ${lightState[1]}"
    }
    lightState[2] = null
    def light2ChildDevice = fetchChild(false, "Switch", "Light 2")
    if (light2ChildDevice != null) {
        lightState[2] = ((decoded[18] & 12) != 0)?'on':'off'
        light2ChildDevice.parse(lightState[2])
        logTrace "lightState[2]= ${lightState[2]}"
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
        pumpStateStatus = "off"
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

    if (spaPanelDisplay) {
        def spaItemsExtra = [
            "Pump State Status"          : "${pumpStateStatus}",
            "Is 24-Hour Time"            : "${is24HourTime}",
            "Temperature Scale"          : "°${temperatureScale}",
            "Accessibility Type"         : "${accessibilityType}",
            "wifiState"                  : "${wifiState}"
        ]
        logTrace "spaItemsExtra=${spaItemsExtra}"
        def spaItems = [
            "Temp"                       : "${actualTemperature}",
            "Spa Time"                   : "${currentSpaTimeHour}:${currentSpaTimeMinute}",
            "Temp - Target"              : "${targetTemperature}",
            "Filter Mode"                : "${filterMode}",
            "Heating Mode"               : "${heatingMode}",
            "Light 1"                    : "${lightState[1]}",
            "Light 2"                    : "${lightState[2]}",
            "Heat Mode"                  : "${heatMode}",
            "Is Heating"                 : "${isHeating}",
            "Pump 0 (Circ)"              : "${pumpState[0]}",
            "Pump 1"                     : "${pumpState[1]}",
            "Pump 2"                     : "${pumpState[2]}",
            "Pump 3"                     : "${pumpState[3]}",
            "Pump 4"                     : "${pumpState[4]}",
            "Pump 5"                     : "${pumpState[5]}",
            "Pump 6"                     : "${pumpState[6]}",
            "BlowerState"                : "${blowerState}",
            "MisterState"                : "${misterState}",
            "AuxState 1 "                : "${auxState[1]}",
            "AuxState 2 "                : "${auxState[2]}",
            "Updated"                    : "${nowFormatted('h:mm:ss a')}"
        ]
        logTrace "spaItemsExtra=${spaItems}"
        makeSpaPanelTable(spaItems)
    }
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
        cd = addChildDevice(NAMESPACE, driverName, childDeviceName, [name: "${device.displayName} {$name}", label: "Spa-${name}", isComponent: false])

        // Switches and Pumps will need to know their respective Balboa API Button IDs

        if (["Switch","Pump"].contains(type) && balboaApiButtonNumber > 0) {
            cd.setBalboaAPIButtonNumber(balboaApiButtonNumber)
        }
    }
    return cd
}

Map checkTypeName(deleteIfWrongTypeName=false, deviceNameSuffix,child) {
    def status
    def deviceTypeName = "HB BWA SPA ${deviceNameSuffix}"
    def deviceName = child.label?:child.displayName
    def color = 'red'
    if (child.typeName == deviceTypeName) {
        status = getImage('checkMarkGreen') + " This child device is ${VERSION} version compliant"
        color='green'
    } else {
        logErr  "ERROR: ${deviceName} should have a deviceTypeName = '${deviceTypeName}'."
        if (deleteIfWrongTypeName==true) {
            logInfo "Renaming ${deviceName}: '${child.deviceNetworkId}' to ${child.deviceNetworkId}-OLD"
            child.deviceNetworkId = "${child.deviceNetworkId}-OLD"
            child.displayName = "${child.displayName}-OLD"
            child.label = child.label?"${child.label}-OLD":"${child.displayName}-OLD"
        }
        status = "This child device is NOT ${VERSION} version compatibble and has been RENAMED with an '-OLD' suffix for dashbboard/RM/piston migration.<br>A NEW ${VERSION} spa '${deviceName}' device has been created and activated"
    }
    return [
        "${deviceName}":[
        'status'       : "<font color=${color}>${status}</font>",
        'typeName'     : child.typeName?:'Unknown'
        ]
    ]
}

def deleteOldChildDevices(okToDelete=false) {
    logDebug "deleteOldChildDevices(okToDelete=${okToDelete})"
    def suffix = "-OLD"
    if (okToDelete) {
        getChildDevices().findAll {it.deviceNetworkId.endsWith(suffix)}.each { child ->
            logInfo "Deleting old child device ${child.displayName}: DNI=${child.deviceNetworkId}"
            deleteChildDevice(child.deviceNetworkId)
        }
    }
}

def configure() {
    logDebug "configure()"
    def spaConfig = parent.returnVar("spaConfiguration").findAll{it.value}
    logTrace "spaConfig= ${spaConfig}"
    state.spaConfiguration = [:]
    state.spaConfiguration = spaConfig
    logTrace "state.spaConfiguration= ${state.spaConfiguration}"
    return spaConfig
}


Map verifyChildrenDevicesMethod(renameBadDevices=false) {
    logTrace "verifyChildrenPumpDevices()"
    spaConfiguration = state.spaConfiguration?:configure()
    logDebug "spaConfiguration= ${spaConfiguration}"
    def returnMap = [:]
    def children = getChildDevices()
    if (spaConfiguration && children) {
        def deviceName
        children.sort().each { child ->
            switch (child.deviceNetworkId) {
                case ~/^.*Pump 0$/:
                logTrace  "Circ Pump: ${child.deviceNetworkId}, ${child.displayName} - ${child.typeName}"
                returnMap << checkTypeName(renameBadDevices,"Switch",child)
                break
                case ~/^.*Pump [1-9]$/:
                logTrace  "Spa Pump ${child.deviceNetworkId[-1]}: ${child.deviceNetworkId}, ${child.displayName} - ${child.typeName}"
                returnMap << checkTypeName(renameBadDevices,"Pump",child)
                break
                case ~/^.*Thermostat$/:
                logTrace  "Thermostat: ${child.deviceNetworkId}, ${child.displayName} - ${child.typeName}"
                returnMap << checkTypeName(renameBadDevices,"Thermostat",child)
                break
                case ~/^.*Light.*$/:
                case ~/^.*Aux.*$/:
                case ~/^.*Mister.*$/:
                logTrace  "Light/Aux/Mister: ${child.deviceNetworkId}, ${child.displayName} - ${child.typeName}"
                returnMap << checkTypeName(renameBadDevices,"Switch",child)
                break
                case ~/^.*-OLD$/:
                deviceName = child.label?:child.displaName
                returnMap << ["${deviceName}":
                              [
                                  status  :"<font color=red>This is an OLD child device (pre ${VERSION}) and can to be deleted using button below.</font>",
                                  typeName:"<font color=red>${child.typeName}</font>"
                              ]
                             ]
                break
                default:
                    logWarn "Unknown: ${child.deviceNetworkId}, ${child.displayName} - ${child.typeName}"
                deviceName = child.label?:child.displaName
                returnMap << ["${deviceName}":
                              [
                                  status  :"<font color=red>This child device with DNI of '${child.deviceNetworkId}' is UNKNOWN.</font>",
                                  typeName:"<font color=red>${child.typeName}</font>"
                              ]
                             ]
                break
            }
        }
        logTrace "==> createChildDevices(${spaConfiguration})"
        createChildDevices(spaConfiguration)
    } else {
        logErr "Parent spa configuration map is null, re-reun device configuration in BWA Spa Manager app"
    }
    logDebug "returnMap= ${returnMap}"
    return returnMap
}

void makeSpaPanelTable(data) {
    logTrace "Creating Device panel HTML table"
    def spaItems = data.findAll{ it.value!='null' }

    // CSS Definition for Table Styling
    def tableCSS = "table.bt{border:2px solid #FFA500;text-align:center;border-collapse:collapse;width:100%}table.bt td,table.bt th{border:1px solid #000000;padding:2px 2px}table.bt thead{border-bottom:2px solid #000000}table.bt thead th{font-weight:bold;color:#FF0000;text-align:center}.g{background-color:green}.r{background-color:red}.b{background-color:blue}"

    //Add style tags
    tableCSS = "<style>${tableCSS}</style>"

    /* Old Style that corrupted device info page tables
    def tableCSS="table, td, th, tr{border: 2px solid orange} table{text-align:center;border-collapse: collapse;color:white;background-color:black} .g{background-color:green} .r{background-color:red} .b{background-color:blue}"
    def tableHeader ="<html><head><style>${tableCSS}</style></head><body><table border=3 cellpadding=2 align=center><tr>"
    */

    // Table Header
    def tableHeader = "<table class=bt><thead><tr><th>Spa Device</th><th>Value</th><th>Spa Device</th><th>Value<</tr></thead></tbody>"

    //Table Footer
    def tableFooter ="</tbody></tr></table>"

    // Table Rows
    def row = new String [15]
    Integer i = 0
    Integer totalSpaItemsPerColumn = spaItems.size()/2 - 1
    spaItems.sort().eachWithIndex { key, value, index ->
        switch(value) {
            case 'high':
            case 'medium':
            case 'low':
            case 'on':
            case 'active':
            case 'Ready':
            case 'true':
            value = "<td class=r>${value}</td>"
            break
            case 'off':
            case 'false':
            case 'inactive':
            value = "<td class=g>${value}</td>"
            break
            default:
                // Highlight high and low temperatures
                if (key.startsWith('Temp')) {
                    if (value.toInteger() >= 99) {
                        value = "<td class=r>${value}°${temperatureScale}</td>"
                    } else {
                        value = "<td class=b>${value}°${temperatureScale}</td>"
                    }
                } else value = "<td>${value}</td>"
            break
        }

        row[i++] += "<td>${key}</td>${value}"
        if (index==totalSpaItemsPerColumn) i = 0
    }
    def rows = ""
    row.findAll{it}.each  {
        rows += "<tr>${it.substring(4)}</tr>"
    }
    // Combine Table Sections
    def spaPanel = "<html>${tableCSS}${tableHeader}${rows}${tableFooter}</html>"
    if (spaPanel.length()>1024) {
        logWarn "the spaPanelHTML.length() is ${spaPanel.length()}, which will be truncated to 1024"
    }
    logTrace spaPanel
    sendEvent(name:'spaPanelHTML',value: spaPanel.take(1024))

/*
    // Write HTM to local file on Hub
    def fName = 'BWASpaManagerPanelTable.htm'
    def localserver = "http://${location.hub.localIP}/local/"
    def source = "${localserver}${fName}"
    if (fileExists(fName)) deleteFile(fName)
    writeFile(fName, spaPanel)
    def iFrameHTML = "<!DOCTYPE html><html><body><iframe src=${source} id='BWAiframe' style='border:0 float:right;margin:1px;' width=400 height=200></iframe></body></html>"

    logTrace "iFrameHTML= ${groovy.xml.XmlUtil.escapeXml(iFrameHTML)}"

    sendEvent(name:'spaPaneliFrame',value: iFrameHTML)
*/

}

def push() {
    logInfo "push(): Refreshing spa panel"
    refresh()
}


void refresh() {
    logInfo "BWA Cloud Panel Refresh Requested"
    sendEvent(name: 'cloudStatus', value: "BWA cloudAPI panel refresh/update requested at ${nowFormatted('h:mm:ss a')}")
    parent.pollChildren(override=true)
}