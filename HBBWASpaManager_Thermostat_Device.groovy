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
*
 */

import groovy.transform.Field
import groovy.time.TimeCategory

@Field static String NAMESPACE = "kurtsanders"

@Field static String THERMOSTAT_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Thermostat"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Thermostat"
@Field static final String VERSION = "1.2.0"
@Field static final List THERMO_STAT_MODES = ["heat","off"]
@Field static final List THERMO_STAT_OPERATING_STATE = ["heating", "idle", "pending heat"]
@Field static final List THERMO_STAT_FAN_MODES = ["off", "circulate"]


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
    log.debug "==> setThermostatMode(${mode})"
    sendEvent([name: "thermostatMode", value: mode])
}

void setCoolingSetpoint(setpoint) {
    log.debug "==> setCoolingSetpoint(setpoint)= ${setpoint}"
    sendEvent(name: "coolingSetpoint", value: setpoint)
    parent?.sendCommand("SetTemp", device.currentValue("temperatureScale") == "C" ? setpoint * 2 : setpoint)
}

void setHeatingSetpoint(setpoint) {
    log.debug "==> setHeatingSetpoint(setpoint)= ${setpoint}"
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
    log.info "Emergency Heat Requested"
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
    log.info "The '${functionnName}' device command is not applicable to the Spa Thermostat mode"
}

