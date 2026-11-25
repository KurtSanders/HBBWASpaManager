// hubitat start
// hub: 10.0.0.77  <- this is hub's IP address
// type: device         <- valid values here are "app" and "device"
// id: 1161           <- this is app or driver's id
// hubitat end
/**
 * Hubitat Balboa Hot Tub Driver Integration by Kurt Sanders 2024,2025,2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include kurtsanders.SanderSoft-Library
#include kurtsanders.Balboa-Hot-Tub-API-Library

// Constants
@Field static final String  PARENT_DEVICE_NAME   		= "Balboa Hot Tub Local Driver"
@Field static final String  CHILD_DEVICE_NAME_SWITCH  	= "Balboa Hot Tub Local Child Switch"
@Field static final String  VERSION 					= "2.3.0"
@Field static final String  FILENAME_CSS 				= "BalboaLocalPanelTable.css"
@Field static final Integer READ_WAIT_SECS 				= 15
@Field static final Integer MAX_RESPONSE_MESSAGES		= 5
@Field static final Integer MAX_RAW_MESSAGES			= 10
@Field static final Integer SOCKET_CONNECT_READ_DELAY 	= 150

metadata {
definition(name: PARENT_DEVICE_NAME,
    	namespace: NAMESPACE,
       	author: AUTHOR_NAME,
       	importUrl: "",
       	singleThreaded: true) {
        capability "Actuator"
		capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Temperature Measurement"
        capability "Thermostat Heating Setpoint"
        capability "Thermostat Setpoint"
    	capability "Contact Sensor"
    	capability "Presence Sensor"
	    capability "Fan Control"

        attribute "Blower", "enum", ["off","low","medium","high"]
    	attribute 'Filter1Start', 'string'
    	attribute 'Filter1Duration', 'string'
    	attribute 'Filter2Enabled', 'string'
    	attribute 'Filter2Start', 'string'
    	attribute 'Filter2Duration', 'string'
        attribute "filterMode", "enum", ["off", "cycle 1", "cycle 2", "cycle 1 and 2"]
        attribute "heatMode", "enum", HEATMODES
        attribute "heatingSetpointFormatted", "string"
        attribute "heatingSetpoint", "number"
        attribute "is24HourTime", "enum", ["true","false"]
        attribute "isFilter2Active", "enum", ["true","false"]
    	attribute "macAddress", "string"
        attribute "network", "enum", ["online","offline"]
        attribute "Mister", "enum", ["on","off"]
        attribute "Light1", "enum", ["on","off"]
        attribute "Light2", "enum", ["on","off"]
        attribute "Lights", "enum", ["on","off"]
        attribute "pollingInterval", "string"
        attribute "Pump0", "string"
        attribute "Pump1", "enum", ["off","low", "high"]
        attribute "Pump2", "enum", ["off","low", "high"]
        attribute "Pump3", "enum", ["off","low", "high"]
        attribute "Pump4", "enum", ["off","low", "high"]
        attribute "Pump5", "enum", ["off","low", "high"]
        attribute "Pump6", "enum", ["off","low", "high"]
        attribute "Pumps", "enum", ["on","off"]
        attribute "spaPanelHTML", "string"
        attribute "spaStatus", "string"
        attribute "spaSessionStatus", "string"
        attribute "spaTime", "string"
        attribute "spaTimeLocalDeltaMins", "number"
        attribute "tempRange", "enum", TEMPRANGES
    	attribute "temperatureScale", "string"
    	attribute "temperatureFormatted", "string"
        attribute "TTSmessage", "string"
        attribute "updated_at", "string"
        attribute "wifiState", "enum", ["WiFi OK","WiFi Spa Not Communicating","WiFi Startup","WiFi Prime","WiFi Hold","WiFi Panel","WiFi Unknown"]

        attribute "supportedThermostatModes", 'JSON_OBJECT'
        attribute "thermostatOperatingState", "enum", THERMO_STAT_OPERATING_STATES
        attribute "thermostatMode", "enum", THERMO_STAT_MODES
        attribute "supportedThermostatFanModes", 'JSON_OBJECT'
        attribute "thermostatFanMode", "enum", THERMO_STAT_FAN_MODES
        attribute "speed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "setSpeed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "supportedFanSpeeds", 'JSON_OBJECT'
    
	     // ==> Start: Code provided by kenrok1
	    command "cyclePump", [[name:"Pump Number*", type: "NUMBER", description: "Enter 1, 2, 3, 4, 5, or 6"]]
		command "createChildDevices"
	    // ==> End: Code provided by kenrok1

	    command "createAllChildrenSpaSwitchs"
        command "disconnect"
        command "getFilterSchedule"
        command "setHeatingSetpoint", [[name:'Heating Setpoint 55-104°F | 14-40°C*'	, 
                                        type:'NUMBER', description:'Heating setpoint temperature from 55°F-104°F| 14-40°C', range: "14..104"]]
    	command "setLights"			, [[name:"Set Spa Lights On/Off*"				, 
                                        type:"ENUM", description:"Set Spa Lights (On/Off)", constraints:LIGHTSMODES]]
        command "setHeatMode"    	, [[name:"Set Heat Mode*"					, 
                                        type:"ENUM", description:"Set Heat Mode (Ready/Rest)", constraints:HEATMODES]]
        command "setSpaToLocalTime"
        command "setSpeed"			, [[name:"Pump speed*"							, 
                                        type:"ENUM", description:"Pump speed to set", constraints:SUPPORTED_PUMP_SPEED_MODES]]
        command "setTempRange"   	, [[name:"Set Temp Range*"						, 
                                        type:"ENUM", description:"Set Temperature Range of the Spa", constraints:TEMPRANGES]]
        command "setTempScale"		, [[name:"Set Temp Scale*"						, 
                                        type:"ENUM", description:"Set Temperature Scale °C|°F of the Spa", constraints:TEMPSCALES]]
	}
}

preferences {
    section ("Required Inputs") {
        input "ipaddress", "text", title: "Device IP:", required: true,
            description: "Balboa device local IP address. Tip: Configure a fixed IP address for your spa on the same network as your Hubitat hub to make sure the spa's IP address does not change."

        input "port", "text", title: "Device IP Port:", required: true, defaultValue: '4257', description: "Balboa device IP Port address (Default 4257). Please insure that your EW11 RS485 is configured to this same port!"

        input name: "poll_interval", 		type: "enum", title: "Configure device auto polling interval", defaultValue: 'Off', options: POLLING_OPTIONS,
            description: "Auto poll the panel status of the deivce. Select 'Off' to stop automatic polling.", required: true
        input name: "spaPanelDisplay", 		type: "bool", defaultValue: 'false', title: fmtTitle("Create Spa Panel Table for Dashboard"),
            description: fmtDesc("An HTML Matrix Table for HE dashboard. Remember to Save!"), required: true
        input name: "spaPanelTableRemovePump0", type: "bool", title: "Remove 'Pump0' Data Element in HTML Table", defaultValue: false,
            description: fmtDesc("Remove 'Pump0' Data Element in HTML Table. Remember to Save!"), required: true
        input name: "createMomentarySwitches", type: "bool", defaultValue: true, title: fmtTitle("Create Momentary Control Child Switches"),
            description: fmtDesc("Create/maintain momentary child switches for Dashboard/Rules"), required: false
        input name: "autoSyncClock", type: "bool", defaultValue: false, title: fmtTitle("Auto-Sync Spa Clock to Hub Time"),
            description: fmtDesc("Automatically update spa clock if drift exceeds threshold"), required: false
        input name: "clockDriftThresholdMins", type: "number", defaultValue: 2, title: fmtTitle("Clock Drift Threshold (Minutes)"),
            description: fmtDesc("Minimum minute difference before auto-syncing clock"), required: false
        input name: "presetTempHigh", type: "number", defaultValue: 102, range: "10..104", title: fmtTitle("Preset High Temperature"),
            description: fmtDesc("Target spa temperature when the Preset Temp child switch is turned ON"), required: false
        input name: "presetTempLow", type: "number", defaultValue: 80, range: "10..104", title: fmtTitle("Preset Low Temperature"),
            description: fmtDesc("Target spa temperature when the Preset Temp child switch is turned OFF"), required: false
    }
    section ("Advanced Driver Setttings") {
        input name: "readWaitSecs", 	type: "number", defaultValue: READ_WAIT_SECS, title: fmtTitle("Advanced: Read Wait Time, secs"),
            description: fmtDesc("Time to wait between successive reads"), required: true
        input name: "maxResponsesMessages", 	type: "number", defaultValue: MAX_RESPONSE_MESSAGES, title: fmtTitle("Advanced: Maxium Response Messages to Process"),
            description: fmtDesc("Maximum device responses to process"), required: true
        input name: "maxRawMessages", 	type: "number", defaultValue: MAX_RAW_MESSAGES, title: fmtTitle("Advanced: Maximum Raw Messages"),
            description: fmtDesc("Maximum raw messsages to process from the device"), required: true
        input name: "socketConnectReadDelay", 	type: "number", defaultValue: SOCKET_CONNECT_READ_DELAY, title: fmtTitle("Advanced: Socket Connect Read Delay, MilliSecs"),
            description: fmtDesc("Socket Connect Read Delay, MilliSecs"), required: true
    }

    section ("Logging") { 
        //	Logging Levels & Help
        input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
            description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
        input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
            description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
        //  Display Help Link
        input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
    }
}

void installed() {
    setLogLevel("Debug", "30 Minutes")
	logInfo "Setting Inital logging level to 'Debug' for 30 minutes"
    device.updateSetting('poll_interval', [type: "enum", value: 'off'])
	logInfo "Setting HTML Pump0 HTML Table to False"
    device.updateSetting('spaPanelTableRemovePump0', [type: "bool", value: false])
    logInfo "Setting Spa Thermo defaults..."
    setsupportedFanSpeeds()
    updateSettings()
    makeEvent("switch", "off")
    initialize()
    if (settings.createMomentarySwitches != false) createAllChildrenSpaSwitchs()
}

void updateSettings() {
    if (readWaitSecs == null) 			device.updateSetting('readWaitSecs', [type: "number", value: READ_WAIT_SECS])
    if (maxResponsesMessages == null) 	device.updateSetting('maxResponsesMessages', [type: "number", value: MAX_RESPONSE_MESSAGES])
    if (maxRawMessages == null) 		device.updateSetting('maxRawMessages', [type: "number", value: MAX_RAW_MESSAGES])
    if (socketConnectReadDelay == null) device.updateSetting('socketConnectReadDelay', [type: "number", value: SOCKET_CONNECT_READ_DELAY])
    if (tempscale == null) 				device.updateSetting('tempscale', [type: "enum", value: TEMPSCALES])
    if (state.temperatureScale == null) state.temperatureScale = "°F"
    logInfo "readWaitSecs = ${readWaitSecs} seconds"
    logInfo "maxResponsesMessages = ${maxResponsesMessages}"
    logInfo "maxRawMessages = ${maxRawMessages}"
    logInfo "socketConnectReadDelay = ${socketConnectReadDelay} milliSeconds"
}

void initialize() {
    // Reset thermostat attribute states
    String stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_MODES).toString()
    sendEvent(name: "supportedThermostatModes", value: stmJSON, displayed: false, isStateChange: true)
    stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_FAN_MODES).toString()
    sendEvent(name: "supportedThermostatFanModes", value: stmJSON, displayed: false, isStateChange: true)
    makeEvent("thermostatFanMode", "auto")
    makeEvent("thermostatMode", "heat")
    updateSettings()
}

void updated() {
	logDebug "Running Updated()... Checking for valid ip address"
    if (ipaddress==null || ipaddress.isEmpty()) return
	logDebug "Preferences Updated..."

    checkLogLevel()  // Set Logging Objects
    initialize() // Reset thermostat attribute states

    // Remove legacy state.pumpConfiguration, copy to state.spaConfiguration
    if (state.pumpConfiguration) {
    	state.spaConfiguration = state.pumpConfiguration
    	state.remove('pumpConfiguration')
    }

    // Verify that we have a valid spa congiguration
    if (!isSpaConfigOK()) {
        logWarn "Warning: Auto-generating spa configuration..."
        configure()
    }
    // Show the Spa Configuration
    if (state.spaConfiguration) {
	    state.spaConfiguration.sort().each { key, value ->
    	    logDebug "state.spaConfiguration: ${key}: = ${value}"
    	}
    } else {
        logErr "state.spaConfiguration is missing data, running Configure()"
        runIn(5, 'configure')
    }

    // Configure device poll timer interval
    if (poll_interval) {
        switch(poll_interval) {
            case 'off':
                unschedule(refresh)
                logDebug "Auto polling is stopped, use manual command 'refresh' button to update device panel status"
		        makeEvent('pollingInterval',poll_interval)
                break
            default:
                // Verify valid interval in case we are invoked outside this method
                if(poll_interval.findAll(/^runEvery((5|10|15|30)Minutes|1Minute|1Hour|3Hours)$/)) {
                	logDebug "Setting Auto polling to '${POLLING_OPTIONS[poll_interval]}'"
			        makeEvent('pollingInterval',poll_interval)
            		this."${poll_interval}"('refresh')
                } else {
                    logErr "Invalid poll interval ${poll_interval} requested. Valid List = ${POLLING_OPTIONS.keySet()}.  Command Ignored."
            	}
            	break
        }
    }
    if (settings.createMomentarySwitches != false) createAllChildrenSpaSwitchs()
}

def createAllChildrenSpaSwitchs() {
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Refresh Local Spa"      , [functionName: 'refresh'     , parameter: "null"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Heat Mode to Ready" , [functionName: 'setHeatMode' , parameter: "Ready"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Heat Mode to Rest"  , [functionName: 'setHeatMode' , parameter: "Rest"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Temp Range to High" , [functionName: 'setTempRange', parameter: "High"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Temp Range to Low"  , [functionName: 'setTempRange', parameter: "Low"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Spa Pumps On"       , [functionName: 'setPumps'    , parameter: "on"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Spa Pumps Off"      , [functionName: 'setPumps'    , parameter: "off"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Spa Lights On"      , [functionName: 'setLights'   , parameter: "on"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Spa Lights Off"     , [functionName: 'setLights'   , parameter: "off"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Spa Preset Temp (High/Low)", [functionName: 'setPresetTemp', parameter: "high", offFunctionName: 'setPresetTemp', offParameter: "low", stateful: true])
}

def createChildren(createIfDoesntExist, String type, String name, Map apiMap = null) {
    String thisId = device.id
    def childDeviceName = "${thisId}-${name}"
    logTrace "childDeviceName: '${childDeviceName}'"

    def cd = getChildDevice(childDeviceName)
    if (!cd && createIfDoesntExist) {
        logInfo "Adding Child Device Driver: ${type}, Name: '${childDeviceName}' with apiMap: ${apiMap}"
        cd = addChildDevice(NAMESPACE, type, childDeviceName, [name: "${device.displayName} - ${name}", label: "${name}", isComponent: false])
    }
    if (apiMap && cd) {
        cd.setApiMap(apiMap)
    }
    return cd
}

// ==> Start: Code provided by kenrok1
// ============================================================
//   SPA CHILD MANAGEMENT & ALEXA BRIDGE (INTEGRATED)
// ============================================================

def setPumps(String status) {
    cycleSpeed()
}

// --- STANDALONE CHILD DISCOVERY ---
def createChildDevices() {
    log.info "Analyzing spaConfiguration for device discovery..."
    def config = state.spaConfiguration
    if (config instanceof String) { config = parseJson(config) }

    if (!config) {
        log.warn "No spaConfiguration found! Click Refresh, wait 5s, then click 'Create Child Devices'."
        return
    }

    (1..6).each { i ->
        def pumpInfo = config["Pump${i}"]
        if (pumpInfo?.installed == true) {
            String pId = "${device.deviceNetworkId}-Pump${i}"
            if (!getChildDevice(pId)) {
                addChildDevice("hubitat", "Generic Component Switch", pId, [name: "${device.displayName} Pump ${i}", isComponent: false])
                log.info "Created: Pump ${i}"
            }
            syncComponentChild("Pump${i}", (device.currentValue("Pump${i}") ?: "off").toLowerCase() != "off")
        }
    }

    (1..2).each { i ->
        def lightInfo = config["Light${i}"]
        if (lightInfo?.installed == true) {
            String lId = "${device.deviceNetworkId}-Light${i}"
            if (!getChildDevice(lId)) {
                addChildDevice("hubitat", "Generic Component Switch", lId, [name: "${device.displayName} Light ${i}", isComponent: false])
                log.info "Created: Light ${i}"
            }
            syncComponentChild("Light${i}", (device.currentValue("Light${i}") ?: "off").toLowerCase() == "on")
        }
    }
}

void syncComponentChild(String suffix, boolean isOn) {
    def cd = getChildDevice("${device.deviceNetworkId}-${suffix}")
    if (!cd) return
    String target = isOn ? "on" : "off"
    if (cd.currentValue("switch") != target) {
        cd.sendEvent(name: "switch", value: target)
    }
}

// --- CHILD EVENT HANDLERS (ALEXA COMPATIBILITY) ---
void componentOn(cd)  { processChildCommand(cd, "on") }
void componentOff(cd) { processChildCommand(cd, "off") }

void componentRefresh(cd) {
    refresh()
}

private processChildCommand(cd, String desiredState = "on") {
    String dni = cd.deviceNetworkId
    cd.sendEvent(name: "switch", value: desiredState, isStateChange: true)

    if (dni.contains("-Pump")) {
        int num = dni.split("-Pump")[-1] as Integer
        String curState = (device.currentValue("Pump${num}") ?: "off").toLowerCase()
        if (desiredState == "on" && curState == "off") {
            cyclePump(num)
        } else if (desiredState == "off" && curState != "off") {
            int speeds = (state.spaConfiguration?."Pump${num}"?.speeds ?: '2') as Integer
            int presses = (speeds == 2 && curState == "low") ? 2 : 1
            presses.times { idx ->
                cyclePump(num)
                if (idx + 1 < presses) pauseExecution(500)
            }
        }
        runIn(3, "refresh", [overwrite: true])
    } else if (dni.contains("-Light")) {
        int num = dni.split("-Light")[-1] as Integer
        String curState = (device.currentValue("Light${num}") ?: "off").toLowerCase()
        if (curState != desiredState) {
            String lightCmd = "SET_LIGHT${num}"
            if (this."${lightCmd}") { send(this."${lightCmd}") }
            runIn(3, "refresh", [overwrite: true])
        }
    }
}

// Helper to cycle pumps by number
def cyclePump(pumpNumber) {
    def target = (pumpNumber instanceof Map) ? pumpNumber.data : pumpNumber
    if (!(target?.toString()?.isInteger())) return
    String pumpCmd = "SET_PUMP${target}"
    if (this."${pumpCmd}") {
        send(this."${pumpCmd}")
    }
}

// Kept for backward compatibility with any pending scheduled jobs
void resetChildSwitch(dni) {}

// ============================================================
// ==> End: Code provided by kenrok1

void childRunApi(device, apiMap) {
    logInfo "Dynamic Function Call: '${apiMap}' received from ${device} child device."
    if (apiMap.parameter) {
        this.&(apiMap.functionName)(apiMap.parameter)
    } else {
        this.&(apiMap.functionName)()
    }
}

void setsupportedFanSpeeds() {
    def modes = new groovy.json.JsonBuilder(SUPPORTED_PUMP_SPEED_MODES)
    sendEvent(name: "supportedFanSpeeds", value: modes, type:"digital", isStateChange: true, descriptionText:"Supported pump speeds initialized to ${SUPPORTED_PUMP_SPEED_MODES}")
}

void refresh(arg=null) {
    logTrace "refresh()"
    if (!isSpaConfigOK()) {
        logWarn "Warning: Auto-generating spa configuration..."
        configure()
    } else {
        state.expectConfig = false
        send(NOTHING_TO_SEND)
    }
}

void configure() {
    logDebug "configure(): Get Device Configuration"
    state.expectConfig = true
    send(GET_DEVICES)
}

void getFilterSchedule() {
    logDebug "getFilterSchedule(): Get Filter Cycle Configuration"
    state.expectConfig = true
    send(GET_FILTER)
}

boolean isSpaConfigOK(autoConfigure = false) {
	def exists = (state.spaConfiguration && state.spaConfiguration != null)
    if (!exists) {
        logErr "Spa Configuration Error: state.spaConfiguration is null"
        if (autoConfigure) configure()
    }    
    return exists
}

def setTempRange(mode=null) {
    logDebug "setTempRange(${mode})..."
    mode = mode?.toLowerCase()

    if (!['low','high'].contains(mode)) {
        logErr "Invalid Temp Range '${mode}'.. Exiting"
        return false
    }

    if (!isSpaConfigOK()) configure()
    else refresh() // Get Latest Spa Accessory States

    String tempRangeCurrent = device.currentValue("tempRange")?.toLowerCase()
    logDebug "Device current heating mode is ${tempRangeCurrent?.capitalize()}"
    if (tempRangeCurrent != mode) {
        logDebug "Setting New TempRange to '${mode.capitalize()}'"
        send(SET_TEMP_RANGE)
    } else {
        logErr "Spa is already in '${mode.capitalize()}' mode, command ignored"
    }
}

def setThermostatMode(thermostatmode) {
    makeEvent("thermostatMode", thermostatmode)
}

def setThermostatFanMode(fanmode) {
    makeEvent("thermostatFanMode", fanmode)
}

def setHeatMode(mode=null) {
    logDebug "setHeatMode(${mode})"
    mode = mode?.toLowerCase()

    if (!['ready','rest'].contains(mode)) {
        logErr "Invalid Heat Mode '${mode}'.. Exiting"
        return false
    }

    if (!isSpaConfigOK()) configure()
    else refresh() // Get Latest Spa Accessory States

    String currentHeatMode = device.currentValue("heatMode")?.toLowerCase()
    logDebug "Device current mode is ${currentHeatMode}"

    if (currentHeatMode != mode) {
        send(SET_HEAT_MODE)
        logDebug "Setting new heatMode to '${mode}'"
    } else {
        logErr "Error: Spa is already in '${currentHeatMode}' mode, command ignored"
    }
}

def setLights(mode=null) {
    logDebug "setLights(${mode})"

    mode = mode?.toLowerCase()
    if (!['on','off'].contains(mode)) {
        logErr "Invalid Light Mode '${mode}'.. Exiting"
        return false
    }

    if (!isSpaConfigOK()) {
        configure()
        return false
    }
    refresh() // Get Latest Spa Accessory States
    pauseExecution(1000)

    state.spaConfiguration?.findAll { it.key.startsWith('Light') }?.each { outerKey, outerValue ->
        if (outerValue?.installed) {
            logInfo "Checking spa state of '${outerKey}'"
            if (device.currentValue(outerKey) != mode) {
                logInfo "Setting '${outerKey}' to ${mode}"
                logDebug "Toggle ${outerKey}: Send " + this."SET_${outerKey.toUpperCase()}"
                send(this."SET_${outerKey.toUpperCase()}")
                pauseExecution(500)
            } else {
                logInfo "Set Lights ${mode}: Skipping ${outerKey}, already '${device.currentValue(outerKey)}'"
            }
        }
    }
}

void on() {
    logDebug "Switch: on()"
    if (!isSpaConfigOK()) {
        configure()
        return
    }
    state.spaConfiguration?.findAll { it.key =~ /Pump[123456]/ }?.each { outerKey, outerValue ->
        if (outerValue?.installed) {
            logDebug "Toggle ${outerKey}: Send " + this."SET_${outerKey.toUpperCase()}"
            send(this."SET_${outerKey.toUpperCase()}")
            pauseExecution(1000)
        }
    }
    setLights('on')
}

void off() {
    logDebug "Switch: off()"
    logDebug "Set Soak Mode"
    send(SET_SOAK_MODE)
    setLights('off')
}

def updateThermostatSetpoints(setpoint) {
    logInfo "==>updateThermostatSetpoints(${setpoint})"
    String tempRange = (device.currentValue("tempRange") ?: "high").toLowerCase()
    String scale = state.temperatureScale ?: "°F"
    boolean validTempBool = false

    switch (scale) {
        case '°F':
            def rangeF = TEMPERATURE_VALID_RANGES_F[tempRange]
            if (rangeF && setpoint >= rangeF[0] && setpoint <= rangeF[1]) validTempBool = true
            break
        case '°C':
            def rangeC = TEMPERATURE_VALID_RANGES_C[tempRange]
            if (rangeC && setpoint >= rangeC[0] && setpoint <= rangeC[1]) validTempBool = true
            break
        default:
            break
    }
    if (validTempBool) {
        logDebug "setTemperature: Valid setpoint temperature ${setpoint}${scale}"
        def encodedTemp = (scale == '°C') ? (setpoint.toBigDecimal() * 2).toInteger() : setpoint.toInteger()
        makeRequest("setTemperature", encodedTemp, true)
    } else {
        logErr "setTemperature: Invalid setpoint temperature: ${setpoint}${scale} for '${tempRange}' range"
    }
}

void setHeatingSetpoint(setpoint) {
    logTrace "==> setHeatingSetpoint(), waiting 5 secs for more input to updateThermostatSetpoint to ${setpoint}..."
    runIn(5, "updateThermostatSetpoints", [overwrite: true, data: setpoint])
}

void setPresetTemp(String mode) {
    def targetTemp = (mode?.toLowerCase() == 'high') ? (settings.presetTempHigh ?: 102) : (settings.presetTempLow ?: 80)
    logInfo "setPresetTemp(${mode}): Setting spa setpoint to ${targetTemp}${state.temperatureScale ?: '°F'}"
    updateThermostatSetpoints(targetTemp)
}

void setTempScale(tempscale) {
    logInfo "Changing Spa Temperature Scale to ${tempscale}"
    if (tempscale == '°C') {
        send(SET_HEAT_SCALE_C)
    } else {
        send(SET_HEAT_SCALE_F)
    }
}

def makeRequest(command, options, ok2send=false) {
    logDebug "==> makeRequest(${command}, ${options})"
    String messageRequest
    boolean rc = true
    switch(command) {
        case 'setTemperature':
            messageRequest = "06${CHANNEL}20${hubitat.helper.HexUtils.integerToHexString(options.toInteger(), 1)}"
            logDebug "==> messageRequest= ${messageRequest}"
            byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(messageRequest)
            def CRCByte = calculateChecksum(decodedByte)
            messageRequest = "${MESSAGE_DELIMITER}${messageRequest}${CRCByte}${MESSAGE_DELIMITER}"
            break
        case 'setTime':
            int HH = options.split(':')[0] as Integer
            int MM = options.split(':')[1] as Integer
            messageRequest = "07${CHANNEL}21${hubitat.helper.HexUtils.integerToHexString(HH, 1)}${hubitat.helper.HexUtils.integerToHexString(MM, 1)}"
            logTrace "==> messageRequest= ${messageRequest}"
            byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(messageRequest)
            def CRCByte = calculateChecksum(decodedByte)
            messageRequest = "${MESSAGE_DELIMITER}${messageRequest}${CRCByte}${MESSAGE_DELIMITER}"
            break
        default:
            logErr "makeRequest(${command}) command is Invalid. Ignoring"
            return false
    }
    logDebug "==> '${command}' messageRequest= ${messageRequest}"
    if (ok2send) {
        state.expectConfig = true
        rc = send(messageRequest)
        runIn(5, "refresh", [overwrite: true])
    } else {
        return messageRequest
    }
}

void removeDeviceSetting(devname) {
    device.removeSetting(devname)
}

void generateToggleHexStrings() {
    TOGGLE_ITEM_HEX_CODE.each { key, hexCode ->
        String messageRequest = "07${CHANNEL}11${hexCode}00"
        byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(messageRequest)
        def CRCByte = calculateChecksum(decodedByte)
        messageRequest = "${MESSAGE_DELIMITER}${messageRequest}${CRCByte}${MESSAGE_DELIMITER}"
        logDebug "\n@Field static final String GET_${key.toUpperCase()} = '${messageRequest}'"
    }
}

void cycleSpeed() {
    on()
}

void setSpeed(speedName) {
    logTrace "setSpeed(${speedName})"
    if (device.currentValue("presence") != "present") {
        logErr "Spa is 'offline', Ignoring setSpeed request for '${speedName}'. Try a COMMAND refresh to check online status"
        return
    }
    switch (speedName?.toLowerCase()) {
        case 'off':
            speedName = "off"
            break
        case "low":
        case ~/^medium.*/:
            speedName = "low"
            break
        case "high":
        case "auto":
            speedName = "high"
            break
        default:
            logWarn "Invalid speedName '${speedName}' value passed to setSpeed(). Ignoring..."
            return
    }
    String currentPump1 = (device.currentValue("Pump1") ?: "off").toLowerCase()
    if (currentPump1 == speedName) {
        logWarn "Pump1 is already at '${speedName}'. Ignoring redundant setSpeed request."
        return
    }
    int speeds = (state.spaConfiguration?.Pump1?.speeds ?: '2') as Integer
    List<String> cycleOrder = (speeds == 1) ? ['off', 'high'] : ['off', 'low', 'high']
    if (!cycleOrder.contains(speedName)) speedName = 'high'
    int currentIdx = Math.max(0, cycleOrder.indexOf(currentPump1))
    int targetIdx = cycleOrder.indexOf(speedName)
    int presses = (targetIdx - currentIdx + cycleOrder.size()) % cycleOrder.size()
    logInfo "Cycling Pump1 from '${currentPump1}' to '${speedName}' (${presses} press(es))"
    presses.times { idx ->
        send(SET_PUMP1)
        if (idx + 1 < presses) pauseExecution(500)
    }
    makeEvent("speed", speedName)
    makeEvent("setSpeed", speedName)
}

// Process the raw hexstring message packets from the device
def parse(String message) {
    state.lastMessage = (state?.lastMessage)?:[:]
    message = message.toUpperCase()
    logDebug "parse() message = ${message}"
    String hexData
    byte[] decodedByte
    def cs
    String messageType
    String CRCByte    
    
    // ==> Start: Code provided by kenrok1
    // Check if the message is already correctly aligned starting with 7E 
    if (message.startsWith(MESSAGE_DELIMITER)) {
        logTrace "Message is already aligned starting at ${MESSAGE_DELIMITER}"
    } else if (message.contains(MESSAGE_DELIMITER)) {
        // Only try to re-align if 7E exists somewhere else in the string
        logTrace "Misaligned Raw Message: Re-aligning to ${MESSAGE_DELIMITER}"
        try {
            message = message.substring(message.indexOf(MESSAGE_DELIMITER))
            logTrace "==> Here is the new aligned message= ${message}"
        } catch (e) {
            log.warn "Could not re-align message: ${message}. Error: ${e}"
            return
        }
    } else {
        log.warn "Invalid message received (no delimiter found): ${message}"
        return
    }
    // ==> End: Code provided by kenrok1

	// Outdated code replaced by above
    // Check to make sure that the first byte starts with MESSAGE_DELIMITER
    // Check for invalid 7E7E message packets and discard them
    /* 
    if (message ==~ /^${MESSAGE_DELIMITER}/) {
        logTrace "This raw message is perfectly aligned starting at ${MESSAGE_DELIMITER}"
    } else {
        logTrace "Misaligned Raw Message: Re-aligning to ${MESSAGE_DELIMITER}"
		message = (message =~ /^.*?${MESSAGE_DELIMITER}(${MESSAGE_DELIMITER}.*)/)[0][1]
        logTrace "==> Here is the new aligned message= ${message}"
    }
	//	List<String> hexDataList = message.findAll(/${MESSAGE_DELIMITER}.*?${MESSAGE_DELIMITER}/).unique { a, b -> a <=> b } as String[]
	*/

	def regex_pattern = /(?!${MESSAGE_DELIMITER}${MESSAGE_DELIMITER})${MESSAGE_DELIMITER}(.{6}(?:13|2E|23|24).*?)${MESSAGE_DELIMITER}/    
    List<String> hexDataList = message.findAll(regex_pattern).unique { a, b -> a <=> b } as String[]
    logDebug "==> Found ${hexDataList.size()} important status responses"

    for (int index = 0; index < hexDataList.size(); index++) {
        String it = hexDataList[index]
        logDebug "Regex ${index+1}= '${it}'"
        // Track Raw Message Count
        state.rawMessageCount = (state?.rawMessageCount)?state.rawMessageCount + 1:1
        // Runaway Raw Messages Loop Max Check
        if (state.rawMessageCount > maxRawMessages) {
            logWarn "Runaway Message Received Loop Check Exceded ${maxRawMessages} messages, exiting!"
            disconnect()
            break
        }
        // Discard bad length packets
        if (it.length() < 5) {
            logWarn "Short response Message#: state.rawMessageCount '${it}' skipped"
            continue
        }

        String messagePacket
        def msgMeta
        try {
            CRCByte     = it[-4..-3]
            messageType = it[8..9]
            msgMeta     = BALBOA_MESSAGE_TYPES[messageType]
            if (!msgMeta) {
                logDebug "Message#${state.rawMessageCount} Unknown type '${messageType}': '${it}'"
                continue
            }
            logDebug "==> Raw message #${state.rawMessageCount}: ${msgMeta.name}"

            // Filter/Skip messages that we do not process
            if (msgMeta.program) {
                int maxResp = (maxResponsesMessages ?: MAX_RESPONSE_MESSAGES) as Integer
                if ((state.processedMessageCount ?: 0) >= maxResp) {
                    logDebug "Auto closing device socket after ${state.processedMessageCount} response messages."
                    disconnect()
                    break
                }
                logDebug "Message#${state.rawMessageCount} Important:   ${msgMeta.name}"
                state.processedMessageCount = (state.processedMessageCount ?: 0) + 1
            } else {
                logDebug "Message#${state.rawMessageCount} Ignored: ${msgMeta.name} '${it}'"
                continue
            }

            if (state.lastMessage[messageType] == CRCByte) {
                logDebug "<span style=color:${msgMeta.color};>Message #:${state.rawMessageCount} CRCByte ${CRCByte} is redundant with previous ${messageType} message and will be ignored!</span>"
                continue
            }
            // Validate message integrity using CRC-8
            hexData = it[2..-5]
            decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
            cs = calculateChecksum(decodedByte)
            if (CRCByte == cs) {
                logTrace "Checksum Byte Verification Passed for Message #${state.rawMessageCount} '${it}': ${CRCByte} <> ${cs}"
            } else {
                logTrace "Checksum Byte Verification Failed for Message #${state.rawMessageCount} '${it}': ${CRCByte} <> ${cs}. Message #${state.rawMessageCount} Ignored."
                continue
            }
            logTrace "Unprocessed hexData for message ${state.rawMessageCount}: ${it}"
            messagePacket = it[10..-5]
            logTrace "==> messagePacket for message ${state.rawMessageCount}: ${messagePacket}"
        } catch (java.lang.StringIndexOutOfBoundsException ex) {
            logErr "Error processing message packet: '${it}'"
            continue
        } catch (e) {
            logErr "Error ${e} processing message packet: '${it}'"
            continue
        }

        // What type of messageType have we received
        if (BALBOA_MESSAGE_TYPES[messageType]) {
            makeEvent('spaSessionStatus', "<span style=color:yellow;>Received a ${BALBOA_MESSAGE_TYPES[messageType].name} response from spa</span>")
            logDebug "<span style=color:${BALBOA_MESSAGE_TYPES[messageType].color};>${BALBOA_MESSAGE_TYPES[messageType].name} Update: ${messagePacket}</span>"
        } else {
            logDebug "<span style=color:red;>Message type#: ${messageType} unknown: ${messagePacket}</span>"
            continue
        }
        // Let's count, record and process the message if we have a registered function
        if (BALBOA_MESSAGE_TYPES[messageType].program) {
            // Record this CRCByte with the messageType in a state map
            state.lastMessage[messageType] = "${CRCByte}"
            logDebug "Calling ${BALBOA_MESSAGE_TYPES[messageType].name} Message Handler: ${BALBOA_MESSAGE_TYPES[messageType].program}"
            // Dynamically call the correct parsing program for the messagePacket hex number
            def messageHandler = this.&(BALBOA_MESSAGE_TYPES[messageType].program)
            messageHandler(messagePacket)
            if ((!state.expectConfig && messageType == '13') || (state.expectConfig && messageType == '23')) {
                logDebug "Closing socket after receiving ${BALBOA_MESSAGE_TYPES[messageType].name}."
                disconnect()
                break
            }
        } else {
            logDebug "No Message Handler is defined for ${BALBOA_MESSAGE_TYPES[messageType].name} for message ${state.processedMessageCount}: ${messagePacket}"
        }
    }
}

void setSpaToLocalTime() {
    def timeData = nowFormatted("HH:mm")
    logDebug "setSpaToLocalTime(): Update Device System Local Time to ${timeData}"
    boolean rc = makeRequest('setTime',timeData, true)
    logTrace "==> setSpaToLocalTime() rc= ${rc}"
}

// Filter Cycle Response
def parseFilterResponse(hexData) {
    logDebug "parseFilterResponse: '${hexData}'"
    List hexParsed = hexData.split("(?<=\\G.{2})")
    logTrace "==> hexParsed= ${hexParsed}"
    byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    def decoded = decodedByte.collect { (it & 0xFF).toString().padLeft(2, '0') }
    decoded.eachWithIndex { hexnum, idx ->
        logDebug "${idx}: ${hexnum}"
    }
    boolean is24HourTime = (device.currentValue("is24HourTime") ?: "false").toBoolean()
    logTrace "==> is24HourTime= ${is24HourTime}"
    String timeFmt = is24HourTime ? "HH:mm" : "h:mm a"
    String Filter1Start = Date.parse("HHmm", "${decoded[0]}${decoded[1]}").format(timeFmt)
    logTrace "==> Filter1Start= ${Filter1Start}"
    makeEvent('Filter1Start', Filter1Start)
    String Filter1Duration = Date.parse("HHmm", "${decoded[2]}${decoded[3]}").format("H:mm")
    makeEvent('Filter1Duration', Filter1Duration)
    logTrace "==> Filter1Duration= ${Filter1Duration}"

    int filter4ByteInt = hubitat.helper.HexUtils.hexStringToInt(hexParsed[4])
    logTrace "==> filter4ByteInt= ${filter4ByteInt}"
    boolean isFilter2Active = ((filter4ByteInt & 128) == 128)
    makeEvent('isFilter2Active', isFilter2Active)
    logTrace "==> isFilter2Active= ${isFilter2Active}"
    if (isFilter2Active) {
        String f2Hour = (filter4ByteInt & 127).toString().padLeft(2, '0')
        String Filter2Start = Date.parse("HHmm", "${f2Hour}${decoded[5]}").format(timeFmt)
        makeEvent('Filter2Start', Filter2Start)
        logTrace "==> Filter2Start= ${Filter2Start}"
        String Filter2Duration = Date.parse("HHmm", "${decoded[6]}${decoded[7]}").format("H:mm")
        makeEvent('Filter2Duration', Filter2Duration)
        logTrace "==> Filter2Duration= ${Filter2Duration}"
    }
}

// Information Response
def parseInformationResponse(hexData) {
    logDebug "parseInformationResponse: '${hexData}'"
    List decoded = hexData.split("(?<=\\G.{2})")
    logDebug "==> decoded= ${decoded}"
}

// WiFi Module Configuration Response  00:15:27:35:42:5d
def parseWiFiModuleConfigurationResponse(hexData) {
    logDebug "parseWiFiModuleConfigurationResponse: '${hexData}'"
    List decoded = hexData.split("(?<=\\G.{2})")
    logDebug "==> decoded= ${decoded}"
    String macAddress = (3..8).collect { decoded[it] }.join(':')
    logDebug "==> macAddress= ${macAddress}"
    makeEvent('macAddress', macAddress)
}

def parsePanelData(hexData) {
    logDebug "parsePanelData(${hexData})"
    if (!isSpaConfigOK()) configure()

    byte[] decoded = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    Map decodedMap = [:]
    decoded.eachWithIndex { hexnum, idx ->
        decodedMap["${idx}"] = "${(hexnum & 0xFF).toString().padLeft(2, '0')}"
    }
    logDebug "${decodedMap}"

    // check for different bytes excluding time bytes at [3,4]
    if (state?.lastPanelData) {
        decodedMap['3'] = ''
        decodedMap['4'] = ''
        Map differences = decodedMap.minus(state?.lastPanelData)
        if (differences) logDebug "Different Bytes: ${differences}"
    }
    state.lastPanelData = decodedMap

    String updatedAtStr = new Date().format('EEE MMM d, h:mm:ss a', location.timeZone)

    makeEvent('presence', 'present')
    makeEvent('network', 'online')

    // Get Spa Time
    int spaHour = decoded[3] & 0xFF
    int spaMinute = decoded[4] & 0xFF
    String currentSpaTimeHour = spaHour.toString().padLeft(2, "0")
    String currentSpaTimeMinute = spaMinute.toString().padLeft(2, "0")
    String is24HourTime = (decoded[9] & 2) != 0 ? "true" : "false"
    String spaTime = Date.parse("HHmm", "${currentSpaTimeHour}${currentSpaTimeMinute}").format((is24HourTime.toBoolean()) ? "HH:mm" : "h:mm a")

    // Get delta mins from spa internal clock to hub's localtime (normalized to [-720, 720])
    String[] localHHmm = nowFormatted("HH:mm").split(":")
    int localTotalMins = (localHHmm[0] as Integer) * 60 + (localHHmm[1] as Integer)
    int spaTotalMins = spaHour * 60 + spaMinute
    int durationMins = spaTotalMins - localTotalMins
    if (durationMins > 720) durationMins -= 1440
    else if (durationMins < -720) durationMins += 1440
    makeEvent("spaTimeLocalDeltaMins", durationMins)

    if (settings.autoSyncClock) {
        int threshold = (settings.clockDriftThresholdMins ?: 2) as Integer
        if (Math.abs(durationMins) < threshold) {
            state.clockSyncAttempts = 0
        } else {
            long nowMs = new Date().time
            long lastSyncMs = (state.lastClockSyncMs ?: 0L) as Long
            int attempts = (state.clockSyncAttempts ?: 0) as Integer
            if ((nowMs - lastSyncMs) > 3600000L || attempts < 2) {
                state.lastClockSyncMs = nowMs
                state.clockSyncAttempts = attempts + 1
                logInfo "Spa clock drift (${durationMins} mins) >= ${threshold} mins; syncing spa clock to hub local time (attempt ${state.clockSyncAttempts})"
                runIn(2, 'setSpaToLocalTime')
            }
        }
    }

    String temperatureScale = (decoded[9] & 1) == 0 ? "°F" : "°C"
    logDebug "==> temperatureScale= ${temperatureScale}"
    def actualTemperature = (decoded[2] & 0xFF)
    def targetTemperature = (decoded[20] & 0xFF)

    if (actualTemperature == 255) {
        def prevTemp = device.currentValue("temperature")
        actualTemperature = (prevTemp != null) ? (prevTemp * (temperatureScale == "°C" ? 2.0F : 1)) : -1
    }
    if (temperatureScale == '°C') {
        if (actualTemperature >= 0) actualTemperature = actualTemperature / 2.0F
        targetTemperature = targetTemperature / 2.0F
    }

    // Set the thermostat mode & operating states for the spa
    String thermostatMode
    String thermostatOperatingState
    if ((decoded[10] & 16) == 16) {
        thermostatMode = "heat"
        thermostatOperatingState = "pending heat"
    } else if ((decoded[10] & 8) == 8) {
        thermostatMode = "heat"
        thermostatOperatingState = "heating"
    } else if ((decoded[10] & 8) == 0) {
        thermostatMode = "heat"
        thermostatOperatingState = "idle"
    } else {
        logErr "==> Received an unexpected spa thermostatMode state: hexdecoded[10]= ${decoded[10]}"
        thermostatMode = "off"
        thermostatOperatingState = "idle"
    }
    makeEvent('thermostatMode', thermostatMode)
    makeEvent('temperatureScale', temperatureScale)
    makeEvent('thermostatOperatingState', thermostatOperatingState)

    // Set a state variable to track temperature scale
    state.temperatureScale = temperatureScale

    // heatingMode
    String heatingMode = (decoded[10] & 4) == 4 ? "high" : "low"

    String heatMode
    switch (decoded[5] & 0xFF) {
        case 0:
            heatMode = "Ready"
            break
        case 1:
            heatMode = "Rest"
            break
        case 2:
            heatMode = "Ready in Rest"
            break
        default:
            heatMode = "None"
    }

    if (actualTemperature > -1) {
        makeEvent('temperature', actualTemperature, temperatureScale, 'actual spa temperature')
        makeEvent('temperatureFormatted', "${actualTemperature}${temperatureScale}")
        makeEvent('heatingSetpoint', targetTemperature, temperatureScale, 'target spa temperature')
        makeEvent('heatingSetpointFormatted', "${targetTemperature}${temperatureScale}")
        makeEvent('thermostatSetpoint', targetTemperature, temperatureScale, 'target spa temperature')
    }
    def presetCd = getChildDevice("${device.id}-Spa Preset Temp (High/Low)")
    if (presetCd) {
        BigDecimal highVal = (settings.presetTempHigh ?: 102).toBigDecimal()
        BigDecimal lowVal = (settings.presetTempLow ?: 80).toBigDecimal()
        BigDecimal targetVal = targetTemperature.toBigDecimal()
        if (targetVal == highVal && presetCd.currentValue("switch") != "on") {
            presetCd.sendEvent(name: "switch", value: "on")
        } else if (targetVal == lowVal && presetCd.currentValue("switch") != "off") {
            presetCd.sendEvent(name: "switch", value: "off")
        }
    }

    String filterMode
    switch (decoded[9] & 12) {
        case 4:
            filterMode = "Cycle 1"
            break
        case 8:
            filterMode = "Cycle 2"
            break
        case 12:
            filterMode = "Cycle 1 & 2"
            break
        case 0:
        default:
            filterMode = "off"
    }
    makeEvent("filterMode", filterMode)

    // Spa Pumps
    String pumpState0
    def accessory = 'Pump0'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        pumpState0 = ((decoded[13] & 2) == 2) ? "Active" : "Inactive"
        makeEvent(accessory, pumpState0)
    }

    String pumpState1
    accessory = 'Pump1'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[11] & 3) {
            case 1: pumpState1 = "Low"; break
            case 2: pumpState1 = "High"; break
            default: pumpState1 = "off"
        }
        makeEvent(accessory, pumpState1)
        syncComponentChild(accessory, pumpState1 != "off")
    }

    String pumpState2
    accessory = 'Pump2'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[11] & 12) {
            case 4: pumpState2 = "Low"; break
            case 8: pumpState2 = "High"; break
            default: pumpState2 = "off"
        }
        makeEvent(accessory, pumpState2)
        syncComponentChild(accessory, pumpState2 != "off")
    }

    String pumpState3
    accessory = 'Pump3'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[11] & 48) {
            case 16: pumpState3 = "Low"; break
            case 32: pumpState3 = "High"; break
            default: pumpState3 = "off"
        }
        makeEvent(accessory, pumpState3)
        syncComponentChild(accessory, pumpState3 != "off")
    }

    String pumpState4
    accessory = 'Pump4'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[11] & 192) {
            case 64: pumpState4 = "Low"; break
            case 128: pumpState4 = "High"; break
            default: pumpState4 = "off"
        }
        makeEvent(accessory, pumpState4)
        syncComponentChild(accessory, pumpState4 != "off")
    }

    String pumpState5
    accessory = 'Pump5'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[12] & 3) {
            case 1: pumpState5 = "Low"; break
            case 2: pumpState5 = "High"; break
            default: pumpState5 = "off"
        }
        makeEvent(accessory, pumpState5)
        syncComponentChild(accessory, pumpState5 != "off")
    }

    String pumpState6
    accessory = 'Pump6'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[12] & 12) {
            case 4: pumpState6 = "Low"; break
            case 8: pumpState6 = "High"; break
            default: pumpState6 = "off"
        }
        makeEvent(accessory, pumpState6)
        syncComponentChild(accessory, pumpState6 != "off")
    }

    // Keep Pumps reflecting physical pump activity, but keep switch off when Pump 1 is only on Low for a filter cycle
    boolean anyPumpRunning = (decoded[11] & 0xFF) > 0 || (decoded[12] & 0x0F) > 0
    boolean otherPumpsRunning = (decoded[11] & 0xFC) > 0 || (decoded[12] & 0x0F) > 0
    boolean pump1High = (decoded[11] & 3) == 2
    boolean pump1Low = (decoded[11] & 3) == 1
    boolean filterActive = (decoded[9] & 12) != 0

    makeEvent("Pumps", anyPumpRunning ? "on" : "off")
    if (otherPumpsRunning || pump1High || (pump1Low && !filterActive)) {
        makeEvent("switch", "on")
    } else {
        makeEvent("switch", "off")
    }

    // Blower
    String blowerState
    accessory = 'Blower'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        switch (decoded[17] & 12) {
            case 4: blowerState = "Low"; break
            case 8: blowerState = "Medium"; break
            case 12: blowerState = "High"; break
            default: blowerState = "off"
        }
        makeEvent(accessory, blowerState)
    }

    // Lights
    String light1
    accessory = 'Light1'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        light1 = ((decoded[14] & 3) != 0) ? 'on' : 'off'
        makeEvent(accessory, light1)
        syncComponentChild(accessory, light1 == 'on')
    }
    String light2
    accessory = 'Light2'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        light2 = ((decoded[14] & 12) != 0) ? 'on' : 'off'
        makeEvent(accessory, light2)
        syncComponentChild(accessory, light2 == 'on')
    }

    if (light1 == 'on' || light2 == 'on') makeEvent('Lights', 'on')
    else makeEvent('Lights', 'off')

    // Mister
    String misterState
    accessory = 'Mister'
    if (state.spaConfiguration?.get(accessory)?.installed) {
        misterState = ((decoded[15] & 1) != 0) ? 'on' : 'off'
        makeEvent(accessory, misterState)
    }

    String wifiState
    switch (decoded[16] & 240) {
        case 0: wifiState = "WiFi OK"; break
        case 16: wifiState = "WiFi Spa Not Communicating"; break
        case 32: wifiState = "WiFi Startup"; break
        case 48: wifiState = "WiFi Prime"; break
        case 64: wifiState = "WiFi Hold"; break
        case 80: wifiState = "WiFi Panel"; break
        default: wifiState = "WiFi Unknown"
    }

    // Create events for the parent spa device
    String heatModeText = "${thermostatOperatingState} to ${targetTemperature}${temperatureScale}"
    makeEvent("spaStatus", "${heatModeText}")
    makeEvent("heatMode", "${heatMode}")
    makeEvent("tempRange", "${heatingMode}")
    makeEvent("updated_at", updatedAtStr)
    makeEvent("wifiState", "${wifiState}")
    makeEvent("spaTime", "${spaTime}")
    makeEvent("is24HourTime", "${is24HourTime}")

    def spaItems = [
        "Temp"          : "${actualTemperature}",
        "Spa Time"      : "${spaTime}",
        "Temp - Target" : "${targetTemperature}",
        "Filter Mode"   : "${filterMode}",
        "Heating Mode"  : "${heatingMode}",
        "Light 1"       : "${light1 ?: null}",
        "Light 2"       : "${light2 ?: null}",
        "Heat Mode"     : "${heatMode}",
        "Heating State" : "${thermostatOperatingState}",
        "Pump 0 (Circ)" : "${pumpState0 ?: null}",
        "Pump 1"        : "${pumpState1 ?: null}",
        "Pump 2"        : "${pumpState2 ?: null}",
        "Pump 3"        : "${pumpState3 ?: null}",
        "Pump 4"        : "${pumpState4 ?: null}",
        "Pump 5"        : "${pumpState5 ?: null}",
        "Pump 6"        : "${pumpState6 ?: null}",
        "Blower"        : "${blowerState ?: null}",
        "Mister"        : "${misterState ?: null}",
        "Updated"       : "${nowFormatted('h:mm:ss a')}"
    ]

    if (settings.spaPanelTableRemovePump0 == null || settings.spaPanelTableRemovePump0 == true) {
        spaItems.remove('Pump 0 (Circ)')
    }

    makeEvent('TTSmessage', "Your spa is currently at ${actualTemperature}${temperatureScale} and is ${heatModeText.uncapitalize()}.")

    if (spaPanelDisplay) makeSpaPanelTable(spaItems)
    return true
}

void makeSpaPanelTable(data) {
    logTrace "Creating Device panel HTML table"
    def spaItems = data.findAll { it.value != 'null' && it.value != null }

    // CSS Definition for Table Styling
    String tableCSS = "table.bt{border:2px solid #FFA500;text-align:center;border-collapse:collapse;width:100%}table.bt td,table.bt th{border:1px solid #000000;padding:2px 2px}table.bt thead{border-bottom:2px solid #000000}table.bt thead th{font-weight:bold;color:#FF0000;text-align:center}.g{background-color:green}.r{background-color:red}.b{background-color:blue}"

    // Table Header & Footer
    String tableHeader = "<table class=bt><thead><tr><th>Spa Device</th><th>Value</th><th>Spa Device</th><th>Value</th></tr></thead><tbody>"
    String tableFooter = "</tbody></table>"

    // Table Rows
    int totalItems = spaItems.size()
    int rowsPerCol = Math.max(1, Math.ceil(totalItems / 2.0) as Integer)
    List<String> rowList = (0..<rowsPerCol).collect { "" }
    int i = 0
    spaItems.sort().each { key, value ->
        String valStr = value?.toString() ?: ""
        String cell
        switch (valStr.toLowerCase()) {
            case 'high':
            case 'medium':
            case 'low':
            case 'heat':
            case 'heating':
            case 'on':
            case 'active':
            case 'ready':
            case 'true':
            case 'yes':
                cell = "<td class=r>${valStr}</td>"
                break
            case 'off':
            case 'false':
            case 'idle':
            case 'cool':
            case 'cooling':
            case 'inactive':
            case 'no':
                cell = "<td class=g>${valStr}</td>"
                break
            default:
                if (key.startsWith('Temp')) {
                    BigDecimal numVal = valStr.isBigDecimal() ? valStr.toBigDecimal() : 0
                    BigDecimal hotThreshold = (state.temperatureScale == '°C') ? 37.0 : 99.0
                    String cls = (numVal >= hotThreshold) ? 'r' : 'b'
                    cell = "<td class=${cls}>${valStr}${state.temperatureScale ?: ''}</td>"
                } else {
                    cell = "<td>${valStr}</td>"
                }
                break
        }
        rowList[i] += "<td>${key}</td>${cell}"
        i = (i + 1) % rowsPerCol
    }
    String rows = rowList.findAll { it }.collect { "<tr>${it}</tr>" }.join("")

    // Write CSS styles to a local .css file on Hub once
    String fullWebPathName = "http://${location.hub.localIP}/local/${FILENAME_CSS}"
    if (!state.cssWritten) {
        if (!fileExists(FILENAME_CSS)) writeFile(FILENAME_CSS, tableCSS)
        state.cssWritten = true
    }
    String spaPanel = "<link rel='stylesheet' href=${fullWebPathName}>${tableHeader}${rows}${tableFooter}"

    if (spaPanel.length() > 1024) {
        logWarn "the spaPanelHTML attribute is ${spaPanel.length()} bytes. It will be truncated to 1024 bytes to fit on the HE dashboard"
    }

    makeEvent('spaPanelHTML', spaPanel.take(1024))
}


// Decode the encoded configuration data received from Balboa
def ParseDeviceConfigurationData(hexData) {
    logDebug "ParseDeviceConfigurationData(hexData)-> '${hexData}'"
    byte[] decoded = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    decoded.eachWithIndex { hexnum, idx ->
        logTrace "${idx}: ${(hexnum & 0xFF).toString().padLeft(2, '0')}"
    }

    def pumpConfiguration = [:]
    def accessory = 'Pump0'  // Circulation Pump
    if ((decoded[0] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump1'
    if ((decoded[0] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[0] & 2) == 2 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump2'
    if ((decoded[0] & 12) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[0] & 8) == 8 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump3'
    if ((decoded[0] & 48) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[0] & 32) == 32 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump4'
    if ((decoded[0] & 192) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[0] & 128) == 128 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump5'
    if ((decoded[1] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[1] & 2) == 2 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Pump6'
    if ((decoded[1] & 12) != 0) {
        pumpConfiguration[accessory] = ['installed': true, 'speeds': (decoded[1] & 8) == 8 ? '2' : '1']
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Light1'
    if ((decoded[2] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Light2'
    if ((decoded[2] & 192) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Blower'
    if ((decoded[3] & 1) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Aux1'
    if ((decoded[4] & 1) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Aux2'
    if ((decoded[4] & 2) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)
    accessory = 'Mister'
    if ((decoded[4] & 16) != 0) {
        pumpConfiguration[accessory] = ['installed': true]
        makeEvent(accessory, 'Installed')
    } else device.deleteCurrentState(accessory)

    logDebug "==> pumpConfiguration Map= ${pumpConfiguration}"
    state.spaConfiguration = pumpConfiguration
    logDebug "ParseDeviceConfigurationData() state.spaConfiguration= ${state.spaConfiguration}"
    runIn(2, 'getFilterSchedule')
}

def makeEvent(name, value, unit = null, descriptionText = null) {
    if (name != 'updated_at' && name != 'spaSessionStatus') {
        def cur = device.currentValue(name)
        if (cur != null && cur.toString() == value?.toString()) {
            return
        }
    }
    Map evt = [name: name, value: value]
    if (unit != null) evt.unit = unit
    if (descriptionText != null) evt.descriptionText = descriptionText
    sendEvent(evt)
    logTrace "<span style='color:green'><b>sendEvent(</b>${evt})</span>"
}

def calculateChecksum(byte[] data) {
    /** Calculate the checksum byte for a message. */
    int checksum = 0xB5
    for (int index = 0; index < data.length; index++) {
        byte currentByte = data[index]
        for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
            int bit = checksum & 0x80
            checksum = ((checksum << 1) & 0xFF) | ((currentByte >> (7 - bitIndex)) & 0x01)
            if (bit) {
                checksum ^= 0x07
            }
        }
        checksum &= 0xFF
    }
    for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
        int bit = checksum & 0x80
        checksum = (checksum << 1) & 0xFF
        if (bit) {
            checksum ^= 0x07
        }
    }
    return hubitat.helper.HexUtils.integerToHexString(checksum ^ 0x02, 1)
}

// **************************************************************************************************
// **************************************************************************************************
// ************************BALBOA PROTOCOL TCP SOCKET ENDPOINT FUNCTIONS ****************************
// **************************************************************************************************
// **************************************************************************************************

// Callback function used by HE to notify about socket changes
// This has been reported to be buggy
def socketStatus(String socketMessage) {
    logErr "Socket Status Message Received: ${socketMessage}"
}

// Wrapper for socket_connect
def connect() {
    logInfo "Attempting to connect to spa..."
    return socket_connect()
}

boolean socket_connect() {
    if (!settings.ipaddress) {
        logErr "Socket Connect Error: Device IP address is not configured."
        return false
    }
    // Check contact status for socket_status
    if (device.currentValue("contact") == 'open') {
        logInfo "Socket is already open"
        return true
    }

    int readDelayMs = (socketConnectReadDelay ?: SOCKET_CONNECT_READ_DELAY) as Integer
    int waitSecs = (readWaitSecs ?: READ_WAIT_SECS) as Integer
    int portNum = (port ?: '4257').toInteger()

    logDebug "Socket Connect(): ${settings.ipaddress} at port: ${portNum} for a delay reads of ${readDelayMs} millisecs"
    boolean rc = false
    // Reset device message counters and per-session CRC cache
    state.processedMessageCount = 0
    state.rawMessageCount = 0
    state.lastMessage = [:]

    try {
        logInfo "Attempting to connect to Spa device..."
        interfaces.rawSocket.connect(settings.ipaddress, portNum, byteInterface: true, readDelay: readDelayMs)
        rc = true
        logDebug "Reset auto closing device socket timer to ${waitSecs} secs"
        runIn(waitSecs, 'socket_close')
    } catch (java.net.NoRouteToHostException ex) {
        logErr "Error: No Route To Host - Can't connect to spa, make sure spa is on the network. Exiting.."
        rc = false
    } catch (java.net.SocketTimeoutException ex) {
        logErr "Error: Socket Timeout - Can't connect to spa's socket, make sure spa is online and accepting network connections. Exiting.."
        rc = false
    } catch (e) {
        logErr "Error: $e"
        rc = false
    }
    logDebug "Socket_Connect rc=${rc}"
    makeEvent('contact', rc ? 'open' : 'closed')
    makeEvent('presence', rc ? 'present' : 'not present')
    makeEvent('network', rc ? 'online' : 'offline')
    return rc
}

// Wrapper for socket_write
def send(message) {
    logInfo "Sending command to Spa device..."
    return socket_write(message)
}

def socket_write(message) {
    logDebug "Socket: write - ${settings.ipaddress}:${port} → message: ${message}"
    if (message == null || message == '') {
        logErr "socket_write(${message}): message argument is either null or empty, exiting..."
        return false
    }
    // Open socket if not open
    if (device.currentValue("contact") != 'open') {
        if (!socket_connect()) {
            logErr "socket_write aborted: failed to connect to spa at ${settings.ipaddress}:${port}"
            return false
        }
    }

    int waitSecs = (readWaitSecs ?: READ_WAIT_SECS) as Integer
    logDebug "Reset auto closing socket timer to ${waitSecs} secs to send '${message}' to device"
    runIn(waitSecs, 'socket_close')

    boolean rc = false
    try {
        interfaces.rawSocket.sendMessage(message)
        rc = true
    } catch (e) {
        logErr "Error sending ${message} to device: $e"
    }
    return rc
}

// Wrapper for socket_close
def disconnect() {
	return socket_close()
}

boolean socket_close() {
    unschedule('socket_close')
    state.expectConfig = false
    if (device.currentValue("presence") == 'present') logInfo  "Command complete at ${nowFormatted('MMM-dd h:mm:ss a')}"
    else {
        def msg = "Command Error: The Spa is 'Offline' at ${nowFormatted('MMM-dd h:mm:ss a')}"
        logInfo msg
        makeEvent('TTSmessage', msg)
    }

    boolean rc = false //default

	try {
		interfaces.rawSocket.close()
        rc = true
	} catch (e) {
		logErr "Error: Could not close socket: $e"
        rc = false
        return rc
	}
    makeEvent("contact", "closed")
    return rc
}