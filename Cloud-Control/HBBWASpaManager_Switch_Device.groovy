/*
 *  Hubitat BWA Spa Manager
 *  -> Switch Device Driver
 *
 *  Copyright 2023, 2024 Kurt Sannders
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
 */
#include kurtsanders.SanderSoft-Library
#include kurtsanders.BWA-Library

import groovy.transform.Field

@Field static String SWITCH_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Switch"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Parent"
@Field static final String VERSION = "2.0.1"
@Field static final List SUPPORTED_SWITCH_MODES = ["off", "on"]
@Field static final List SUPPORTED_ACCELERATION_MODES = ["inactive", "active"]

metadata {
    definition (name: SWITCH_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Refresh"
        capability "Switch"
        capability "AccelerationSensor"
        capability "Momentary"
        capability "Actuator"

        attribute "acceleration", "enum", SUPPORTED_ACCELERATION_MODES
        attribute "switch", "enum", SUPPORTED_SWITCH_MODES
        attribute "balboaAPIButtonNumber", "number"

        command "push"   , [[name:"Push Button", description:"Push Button"]]
    }
}

void parse(state) {
    logTrace "Switch Device parse(${state})"
    switch (state) {
        case 'off':
        case 'on':
        logTrace "switch: ${state}"
        sendEvent(name: "switch", value: state)
        break
        // Pump 0 is a Circ Pump and only a readonly attribute as acceleration
        case 'active':
        case 'inactive':
        logTrace "acceleration: ${state}"
        sendEvent(name:'acceleration', value: state)
        break
        default:
            logErr "Invalid state ${state} sent, Ignored"
        break
    }
}

void installed() {
    setLogLevel("Info", "30 Minutes")
    logInfo "New Install: Inital logging level set at 'Info' for 30 Minutes"
    sendEvent(name: "switch", value: "off")
}

def updated() {
	logInfo "Preferences Updated..."
	checkLogLevel()
}

void setBalboaAPIButtonNumber(balboaAPIButtonNumber) {
    sendEvent(name: "balboaAPIButtonNumber", value: balboaAPIButtonNumber)
}

void on() {
    push()
}

void off() {
    push()
}

void push() {
    def balboaAPIButtonNumber = device.currentValue("balboaAPIButtonNumber")
    if (balboaAPIButtonNumber) {
        if (device.currentValue("switch", true) != state) {
            logTrace "Sending Button, ${balboaAPIButtonNumber} to parent"
            parent?.sendCommand("Button", balboaAPIButtonNumber)
        }
    } else sendEvent(name: "errorMsg", value: "Error: ${device.displayName} is not user controllable by switches")

}

def refresh() {
    parent?.refresh()
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
