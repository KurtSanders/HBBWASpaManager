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
 *  CHANGE HISTORY
 *  VERSION     DATE            NOTES
 *  0.0.1       2020-10-11      First release
 *  1.2.0       2023-11-11      V1.2.0 code stream maintained by Kurt Sanders
 *                              Moved hardcoded logging in driver to UI preferences and added expire timeout logic.
 *                              Added driver to HPM Public Install App
 *
 */

import groovy.transform.Field


@Field static String NAMESPACE = "kurtsanders"

@Field static String SWITCH_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Switch"

@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Switch"
@Field static final String VERSION = "1.2.0"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/balboa-spa-controller-app/18194/45"


metadata {
    definition (name: SWITCH_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Switch"
        capability "Refresh"

        attribute "switch", "enum", ["on", "off"]
        attribute "balboaAPIButtonNumber", "number"

        command "on"
        command "off"
//        command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
//                                [name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]

    }
}

void parse(input) {
    logDebug ("Switch input: '${input}'")
    switch (input) {
        case "on":
        case "true":
            sendEvent(name: "switch", value: "on")
            break;
        case "off":
        case "false":
            sendEvent(name: "switch", value: "off")
            break;
    }

}

void installed() {
}

void setBalboaAPIButtonNumber(balboaAPIButtonNumber) {
    sendEvent(name: "balboaAPIButtonNumber", value: balboaAPIButtonNumber)
}

void on() {
    if (device.currentValue("switch", true) != "on")
    {
        sendEvent(name: "switch", value: "on")
        parent?.sendCommand("Button", device.currentValue("balboaAPIButtonNumber"))
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
    logDebug ("Refresh requested")
    parent?.refresh()
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
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
*/

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:"3", type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"30", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
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
	Integer level = settings.logLevel as Integer ?: 3
	Integer time = settings.logLevelTime as Integer ?: 0
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	logWarn "Debug logging toggle disabled..."
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		device.updateSetting("logLevel",[value:"2", type:"enum"])
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${device.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=1) log.warn "${device.displayName}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=2) log.info "${device.displayName}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=3) log.debug "${device.displayName}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=4) log.trace "${device.displayName}: ${msg}"
}