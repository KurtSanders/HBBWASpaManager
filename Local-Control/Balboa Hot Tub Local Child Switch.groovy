/*
 *  Balboa Hot Tub Local Child Switch
 *  -> Switch Device Driver
 *
 *  Copyright 2023, 2024, 2025 Kurt Sannders
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
#include kurtsanders.Balboa-Hot-Tub-API-Library

import groovy.transform.Field

@Field static String SWITCH_CHILD_DEVICE_NAME_PREFIX = "Balboa Hot Tub Local Child Switch"
@Field static String PARENT_DEVICE_NAME = "Balboa Hot Tub Local Driver"
@Field static final String VERSION = "2.0.0"

metadata {
    definition (name: SWITCH_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Refresh"
        capability "Switch"
        capability "Actuator"
        attribute "apiNumber", "number"
    }
}

void installed() {
    setLogLevel("Debug", "30 Minutes")
    logInfo "New Install: Inital logging level set at 'Debug' for 30 Minutes"
    sendEvent(name: "switch", value: "off")
}

def updated() {
	logInfo "Preferences Updated..."
	checkLogLevel()
}

void setApiMap(Map apiMap) {
	state.apiMap = apiMap
}

void on() {
    if (state.apiMap) {
        parent?.childRunApi(device, state.apiMap)
    } else logErr "Error missing required state variable apiMap '${state.apiMap}' to send to parent device"
    runIn(1, 'off')
}

void off() {
    sendEvent(name: "switch", value: 'off')
}

def refresh() {
    if (logEnable) log.debug "Refresh called from child, requesting update from parent"
    parent.refresh()
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
