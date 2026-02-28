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
    if (state.deviceName != spaDriverName) {
	    log.info "Changing Spa Parent Driver label to ${spaDriverName}"
    	getChildDevice(state.deviceNetworkID).label = spaDriverName
        state.deviceName = spaDriverName
    }
}

def mainPage() {

    dynamicPage(name: "mainPage", uninstall: true, install: getChildDevices()?true:false ) {
        //Community Help Link
        section () {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community Support <u>WebLink</u> to ${app.name}")
            paragraph ("")
        }
        section(sectionHeader("BWA Spa Manager - Local TCP Access")) {
            input name: "ipAddress", type: "string", title: fmtTitle("Local IP address (eg. x.x.x.x) of the Spa"), submitOnChange: true, required: true
            def deviceID = getChildDevice(state.deviceNetworkID).id
            paragraph "The <a target='_blank' rel='noopener noreferrer' href='http://${location.hub.localIP}/device/edit/${getChildDevice(state.deviceNetworkID).id}'>${getChildDevice(state.deviceNetworkID)}</a> parent device has been installed for you.\nYou can control all your BWA Spa accessories using this parent device."
            paragraph "The following ${getFormat('text-red','child virtual switches')} under the '${spaDriverName}' device have been created for controlling your BWA Spa accessories from your Hubitat Dashboard, Rules or WebCore."
            getChildDevice(state.deviceNetworkID).getChildDevices().each {
                paragraph "→ <a target='_blank' rel='noopener noreferrer' href='http://${location.hub.localIP}/device/edit/${it.id}'>${it}</a>"
            }
            if (settings.ipAddress) {
                def d = getChildDevice(state.deviceNetworkID)
                logInfo "Updating ${d.label} ipaddress to ${ipAddress}"
                d.updateSetting("ipaddress",[value:"${ipAddress}",type:"enum"])
                logInfo "Checking for a valid spa configuration at ${ipAddress}"
                d.isSpaConfigOK(true)
            }
        }

        //Spa Parent Driver Options
		section(sectionHeader("BWA Spa Parent Driver Rename")) {
        	input name:"spaDriverName", title: fmtTitle("Optional: Enter a new name for the parent spa device."), type: "string", required: true, multiple: false, defaultValue: app.name
        }
        
        //Logging Options
        section(sectionHeader("Logging Options")) {
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, submitOnChange: true, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                description: fmtDesc("Time to enable Debug/Trace logging"), defaultValue: 10, options: LOG_TIMES
        }
    }
}