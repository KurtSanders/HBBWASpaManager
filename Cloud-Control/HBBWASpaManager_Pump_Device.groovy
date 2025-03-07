/*
 *  Hubitat BWA Spa Manager
 *  -> Spa Pump Device Driver
 *
 *  Copyright 2023/2024 Kurt Sannders
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

@Field static String PUMP_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Pump"
@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Parent"
@Field static final String VERSION = "2.0.1"
@Field static final List SUPPORTED_PUMP_SPEED_MODES = ["off", "low", "high"]
@Field static final List SUPPORTED_SWITCH_MODES = ["off", "on"]

metadata {
    definition (name: PUMP_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Refresh"
        capability "FanControl"
        capability "Momentary"
        capability "Actuator"

        attribute "balboaAPIButtonNumber", "number"
        attribute "speed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "setSpeed", "enum", SUPPORTED_PUMP_SPEED_MODES
        attribute "supportedFanSpeeds", 'JSON_OBJECT'
        attribute "switch", "enum", SUPPORTED_SWITCH_MODES
    }
    command "setSpeed", [[name:"Pump speed*", type:"ENUM", description:"Pump speed to set", constraints:SUPPORTED_PUMP_SPEED_MODES]]
}


void parse(input) {
    logTrace "Parsed pump state from poll/refresh is: ${input}"
    switch (input) {
        case "low":
        case "high":
        case "off":
        sendEvent(name: "setSpeed", value: input)
        sendEvent(name: "speed"   , value: input)
        sendEvent(name: "switch"  , value: (input=="off")?input:"on")
            break;
        default:
            logWarn "Invalid pump speed name: '${input}' passed to parse() function. Ignoring..."
        break
    }
}

void setsupportedFanSpeeds() {
    def modes = new groovy.json.JsonBuilder(SUPPORTED_PUMP_SPEED_MODES)
    sendEvent(name: "supportedFanSpeeds", value: modes, type:"digital", isStateChange: true, descriptionText:"Supported pump speeds initialized to ${SUPPORTED_PUMP_SPEED_MODES}")
}

void installed() {
    setLogLevel("Info", "30 Minutes")
    logInfo "New Install: Inital logging level set at 'Info' for 30 Minutes"
    logInfo "Setting Spa Thermo defaults..."
    sendEvent(name: "speed", value: "Panel Refresh Needed")
    setsupportedFanSpeeds()
}

def updated() {
	logInfo "Device preferences updated..."
    setsupportedFanSpeeds()
    setBalboaAPIButtonNumber(4)
	checkLogLevel()
}

void setBalboaAPIButtonNumber(balboaAPIButtonNumber) {
    sendEvent(name: "balboaAPIButtonNumber", value: balboaAPIButtonNumber)
}

def pumpSpeedListIndex(speedName=null) {
    logTrace "pumpSpeedListIndex(speedName)"
    List supportedFanSpeeds = SUPPORTED_PUMP_SPEED_MODES
    if (!speedName) {
        speedName = device.currentValue("speed")
        logTrace "speedName was null: speedName reset to: ${speedName}"
    }
    def speedListIndex = supportedFanSpeeds.findIndexOf { it == speedName }
    logTrace "speedListIndex(${speedName})= ${speedListIndex}"
    return speedListIndex
}

void on() {
    push()
}

void off() {
    push()
}

void push() {
    cycleSpeed()
}

void cycleSpeed() {
    logTrace "cycleSpeed()"
    List supportedFanSpeeds = SUPPORTED_PUMP_SPEED_MODES
    def currentSpeedIndex = pumpSpeedListIndex()
    def nextSpeedIndex = (currentSpeedIndex+1 > supportedFanSpeeds.size()-1)?0:currentSpeedIndex+1
    def nextSpeed = supportedFanSpeeds[nextSpeedIndex]
    logTrace "cycleSpeed() += setSpeed(${nextSpeed})"
    setSpeed(nextSpeed)
}

void setSpeed(speedName) {
    logTrace "setSpeed(${speedName})"
    if (parent.currentValue('online') != "Online") {
        logErr "Spa is 'Offline', Ignoring setSpeed request for '${speedName}'.  Try a COMMAND refresh to check online status"
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

    sendEvent(name: "setSpeed", value: speedName)
    sendEvent(name: "speed"   , value: speedName)
    sendEvent(name: "switch"  , value: (speedName=="off")?speedName:"on")
    logTrace "parent?.sendCommand('Button',${device.currentValue('balboaAPIButtonNumber')},${buttonPresses})"
    parent?.sendCommand("Button", device.currentValue("balboaAPIButtonNumber"),buttonPresses)
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
