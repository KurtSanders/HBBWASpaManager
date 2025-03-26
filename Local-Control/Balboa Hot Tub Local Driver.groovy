// hubitat start
// hub: 10.0.0.77  <- this is hub's IP address
// type: device         <- valid values here are "app" and "device"
// id: 1161           <- this is app or driver's id
// hubitat end
/**
 * Hubitat Balboa Hot Tub Driver Integration by Kurt Sanders 2025
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
        capability "TemperatureMeasurement"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatSetpoint"
    	capability "ContactSensor"
    	capability "PresenceSensor"
	    capability "FanControl"

        attribute "Blower", "enum", ["off","low","medium","high"]
    	attribute 'Filter1Start', 'string'
    	attribute 'Filter1Duration', 'string'
    	attribute 'Filter2Enabled', 'string'
    	attribute 'Filter2Start', 'string'
    	attribute 'Filter2Duration', 'string'
        attribute "filterMode", "enum", ["off", "cycle 1", "cycle 2", "cycle 1 and 2"]
        attribute "heatMode", "enum", HEATMODES
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
        attribute "temperature", "number"
        attribute "tempRange", "enum", TEMPRANGES
        attribute "TTSmessage", "string"
        attribute "updated_at", "string"
        attribute "wifiState", "enum", ["WiFi OK","WiFi Spa Not Communicating","WiFi Startup","WiFi Prime","WiFi Hold","WiFi Panel","WiFi Unnknown"]

        attribute "supportedThermostatFanModes", 'JSON_OBJECT'
        attribute "supportedThermostatModes", 'JSON_OBJECT'
        attribute "thermostatOperatingState", "enum",  THERMO_STAT_OPERATING_STATE
        attribute "thermostatMode", "enum", THERMO_STAT_MODES

        attribute "supportedThermostatFanModes", 'JSON_OBJECT'
        attribute "thermostatFanMode", "enum", THERMO_STAT_FAN_MODES
        attribute "speed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "setSpeed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "supportedFanSpeeds", 'JSON_OBJECT'

	    command "createAllChildrenSpaSwitchs"
        command "disconnect"
        command "setHeatingSetpoint"		, [[name:'Heating Setpoint 55-104°F*'	, type:'NUMBER', description:'Heating setpoint temperature from 55°F-104°F', range: "55..104"]]
    	command "setLights"					, [[name:"Set Spa Lights On/Off*"		, type:"ENUM", description:"Set Spa Lights (On/Off)", constraints:LIGHTSMODES]]
        command "setHeatMode"    			, [[name:"Set Heat Mode*"				, type:"ENUM", description:"Set Heat Mode (Ready/Rest)", constraints:HEATMODES]]
        command "setSpaToLocalTime"
        command "setSpeed"					, [[name:"Pump speed*"					, type:"ENUM", description:"Pump speed to set", constraints:SUPPORTED_PUMP_SPEED_MODES]]
        command "setTempRange"    			, [[name:"Set Temp Range*"				, type:"ENUM", description:"Set Temperature Range of the Spa", constraints:TEMPRANGES]]
	}
}

// Constants
@Field static final String  PARENT_DEVICE_NAME   		= "Balboa Hot Tub Local Driver"
@Field static final String  CHILD_DEVICE_NAME_SWITCH  	= "Balboa Hot Tub Local Child Switch"
@Field static final String  VERSION 					= "0.0.4"
@Field static final String  FILENAME_CSS 				= "BalboaLocalPanelTable.css"
@Field static final Integer READ_WAIT_SECS 				= 10

preferences {
	input "ipaddress", "text", title: "Device IP:", required: true,
        description: "Balboa device local IP address. Tip: Configure a fixed IP address for your spa on the same network as your Hubitat hub to make sure the spa's IP address does not change."

    input name: "poll_interval", type: "enum", title: "Configure device auto polling interval", defaultValue: 'Off', options: POLLING_OPTIONS,
        description: "Auto poll the panel status of the deivce. Select 'Off' to stop automatic polling."
    input name: "spaPanelDisplay", type: "bool", defaultValue: 'false', title: fmtTitle("Create Spa Panel Table for Dashboard"),
        description: fmtDesc("An HTML Matrix Table for HE dashboard")

    //	Logging Levels & Help
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
    	description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
    	description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
    //  Display Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

void installed() {
    setLogLevel("Debug", "30 Minutes")
	logInfo "Setting Inital logging level to 'Debug' for 30 minutes"
    device.updateSetting('poll_interval', [type: "enum", value: 'off'])
    logInfo "Setting Spa Thermo defaults..."
    setsupportedFanSpeeds()
    makeEvent("switch", "off")
    String stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_MODES).toString()
    sendEvent(name: "supportedThermostatModes", value: stmJSON, displayed: false, isStateChange: true)
    stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_FAN_MODES).toString()
    sendEvent(name: "supportedThermostatFanModes", value: stmJSON, displayed: false, isStateChange: true)
    makeEvent("thermostatFanMode", "off")
    makeEvent("thermostatMode", "off")
    createAllChildrenSpaSwitchs()
}

void updated() {
	logDebug "Preferences Updated..."
    if (ipaddress==null || ipaddress.isEmpty()) return

    checkLogLevel()  // Set Logging Objects

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
    state.spaConfiguration.sort().each { key, value ->
        logDebug "state.spaConfiguration: ${key}: = ${value}"
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
    createAllChildrenSpaSwitchs()
}

def createAllChildrenSpaSwitchs() {

    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Refresh Local Spa"      , [functionName: 'refresh'])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Heat Mode to Ready" , [functionName: 'setHeatMode', parameter: "Ready"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Heat Mode to Rest"  , [functionName: 'setHeatMode', parameter: "Rest"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Temp Range to High" , [functionName: 'setTempRange', parameter: "High"])
    createChildren(true, CHILD_DEVICE_NAME_SWITCH, "Set Temp Range to Low"  , [functionName: 'setTempRange', parameter: "Low"])
}

def createChildren(createIfDoesntExist, String type, String name, Map apiMap = null) {
    String thisId = device.id
    def childDeviceName = "${thisId}-${name}"
    logTrace "childDeviceName: '${childDeviceName}"

    def cd = getChildDevice(childDeviceName)
    if (!cd && createIfDoesntExist) {

        logInfo "Adding Child Device Driver: ${type}, Name: '${childDeviceName}' with apiMap: ${apiMap}"
        cd = addChildDevice(NAMESPACE, type, childDeviceName, [name: "${device.displayName} - ${name}", label: "${name}", isComponent: false])
    }
    // child devices will create a state.apiMap variable
    if (apiMap) {
        cd.setApiMap(apiMap)
    }
    return cd
}

void childRunApi(device, apiMap) {
    logInfo "Dynamic Function Call: '${apiMap}' received from ${device} child device."
    if (apiMap.parameter) {
	    def cmd = this.&(apiMap.functionName)(apiMap.parameter)
    } else {
	    def cmd = this.&(apiMap.functionName)()
    }
    cmd
}

void setsupportedFanSpeeds() {
    def modes = new groovy.json.JsonBuilder(SUPPORTED_PUMP_SPEED_MODES)
    sendEvent(name: "supportedFanSpeeds", value: modes, type:"digital", isStateChange: true, descriptionText:"Supported pump speeds initialized to ${SUPPORTED_PUMP_SPEED_MODES}")
}

void refresh(args=null) {
	logTrace ("refresh()")
    send(NOTHING_TO_SEND)
}

void configure() {
	logTrace ("configure()")
	// Get Device Configuration
    send(GET_DEVICES)
}

boolean isSpaConfigOK() {
	def exists = (state.spaConfiguration && !state.spaConfiguration.empty)
    if (!exists) logErr "Configuration Error: state.spaConfiguration is missing"
    return exists
}

def setTempRange(mode=null) {
    logDebug "setTempRange(${mode})..."
    mode = mode.toLowerCase()

    if (!['low','high'].contains(mode)) {
        logErr "Invalid Temp Range '${mode}'.. Exiting"
        return false
    }

    if (!isSpaConfigOK()) configure()
    else refresh() // Get Latest Spa Accessory States

    def tempRangeCurrent = device.currentValue("tempRange").toLowerCase()
    logDebug "Device current heating mode is ${tempRangeCurrent.capitalize()}"
    if (tempRangeCurrent != mode) {
        logDebug "Setting New TempRange to '${mode.capitalize()}'"
		send(SET_TEMP_RANGE)
    } else {
       logErr "Spa is already in '${mode.capitalize()}' mode, command ignored"
    }
}

def setHeatMode(mode=null) {
    logDebug "setHeatMode(${mode})"
    mode = mode.toLowerCase()

    if (!['ready','rest'].contains(mode)) {
        logErr "Invalid Heat Mode '${mode}'.. Exiting"
        return false
    }

    if (!isSpaConfigOK()) configure()
    else refresh() // Get Latest Spa Accessory States

    def currentHeatMode = device.currentValue("heatMode")
    logDebug "Device current mode is ${heatMode}"

    if (heatMode != mode) {
        send(SET_HEAT_MODE)
        logDebug "Setting new heatMode to '${mode}'"
    } else {
        logErr "Error: Spa is already in '${heatMode}' mode, command ignored"
    }
}

void setLights(mode) {
    logDebug ("setLights(${mode})")
    if (!isSpaConfigOK()) configure()
    else refresh() // Get Latest Spa Accessory States
    if (mode) {
        state.spaConfiguration.findAll {it.key.startsWith('Light')}.each { outerKey, outerValue ->
            if (state.spaConfiguration[outerKey].installed) {
                logInfo "Checking spa state of '${outerKey}'"
                if (device.currentValue(outerKey) != mode) {
                    logInfo "Setting '${outerKey}' to ${mode}"
                    logDebug "Toggle ${outerKey}: Send " +  this."SET_${outerKey.toUpperCase()}"
                    send(this."SET_${outerKey.toUpperCase()}")
                    pauseExecution(500)
                } else logInfo "Set Lights ${mode}: Skipping ${outerKey}, already '${device.currentValue(outerKey)}'"
            }
        }
    }
}

void on() {
	logDebug ("Switch: on()")
    if (!isSpaConfigOK()) configure()
    state.spaConfiguration.findAll {it.key =~ /Pump[123456]/}.each { outerKey, outerValue ->
    	if (state.spaConfiguration[outerKey].installed) {
            logDebug "Toggle ${outerKey}: Send " +  this."SET_${outerKey.toUpperCase()}"
  			send(this."SET_${outerKey.toUpperCase()}")
            pauseExecution(1000)
    	}
	}
}

void off() {
	logDebug ("Switch: off()")
    logDebug "Set Soak Mode"
    send(SET_SOAK_MODE)
}

def updateThermostatSetpoints(setpoint) {
    logInfo "==>updateThermostatSetpoints(${setpoint})"
    // Verify a valid temperature
	def tempRange = device.currentValue("tempRange")
    if (setpoint >= TEMPERATURE_VALID_RANGES_F[tempRange][0] && setpoint <= TEMPERATURE_VALID_RANGES_F[tempRange][1]) {
        logDebug "Valid Temp ${setpoint}"
    	makeRequest("setTemperature", setpoint, true)
    } else {
        logErr "Invalid setpoint temp ${setpoint}"
        return
    }
}

void setHeatingSetpoint(setpoint) {
    logTrace "==> setHeatingSetpoint(), waiting 5 secs for more input to updateThermostatSetpoint to ${setpoint}..."
    runIn(5, "updateThermostatSetpoints", [overwrite: true, data: setpoint])
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
            break
    }
    logDebug "==> '${command}' messageRequest= ${messageRequest}"
    if (ok2send) rc=send(messageRequest)
    else return messageRequest
}

void removeDeviceSetting(devname) {
    device.removeSetting(devname)
}

void generateToggleHexStrings() {
    TOGGLE_ITEM_HEX_CODE.each{key, hexCode ->
        messageRequest = "07${CHANNEL}11${hexCode}00"
        byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(messageRequest)
        def CRCByte = calculateChecksum(decodedByte)
        messageRequest = "${MESSAGE_DELIMITER}${messageRequest}${CRCByte}${MESSAGE_DELIMITER}"
        logDebug "\n@Field static final String GET_${key.toUpperCase()} = '${messageRequest}'"
    }
}

def parseUDP(message) {
    logDebug "==> message= ${message}"
}

void cycleSpeed() {
	on()
}

void setSpeed(speedName) {
    logTrace "setSpeed(${speedName})"
    if (currentPresence != "present") {
        logErr "Spa is 'offline', Ignoring setSpeed request for '${speedName}'.  Try a COMMAND refresh to check online status"
        return
    }
    switch (speedName) {
        case 'off':
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
        break
    }
    logTrace "setSpeed/Speed Decoded: → '${speedName}'"
    if (device.currentValue("speed") == speedName) {
        logWarn "The spa is currently at '${speedName}'...Ignoring redundant request to setSpeed to ${speedName}..."
        return
    }
    def curentSpeedIndex = pumpSpeedListIndex()
    logTrace "curentSpeedIndex= ${curentSpeedIndex}"
    def targetSpeedIndex = pumpSpeedListIndex(speedName)
    logTrace "targetSpeedIndex= ${targetSpeedIndex}"
    def buttonPresses = (targetSpeedIndex - curentSpeedIndex > 0)?(targetSpeedIndex - curentSpeedIndex):(3 + (targetSpeedIndex - curentSpeedIndex))
    logTrace "buttonPresses= ${buttonPresses}"
    logInfo "The # virtual button presses to send from ${device.currentValue("speed")} to ${speedName} = ${buttonPresses}"

//    sendEvent(name: "setSpeed", value: speedName)
//    sendEvent(name: "speed"   , value: speedName)
//    sendEvent(name: "switch"  , value: (speedName=="off")?speedName:"on")
    logTrace "parent?.sendCommand('Button',${device.currentValue('balboaAPIButtonNumber')},${buttonPresses})"
//    parent?.sendCommand("Button", device.currentValue("balboaAPIButtonNumber"),buttonPresses)
}


// Process the raw hexstring message packets from the device
def parse(String message) {
    message = message.toUpperCase()
    logDebug "parse(${message})"
    String hexData
    byte[] decodedByte
    def cs
    String messageType
    state.messageCount = (state?.messageCount)?state.messageCount + 1:1
    if (state.messageCount > MAX_MESSAGES) {
        logDebug "Auto closing device socket after ${state.messageCount} messages."
        disconnect()
    }

	List<String> hexDataList = message.findAll(/${MESSAGE_DELIMITER}.*?${MESSAGE_DELIMITER}/).unique { a, b -> a <=> b } as String[]
    hexDataList.each {
        // Check for invalid 7E7E message packets and discard them
        if (it.length() < 5) return

        // Redundant message check...
        String CRCByte = it[-4..-3]
        if (state?.lastCRCByte == CRCByte) {
            logDebug "Message #:${state.messageCount} CRCByte ${CRCByte} is redundant and will be ignored!"
            return
        }
        try {
	        // Validate message integrity usig CRC-8
			hexData = it[2..-5]
        	decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
        	cs = calculateChecksum(decodedByte)
        	if (CRCByte == cs) {
            	logTrace "Checksum Byte Verification Passed for Message #${state.messageCount} '${it}': ${CRCByte} <> ${cs}"
        	} else {
            	logTrace "Checksum Byte Verification Failed for Message #${state.messageCount} '${it}': ${CRCByte} <> ${cs}.  Message #${state.messageCount} Ignored."
				return
        	}
	        // Let's process the message
    	    state.lastCRCByte = CRCByte
        	logDebug "Unprocessed hexData for message ${state.messageCount}: ${it}"
	        messageType  	= it[8..9]
    	    logDebug "==> messageType for message ${state.messageCount}: ${messageType}"
        	messagePacket	= it[10..-5]
        	logTrace "==> messagePacket for message ${state.messageCount}: ${messagePacket}"
        } catch (java.lang.StringIndexOutOfBoundsException ex) {
            logErr "Error processing message packet: '${it}'"
            return
        } catch (e) {
            logErr "Error ${e} processing message packet: '${it}'"
            return
        }

        if (BALBOA_MESSAGE_TYPES[messageType]) {
            makeEvent('spaSessionStatus', "<span style=color:yellow;>Received a ${BALBOA_MESSAGE_TYPES[messageType].name} response from spa</span>")
            logDebug "<span style=color:${BALBOA_MESSAGE_TYPES[messageType].color};>${BALBOA_MESSAGE_TYPES[messageType].name} Update: ${messagePacket}</span>"
        } else {
            logErr "<span style=color:red;>Message type#: ${messageType} unknown: ${messagePacket}</span>"
            return
        }
        if(BALBOA_MESSAGE_TYPES[messageType].program) {
            logDebug "Calling ${BALBOA_MESSAGE_TYPES[messageType].name} Message Handler: ${BALBOA_MESSAGE_TYPES[messageType].program}"
	        // Dynamically call the correct parsing program for the messagePacket hex number
            def messageHandler = this.&(BALBOA_MESSAGE_TYPES[messageType].program)
            messageHandler(messagePacket)
        } else {
            logDebug "No Message Handler is defined for ${BALBOA_MESSAGE_TYPES[messageType].name} for message ${state.messageCount}: ${messagePacket}"
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
    logDebug ("parseFilterResponse: '${hexData}'")
    List hexParsed = hexData.split("(?<=\\G.{2})")
    List eventData = []
    logTrace "==> hexParsed= ${hexParsed}"
    byte[] decodedByte = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    def decoded = decodedByte.collect { it.abs().toString().padLeft(2,'0') }
    decoded.eachWithIndex{ hexnum, idx ->
        logDebug "${idx}: ${hexnum}"
    }
    boolean is24HourTime = device.currentValue("is24HourTime").toBoolean()
    logTrace "==> is24HourTime= ${is24HourTime}"
    String Filter1Start = Date.parse("HHmm", "${decoded[0]}${decoded[1]}").format((is24HourTime.toBoolean())?"HH:mm":"h:mm a")
    logTrace "==> Filter1Start= ${Filter1Start}"
    makeEvent('Filter1Start',Filter1Start)
    String Filter1Duration = Date.parse("HHmm", "${decoded[2]}${decoded[3]}").format("h:mm")
    makeEvent('Filter1Duration',Filter1Duration)
    logTrace "==> Filter1Duration= ${Filter1Duration}"

    int filter4ByteInt = hubitat.helper.HexUtils.hexStringToInt(hexParsed[4])
    logTrace "==> filter4ByteInt= ${filter4ByteInt}"
    boolean isFilter2Active = ((filter4ByteInt & 128) == 128)
    makeEvent('isFilter2Active', isFilter2Active)
    logTrace "==> isFilter2Active= ${isFilter2Active}"
    if (isFilter2Active) {
    	String Filter2Start = Date.parse("HHmm", "${filter4ByteInt&127}${decoded[5]}").format((is24HourTime.toBoolean())?"HH:mm":"h:mm a")
        makeEvent('Filter2Start',Filter2Start)
        logTrace "==> Filter2Start= ${Filter2Start}"
	    String Filter2Duration = Date.parse("HHmm", "${decoded[6]}${decoded[7]}").format("h:mm")
        makeEvent('Filter2Duration',Filter2Duration)
    	logTrace "==> Filter2Duration= ${Filter2Duration}"
    }
}

// Information Response
def parseInformationResponse(hexData) {
    logDebug ("parseInformationResponse: '${hexData}'")
    List decoded = hexData.split("(?<=\\G.{2})")
    logDebug "==> decoded= ${decoded}"
}

// WiFi Module Configuration Response  00:15:27:35:42:5d
def parseWiFiModuleConfigurationResponse(hexData) {
    logDebug ("parseWiFiModuleConfigurationResponse: '${hexData}'")
    List decoded = hexData.split("(?<=\\G.{2})")
    logDebug "==> decoded= ${decoded}"
    String macAddress = '' // hexData[6..17]
    for(int i = 3;i<9;i++) {
        macAddress += "${decoded[i]} ${(i<8)?':':''}"
    }
    logDebug "==> macAddress= ${macAddress}"
	makeEvent('macAddress',macAddress)
}

def parsePanelData(hexData) {
    logDebug "parsePanelData(${hexData})"
    if (!isSpaConfigOK()) configure()

    byte[] decoded = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    Map decodedMap = [:]
    decoded.eachWithIndex{ hexnum, idx ->
        decodedMap[("${idx}")] = "${hexnum.abs().toString().padLeft(2,'0')}"
    }
    logDebug "${decodedMap}"

    // check for different bytes excluding time bytes at [4,5]
    if (state?.lastPanelData) {
        decodedMap['3'] = ''
        decodedMap['4'] = ''
        Map differences = decodedMap.minus(state?.lastPanelData)
        if (differences) logDebug "Different Bytes: ${differences}"
    }
    state.lastPanelData = decodedMap

    def now = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)

    makeEvent('presence', 'present')
    makeEvent('network', 'online')

    // Get Spa Time
    String currentSpaTimeHour = decoded[3].toString().padLeft(2,"0")
    String currentSpaTimeMinute = decoded[4].toString().padLeft(2,"0")
    String is24HourTime = (decoded[9] & 2) != 0 ? "true" : "false"
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
        makeEvent("spaTimeLocalDeltaMins",durationMins.toInteger())
    }
    def temperatureScale = (decoded[9] & 1) == 0 ? "F" : "C"
    def actualTemperature = decoded[2]
    def targetTemperature = decoded[20]
    def isHeating = (decoded[10] & 44) != 0

    def heatingMode = (decoded[10] & 4) == 4 ? "high" : "low"

    def heatMode
    switch (decoded[5]) {
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

    if (actualTemperature > -1) {
        makeEvent('temperature', actualTemperature, temperatureScale)
        makeEvent('heatingSetpoint', targetTemperature, temperatureScale)
        makeEvent('thermostatSetpoint', targetTemperature, temperatureScale)
    }
    makeEvent('temperatureScale',temperatureScale)
    makeEvent('thermostatMode', "heat")
    makeEvent('thermostatOperatingState', isHeating ? "heating" : "idle")

    def filterMode
    switch (decoded[9] & 12) {
        case 4:
        filterMode = "Cycle 1"
        break;
        case 8:
        filterMode = "Cycle 2"
        break;
        case 12:
        filterMode = "Cycle 1 & 2"
        break;
        case 0:
        default:
            filterMode = "off"
    }
    makeEvent("filterMode",filterMode)

    // Spa Pumps
    def pumpState0 // Circulation Pump
    def accessory = 'Pump0'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[13] & 2) { // Pump 0
                case 2:
                pumpState0 = "Active"
                break
                default:
                    pumpState0 = "Inactive"
            }
            makeEvent(accessory, pumpState0)
        }
    }

    def pumpState1
    accessory = 'Pump1'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[11] & 3) { // Pump 1
                case 1:
                pumpState1 = "Low"
                break
                case 2:
                pumpState1 = "High"
                break
                default:
                    pumpState1 = "off"
            }
            makeEvent(accessory, pumpState1)
        }
    }

    def pumpState2
    accessory = 'Pump2'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[11] & 12) { // Pump 2
                case 4:
                pumpState2 = "Low"
                break
                case 8:
                pumpState2 = "High"
                break
                default:
                    pumpState2 = "off"
            }
            makeEvent(accessory, pumpState2)
        }
    }

    def pumpState3
    accessory = 'Pump3'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[11] & 48) { // Pump 3
                case 16:
                pumpState3 = "Low"
                break
                case 32:
                pumpState3 = "High"
                break
                default:
                    pumpState3 = "off"
            }
            makeEvent(accessory, pumpState3)
        }
    }

    def pumpState4
    accessory = 'Pump4'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[11] & 192) { // Pump 4
                case 64:
                pumpState4 = "Low"
                break
                case 128:
                pumpState4 = "High"
                break
                default:
                    pumpState4 = "off"
            }
        makeEvent(accessory, pumpState4)
        }
    }

    def pumpState5
    accessory = 'Pump5'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[12] & 3) {
                case 1:
                pumpState5 = "Low"
                break
                case 2:
                pumpState5 = "High"
                break
                default:
                    pumpState5 = "off"
            }
            makeEvent(accessory, pumpState5)
        }
    }

    def pumpState6
    accessory = 'Pump6'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
        switch (decoded[12] & 12) {
                case 4:
                pumpState6 = "Low"
                break
                case 8:
                pumpState6 = "High"
                break
                default:
                    pumpState6 = "off"
            }
            makeEvent(accessory, pumpState6)
        }
    }

	// If any pump is on, switch = on
        if (decoded[11] > 0 && decoded[12] > 0) {
        makeEvent("switch", "on")
        makeEvent("pumps", "on")
    } else {
        makeEvent("switch", "off")
        makeEvent("pumps", "off")
    }

    // As far as I know, blower is not controllable, just report state in parent child device?
    def blowerState
    accessory = 'Blower'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
            switch (decoded[17] & 12) {
                case 4:
                blowerState = "Low"
                break
                case 8:
                blowerState = "Medium"
                break
                case 12:
                blowerState = "High"
                break
                default:
                    blowerState = "off"
            }
            makeEvent(accessory, blowerState)
        }
    }

    // Lights
    def light1
    accessory = 'Light1'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
	        light1 = (((decoded[14] & 3) != 0)?'on':'off')
    	    makeEvent(accessory, light1)
        }
    }
    def light2
    accessory = 'Light2'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
	        light2 = (((decoded[14] & 12) != 0)?'on':'off')
    	    makeEvent(accessory, light2)
        }
    }

    if (light1 == 'on' || light2 == 'on') makeEvent(lights, 'on')
    else makeEvent('lights', 'off')


    // Mister
    def misterState
    accessory = 'Mister'
    if(state.spaConfiguration.containsKey(accessory)) {
        if (state.spaConfiguration[accessory].installed) {
	        misterState = ((decoded[15] & 1) != 0)
    	    makeEvent(accessory, misterState)
        }
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
    def heatModeText = (isHeating?"${heatMode} heating to ${targetTemperature}°${temperatureScale}" : "${heatMode} not heating")
    makeEvent("spaStatus",                	"${heatModeText}" 	)
    makeEvent("heatMode",                 	"${heatMode}"     	)
    makeEvent("tempRange",                 	"${heatingMode}"  	)
    makeEvent("updated_at",                	"${now}"			)
    makeEvent("online",                    	"online"			)
    makeEvent("wifiState",                 	"${wifiState}"		)
    makeEvent("spaTime",  					"${spaTime}" 		)
    makeEvent("is24HourTime",  				"${is24HourTime}"	)

    def spaItems = [
        "Temp"                       : "${actualTemperature}",
        "Spa Time"                   : "${spaTime}",
        "Temp - Target"              : "${targetTemperature}",
        "Filter Mode"                : "${filterMode}",
        "Heating Mode"               : "${heatingMode}",
        "Light 1"                    : "${(light1)?:null}",
        "Light 2"                    : "${(light2)?:null}",
        "Heat Mode"                  : "${heatMode}",
        "Is Heating"                 : "${(isHeating)?'Yes':'No'}",
        "Pump 0 (Circ)"              : "${(pumpState0)?:null}",
        "Pump 1"                     : "${(pumpState1)?:null}",
        "Pump 2"                     : "${(pumpState2)?:null}",
        "Pump 3"                     : "${(pumpState3)?:null}",
        "Pump 4"                     : "${(pumpState4)?:null}",
        "Pump 5"                     : "${(pumpState5)?:null}",
        "Pump 6"                     : "${(pumpState6)?:null}",
        "Blower"	                 : "${(blowerState)?:null}",
        "Mister"    	             : "${(misterState)?:null}",
        "Updated"                    : "${nowFormatted('h:mm:ss a')}"
    ]

    makeEvent('TTSmessage', "Your spa is currently at ${actualTemperature}°${temperatureScale} and is ${heatModeText.uncapitalize()}.")

    if (spaPanelDisplay) makeSpaPanelTable(spaItems)
    return true
}

void makeSpaPanelTable(data) {
    logTrace "Creating Device panel HTML table"
    def spaItems = data.findAll{ it.value!='null' }

    // CSS Definition for Table Styling
    def tableCSS = "table.bt{border:2px solid #FFA500;text-align:center;border-collapse:collapse;width:100%}table.bt td,table.bt th{border:1px solid #000000;padding:2px 2px}table.bt thead{border-bottom:2px solid #000000}table.bt thead th{font-weight:bold;color:#FF0000;text-align:center}.g{background-color:green}.r{background-color:red}.b{background-color:blue}"

    // Table Header
    def tableHeader = "<table class=bt><thead><tr><th>Spa Device</th><th>Value</th><th>Spa Device</th><th>Value</tr></thead></tbody>"

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
            case 'Yes':
            value = "<td class=r>${value}</td>"
            break
            case 'off':
            case 'false':
            case 'inactive':
            case 'No':
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

    // Write CSS styles to a local .css file on Hub and update spaPanel to refere to external csss file
    def fullWebPathName = "http://${location.hub.localIP}/local/${FILENAME_CSS}"
    if (!fileExists(FILENAME_CSS)) writeFile(FILENAME_CSS, tableCSS)
    spaPanel = "<html><head><link rel='stylesheet' href=${fullWebPathName}></head><body>${tableHeader}${rows}${tableFooter}</body></html>"

    if (spaPanel.length()>1024) {
        logWarn "the spaPanelHTML attribute is ${spaPanel.length()} bytes.  It which will be truncated to 1024 bytes to fit on the HE dashboard"
    }

    sendEvent(name:'spaPanelHTML',value: spaPanel.take(1024))
}


// Decode the encoded configuration data received from Balboa
def ParseDeviceConfigurationData(hexData) {
    logDebug ("ParseDeviceConfigurationData(hexData)-> '${hexData}'")
    byte[] decoded = hubitat.helper.HexUtils.hexStringToByteArray(hexData)
    decoded.eachWithIndex{ hexnum, idx ->
        logTrace "${idx}: ${hexnum.abs().toString().padLeft(2,'0')}"
    }

    def pumpConfiguration = [:]
    def accessory = 'Pump0'  // Blower
    if ((decoded[0] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump1'
    if ((decoded[0] & 3) != 0) {
    	pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[0] & 2) == 2 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump2'
    if ((decoded[0] & 12) != 0) {
    	pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[0] & 8) == 8 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump3'
    if ((decoded[0] & 48) != 0) {
    	pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[0] & 58) == 8 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump4'
    if ((decoded[0] & 192) != 0) {
	    pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[0] & 58) == 8 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump5'
    if  ((decoded[1] & 3) != 0) {
    	pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[1] & 2) == 2 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Pump6'
    if ((decoded[1] & 192) != 0) {
    	pumpConfiguration[accessory] = ['installed' : true ]
    	pumpConfiguration[accessory] << ['speeds':(decoded[1] & 8) == 8 ? '2' : '1']
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Light1'
    if ((decoded[2] & 3) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Light2'
    if ((decoded[2] & 192) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Blower'
    if ((decoded[3] & 1) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Aux1'
    if ((decoded[4] & 1) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Aux2'
    if ((decoded[4] & 2) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)
    accessory =  'Mister'
    if ((decoded[4] & 16) != 0) {
        pumpConfiguration[accessory] = ['installed' : true ]
        makeEvent(accessory,'Installed')
    } else device.deleteCurrentState(accessory)

    logDebug "==> pumpConfiguration Map= ${pumpConfiguration}"
    state.spaConfiguration = pumpConfiguration
    logDebug "ParseDeviceConfigurationData() state.spaConfiguration= ${state.spaConfiguration}"
}

void makeEvent(name, value, units=null, description=null) {
    def dataMap = ['name': name, 'value': value]
    if (units) 			dataMap['units'] = units
    if (description) 	dataMap['description']= description
	sendEvent(dataMap)
	logDebug "<span style='color:green'><b>sendEvent</b> ${name} = ${value}${units?:''} ${description?:''}</span>"
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
    logErr "Socket status message received: ${socketMessage}"
}

// Wrapper for socket_connect
def connect() {
    logInfo "Attempting to connect to spa..."
    return socket_connect()
}

boolean socket_connect() {
    // Check contact status for socket_status
    if (device.currentValue("contact")=='open') {
        logInfo "Socket is already open"
        return true
    }

    logDebug "Socket Connect(): ${settings.ipaddress} at port: 4257 for a delay reads of ${SOCKET_CONNECT_READ_DELAY} millisecs"
	boolean rc = false //default
    // Reset device message received counter to 0
    state.messageCount = 0

    // Dead man fail safe in case we cannot close the socket via number of messages
    logDebug "Reset auto closing device socket timer to ${READ_WAIT_SECS} secs"
    runIn(READ_WAIT_SECS,'socket_close')

	try {
        logInfo "Attempting to connect to Spa device..."
		interfaces.rawSocket.connect(settings.ipaddress, 4257, byteInterface: true, readDelay: SOCKET_CONNECT_READ_DELAY)
        rc = true
	} catch (java.net.NoRouteToHostException ex) {
		logErr "Error: No Route To Host - Can't connect to spa, make sure spa is on the network.  Exiting.."
		rc = false
	} catch (java.net.SocketTimeoutException ex) {
		logErr "Error: Socket Timeout - Can't connect to spa's socket, make sure spa is online and accepting network connections.  Exiting.."
		rc = false
	} catch (e) {
		logErr "Error: $e"
		rc = false
    }
    logDebug "Socket_Connect rc=${rc}"
    makeEvent('contact', (rc)?'open':'closed')
    makeEvent('presence', (rc)?'present':'not present')
    makeEvent('network', (rc)?'online':'offline')
    return rc
}

// Wrapper for socket_write
def send(message) {
    logInfo "Sending command to Spa device..."
    return socket_write(message)
}

def socket_write(message) {
    logDebug  "Socket: write - ${settings.ipaddress}:4257 → message:  ${message}"
    boolean rc = false //default
    if (message==null || message == '') {
        logErr "socket_write(${message}): message argument is either null or empty, exiting..."
        return false
    }
    // Open socket if closed
    if (device.currentValue("contact")=='closed') socket_connect()

    logDebug "Reset auto closing socket timer to ${READ_WAIT_SECS} secs to send '${message}' to device"
    runIn(READ_WAIT_SECS,'socket_close')

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
    state.messageCount = 0
    return rc
}