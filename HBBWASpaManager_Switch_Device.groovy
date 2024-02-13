/*
 *  Hubitat BWA Spa Manager
 *  -> Switch Device Driver
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
 */
#include kurtsanders.SanderSoft-Library
#include kurtsanders.BWA-Library

import groovy.transform.Field

@Field static String SWITCH_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Switch"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Parent"
@Field static final String VERSION = "1.3.1"

metadata {
    definition (name: SWITCH_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Switch"
        capability "Refresh"

        attribute "switch", "enum", ["on", "off"]
        attribute "balboaAPIButtonNumber", "number"
        attribute "speed", "enum", ["off","low","high","unknown"]

        command "on"
        command "off"
    }
    preferences() {
        section(""){
            input "buttonPresses", "number", title: "# of Spa Button Presses when Switch state is turned 'On' (1..5)", required: true, defaultValue: 1, range: "1..5"
        }
    }

}

void parse(input) {
    logInfo "Switch/speed: ${input}"

    switch (input) {
        case "low":
        case "high":
            sendEvent(name: "speed", value: input)
        case "on":
        case "true":
            sendEvent(name: "switch", value: "on")
            break;
        case "off":
        case "false":
            sendEvent(name: "switch", value: "off")
            sendEvent(name: "speed", value: "off")
            break;
        default:
            sendEvent(name: "speed", value: "unknown")
    }
}

void installed() {
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "speed", value: "unknown")
}

def updated() {
	logInfo "Preferences Updated..."
	checkLogLevel()
}

void setBalboaAPIButtonNumber(balboaAPIButtonNumber) {
    sendEvent(name: "balboaAPIButtonNumber", value: balboaAPIButtonNumber)
}

void on() {
    if (device.currentValue("switch", true) != "on") {
        sendEvent(name: "switch", value: "on")
        for(int i = 0;i<buttonPresses;i++) {
            parent?.sendCommand("Button", device.currentValue("balboaAPIButtonNumber"))
        }
    }
}

void off() {
    if (device.currentValue("switch", true) != "off")
    {
        sendEvent(name: "switch", value: "off")
        parent?.sendCommand("Button", device.currentValue("balboaAPIButtonNumber"))
    }
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
