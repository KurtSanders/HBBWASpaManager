/*
 *  Hubitat BWA Spa Manager - Local
 *  -> App
 *
 *  Copyright 2025 Kurt Sanders
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

@Field static String PARENT_DEVICE_NAME            = "BWA Spa Manager App - Local"
@Field static final String VERSION                 = "2.0.0"
@Field static final String PARENT_TYPENAME         = "Balboa Hot Tub Local Driver"


definition(
    name              : PARENT_DEVICE_NAME,
    namespace         : NAMESPACE,
    author            : AUTHOR_NAME,
    description       : "Provide local TCP access to a BWA Spa with a Spa WiFi Module.",
    category          : "",
    iconUrl           : "",
    iconX2Url         : "",
    installOnOpen	  : true,
    documentationLink : COMM_LINK,
    singleInstance    : true
) {
}

preferences {
    page(name: "mainPage")
}

def installed() {
    // Set logging to 'Debug for 30 minutes' during initial setup of the app
    setLogLevel("Debug", "30 Minutes")    
    logInfo "Setting initial '${app.name}' level logging to 'Debug' for 30 Minutes..."
    
    // Create state variables for parent device driver
    state.deviceNetworkID 	= "${app.id}-bwaSpaManagerLocal"
    state.deviceName 		= PARENT_DEVICE_NAME
    app.updateSetting("spaDriverName",[value: state.deviceName, type:"string"])
    
	// Create Parent Device Driver
    def rc = createDataChildDevice(NAMESPACE, PARENT_TYPENAME, state.deviceNetworkID)
    if (rc) logInfo "Parent Spa Driver: Created ${state.deviceNetworkID}"
    else logError "Parent Spa Driver Error (rc=${rc}): Parent Spa Device has NOT been created..."
}

def updated() {
    checkLogLevel()
    def d = getChildDevice(state.deviceNetworkID)
    if (!d) {
        logWarn "Parent Spa device '${state.deviceNetworkID}' not found during updated()"
        return
    }
    if (spaDriverName && state.deviceName != spaDriverName) {
        logInfo "Changing Spa Parent Driver label to ${spaDriverName}"
        d.label = spaDriverName
        state.deviceName = spaDriverName
    }
    if (settings.ipAddress && isValidIPv4(settings.ipAddress)) {
        if (state.lastSyncedIp != settings.ipAddress) {
            logInfo "Updating ${d.label} ipaddress to ${settings.ipAddress}"
            d.updateSetting("ipaddress", [value: "${settings.ipAddress}", type: "text"])
            state.lastSyncedIp = settings.ipAddress
        }
    }
    if (settings.port) {
        d.updateSetting("port", [value: "${settings.port}", type: "text"])
    }
    if (settings.pollInterval) {
        d.updateSetting("poll_interval", [value: "${settings.pollInterval}", type: "enum"])
    }
    d.updateSetting("createMomentarySwitches", [value: (settings.createMomentarySwitches != false), type: "bool"])
    d.updated()
    if (settings.ipAddress && isValidIPv4(settings.ipAddress)) {
        d.isSpaConfigOK(true)
    }
}

private boolean isValidIPv4(String ip) {
    return (ip ==~ /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/)
}

def mainPage() {
    def parentDevice = getChildDevice(state.deviceNetworkID)

    dynamicPage(name: "mainPage", uninstall: true, install: getChildDevices() ? true : false) {
        // Community Help Link
        section() {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community Support <u>WebLink</u> to ${app.name}")
            paragraph("")
        }
        section(sectionHeader("BWA Spa Manager - Local TCP Access")) {
            input name: "ipAddress", type: "string", title: fmtTitle("Local IP address (eg. x.x.x.x) of the Spa"), submitOnChange: true, required: true
            if (settings.ipAddress && !isValidIPv4(settings.ipAddress)) {
                paragraph "<span style='color:red'><b>Invalid IPv4 address:</b> '${settings.ipAddress}'. Please enter a valid IP address (e.g. 192.168.1.50).</span>"
            }
            input name: "port", type: "string", title: fmtTitle("Local TCP Port (Default 4257)"), defaultValue: "4257", required: true
            input name: "pollInterval", type: "enum", title: fmtTitle("Auto Polling Interval"), defaultValue: "runEvery10Minutes", options: POLLING_OPTIONS, required: true
            input name: "createMomentarySwitches", type: "bool", title: fmtTitle("Create Momentary Control Child Switches"), defaultValue: true, required: false

            if (parentDevice) {
                paragraph "The <a target='_blank' rel='noopener noreferrer' href='http://${location.hub.localIP}/device/edit/${parentDevice.id}'>${parentDevice}</a> parent device has been installed for you.\nYou can control all your BWA Spa accessories using this parent device."
                paragraph "The following ${getFormat('text-red', 'child virtual switches')} under the '${spaDriverName}' device have been created for controlling your BWA Spa accessories from your Hubitat Dashboard, Rules or WebCore."
                parentDevice.getChildDevices()?.each {
                    paragraph "→ <a target='_blank' rel='noopener noreferrer' href='http://${location.hub.localIP}/device/edit/${it.id}'>${it}</a>"
                }
                if (settings.ipAddress && isValidIPv4(settings.ipAddress) && state.lastSyncedIp != settings.ipAddress) {
                    logInfo "Updating ${parentDevice.label} ipaddress to ${settings.ipAddress}"
                    parentDevice.updateSetting("ipaddress", [value: "${settings.ipAddress}", type: "text"])
                    state.lastSyncedIp = settings.ipAddress
                    logInfo "Checking for a valid spa configuration at ${settings.ipAddress}"
                    parentDevice.isSpaConfigOK(true)
                }
            } else {
                paragraph "Parent Spa device is not yet installed. Click Done to complete setup."
            }
        }

        // Spa Parent Driver Options
        section(sectionHeader("BWA Spa Parent Driver Rename")) {
            input name: "spaDriverName", title: fmtTitle("Optional: Enter a new name for the parent spa device."), type: "string", required: true, multiple: false, defaultValue: app.name
        }

        // Logging Options
        section(sectionHeader("Logging Options")) {
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, submitOnChange: true, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                description: fmtDesc("Time to enable Debug/Trace logging"), defaultValue: 10, options: LOG_TIMES
        }
    }
}