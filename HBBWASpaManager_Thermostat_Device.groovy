/*
 *  Hubitat BWA Spa Manager
 *  -> Thermostat Device Driver
 *
 *  Copyright 2023 Kurt Sannders
 *   based on work Copyright 2020 Richard Powell that he did for Hubitat
 *
 *  Copyright 2020 Richard Powell
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
 *  0.0.1       2020-10-11      First release
 *  1.2.0       2023-11-11      V1.2.0 code stream maintained by Kurt Sanders
 *                              Moved hardcoded logging in driver to UI preferences and added expire timeout logic.
 *                              Added driver to HPM Public Install App
 *  1.2.2       2023-12-06      Log Enhancements
 *  1.2.4       2023-12-07      Bugfix Log displaying device name in logging functions
 *
 */

import groovy.transform.Field
import groovy.time.TimeCategory

@Field static String NAMESPACE = "kurtsanders"

@Field static String THERMOSTAT_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Thermostat"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static final String VERSION = "1.2.4"
@Field static final List THERMO_STAT_MODES = ["heat","off"]
@Field static final List THERMO_STAT_OPERATING_STATE = ["heating", "idle", "pending heat"]
@Field static final List THERMO_STAT_FAN_MODES = ["off", "circulate"]
@Field static final String COMM_LINK = "https://community.hubitat.com/t/release-hb-bwa-spamanager/128842"


metadata {
    definition (name: THERMOSTAT_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Thermostat"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
        capability "ThermostatMode"
        capability "ThermostatOperatingState"


        attribute "supportedThermostatFanModes", 'JSON_OBJECT'
        attribute "supportedThermostatModes", 'JSON_OBJECT'
        attribute "thermostatFanMode", "enum", THERMO_STAT_FAN_MODES
        attribute "thermostatOperatingState", "enum",  THERMO_STAT_OPERATING_STATE
        attribute "thermostatMode", "enum", THERMO_STAT_MODES

        command "setHeatingSetpoint",    [[name:'Heating Setpoint*', type:'NUMBER', description:'Heating setpoint temperature']]
        command "setCoolingSetpoint",    [[name:'Cooling Setpoint*', type:'NUMBER', description:'Cooling setpoint temperature']]
		command "setThermostatFanMode",	 [[name: 'Fan Mode*', type: 'ENUM', constraints: THERMO_STAT_FAN_MODES]]
		command "setThermostatMode",     [[name: 'Thermostat Mode*', type: 'ENUM', constraints: THERMO_STAT_MODES]]
        command "getTemperatureRange"

        preferences {
            input "defaultOnTemperature", "number", title: "Default Temperature When Turned On", range: getTemperatureRange()
        }
    }
}
void initialize() {
    installed()
}

void installed() {
    String stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_MODES).toString()
    sendEvent(name: "supportedThermostatModes", value: stmJSON, displayed: false, isStateChange: true)
    stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_FAN_MODES).toString()
    sendEvent(name: "supportedThermostatFanModes", value: stmJSON, displayed: false, isStateChange: true)
    sendEvent(name: "thermostatFanMode", value: "circulate")
}

void sendEvents(List<Map> events) {
    events.each {
        sendEvent(name: it.name, value: it.value)
    }
}

void sendEventsWithStateChange(List<Map> events) {
    events.each {
        sendEvent(name: it.name, value: it.value, isStateChange: true)
    }
}

void sendEventsWithUnits(List<Map> events) {
    events.each {
        sendEvent(name: it.name, value: it.value, unit: it.unit)
    }
}

void auto() {
    notImplemennted("auto()")
}

void setThermostatMode(mode) {
    logDebug "==> setThermostatMode(${mode})"
    sendEvent([name: "thermostatMode", value: mode])
}

void setCoolingSetpoint(setpoint) {
    logDebug "==> setCoolingSetpoint(setpoint)= ${setpoint}"
    sendEvent(name: "coolingSetpoint", value: setpoint)
    parent?.sendCommand("SetTemp", device.currentValue("temperatureScale") == "C" ? setpoint * 2 : setpoint)
}

void setHeatingSetpoint(setpoint) {
    logDebug "==> setHeatingSetpoint(setpoint)= ${setpoint}"
    sendEvent(name: "heatingSetpoint", value: setpoint)
    parent?.sendCommand("SetTemp", device.currentValue("temperatureScale") == "C" ? setpoint * 2 : setpoint)
}

def getTemperatureRange() {
    return "(26.5..104)"
}

def refresh() {
    parent?.refresh()
}
void fanOn(){
    notImplemennted("fanOn")
}

void off() {
    notImplemennted("off")
}

void emergencyHeat() {
    logInfo "Emergency Heat Requested"
    setThermostatMode("heat")
}

void fanCirculate() {
    notImplemennted("fanCirculate")
}

void cool() {
    notImplemennted("cool")
}

void heat() {
    setThermostatMode("heat")
}

void setThermostatFanMode(fanmode){
    notImplemennted("setThermostatFanMode(${fanmode})")
}

void fanAuto() {
    notImplemennted("fanAuto")
}

void notImplemennted(functionnName) {
    logInfo "The '${functionnName}' device command is not applicable to the Spa Thermostat mode"
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
********************A************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Off", 1:"Error", 2:"Warn", 3:"Info", 4:"Debug", 5:"Trace"]
@Field static final Map LOG_TIMES  = [0:"Indefinitely", 01:"01 Minute", 05:"05 Minutes", 15:"15 Minutes", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]
@Field static final String LOG_DEFAULT_LEVEL = 0

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:LOG_DEFAULT_LEVEL, type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"0", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 1 && levelInfo.time > 0) {
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
	Integer level = settings.logLevel as Integer ?: 0
	Integer time = settings.logLevelTime as Integer ?: 0
	return [level: level, time: time]
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 1) {
		device.updateSetting("logLevel",[value:"0", type:"enum"])
	}
}

//Logging Functions
void logErr(String msg) {
	if (logLevelInfo.level>=1) log.error "${device.name}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=2) log.warn "${device.name}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=3) log.info "${device.name}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=4) log.debug "${device.name}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=5) log.trace "${device.name}: ${msg}"
}

