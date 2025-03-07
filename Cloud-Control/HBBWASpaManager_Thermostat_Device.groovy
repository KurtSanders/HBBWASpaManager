/*
 *  Hubitat BWA Spa Manager
 *  -> Thermostat Device Driver
 *
 *  Copyright 2023-2024 Kurt Sannders
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
 */
#include kurtsanders.SanderSoft-Library
#include kurtsanders.BWA-Library

import groovy.transform.Field

@Field static String THERMOSTAT_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Thermostat"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static final String VERSION = "2.0.1"
@Field static final List THERMO_STAT_MODES = ["heat","Off"]
@Field static final List THERMO_STAT_OPERATING_STATE = ["heating", "idle", "pending heat"]
@Field static final List THERMO_STAT_FAN_MODES = ["off", "circulate"]

metadata {
    definition (name: THERMOSTAT_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
//        capability "Thermostat"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
//        capability "ThermostatMode"
//        capability "ThermostatOperatingState"

        attribute "supportedThermostatFanModes", 'JSON_OBJECT'
        attribute "supportedThermostatModes", 'JSON_OBJECT'
        attribute "thermostatFanMode", "enum", THERMO_STAT_FAN_MODES
        attribute "thermostatOperatingState", "enum",  THERMO_STAT_OPERATING_STATE
        attribute "thermostatMode", "enum", THERMO_STAT_MODES

        command "setHeatingSetpoint",    [[name:'Heating Setpoint* 55-104°F', type:'NUMBER', description:'Heating setpoint temperature from 55°F-104°F', range: "55..104"]]
//      command "setCoolingSetpoint",    [[name:'Cooling Setpoint*', type:'NUMBER', description:'Cooling setpoint temperature']]
//		command "setThermostatFanMode",	 [[name: 'Fan Mode*', type: 'ENUM', constraints: THERMO_STAT_FAN_MODES]]
//		command "setThermostatMode",     [[name: 'Thermostat Mode*', type: 'ENUM', constraints: THERMO_STAT_MODES]]
//      command "getTemperatureRange"

    }
}
void initialize() {
    installed()
}

void updated() {
	logInfo "Preferences Updated..."
	checkLogLevel()
}

void installed() {
    setLogLevel("Info", "30 Minutes")
    logInfo "New Install: Inital logging level set at 'Info' for 30 Minutes"
    logInfo "Setting Spa Thermo defaults..."
    sendEvent(name: "switch", value: "off")
    String stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_MODES).toString()
    sendEvent(name: "supportedThermostatModes", value: stmJSON, displayed: false, isStateChange: true)
    stmJSON = new groovy.json.JsonBuilder(THERMO_STAT_FAN_MODES).toString()
    sendEvent(name: "supportedThermostatFanModes", value: stmJSON, displayed: false, isStateChange: true)
    sendEvent(name: "thermostatFanMode", value: "circulate")
    sendEvent(name: "thermostatMode", value: "heat")
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
    logTrace "==> setThermostatMode(${mode})"
    sendEvent([name: "thermostatMode", value: "heat"])
//    sendEvent([name: "thermostatMode", value: mode])
}

void setCoolingSetpoint(setpoint) {
    notImplemennted("setCoolingSetpoint(setpoint)= ${setpoint}")
}

void setHeatingSetpoint(setpoint) {
    logTrace "==> setHeatingSetpoint(), waiting 5 secs for more input to updateThermostatSetpoint to ${setpoint}..."
    runIn(5, "updateThermostatSetpoints", [overwrite: true, data: setpoint])
}

def updateThermostatSetpoints(setpoint) {
    logInfo "==>updateThermostatSetpoints(${setpoint})"
    sendEvent(name: "heatingSetpoint", value: setpoint)
    parent?.sendCommand("SetTemp", device.currentValue("temperatureScale") == "C" ? setpoint * 2 : setpoint)
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
    notImplemennted("Emergency Heat Requested")
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