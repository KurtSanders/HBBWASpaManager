/*
 *  Hubitat BWA Spa Manager
 *  -> App
 *
 *  Copyright 2023, 2024 Kurt Sanders
 *   based on work Copyright 2020 Richard Powell that he did for Hubitat
 *
 *  Copyright 2020 Richard Powell
 *   based on work Copyright 2020 Nathan Spencer that he did for SmartThings
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

@Field static String PARENT_DEVICE_NAME            = "BWA Spa Manager"
@Field static final String VERSION                 = "2.0.2"
@Field static String BWGAPI_API_URL                = "https://bwgapi.balboawater.com/"
@Field static String PARENT_DEVICE_NAME_PREFIX     = "HB BPA SPA Parent"
@Field static final String VALID_SPA_STATUS_BYTE   = "2e"

definition(
    name              : PARENT_DEVICE_NAME,
    namespace         : NAMESPACE,
    author            : AUTHOR_NAME,
    description       : "Access and control a BWA Cloud Control Spa.",
    category          : "",
    iconUrl           : "",
    iconX2Url         : "",
    documentationLink : COMM_LINK,
    singleInstance    : false
) {
}

preferences {
    page(name: "mainPage")
    page(name: "authPage")
    page(name: "authResultPage")
    page(name: "confirmPage")
}

Boolean isInstalled() {
    return (app?.installationState=='COMPLETE')
}

void initVariableDefaults() {
    setLogLevel("Debug", "30 Minutes")
    state.refreshNumber = 0
    if (state?.isPaused==null) atomicState.isPaused=false
    // Set isPaused state to false for the first time
    if (!isInstalled()) {
        String defaultName = "${app.name}"
        if (appUserLabel==null) {
            logTrace "defaultName= ${defaultName}"
            app.updateSetting("appUserLabel",[value: defaultName, type:"text"])
        }
        if (pollingModes == null) {
            // Set all polling modes on initially
            app.updateSetting("pollingModes",[value: location.getModes().sort(), type:"mode"])
        }
        if (logLevel==null || logLevelTime == null) {
            // Set logging to 'Debug for 30 minutes' during initial setup of the app
            setLogLevel("Debug", "30 Minutes")
            logInfo "Setting initial BWA application level logging to 'Debug' for 30 Minutes..."
        }
    }
}

def deleteOldDevices() {
    def childDev = getChildDevice(state.spa["deviceId"])
    if (childDev) {
	    def count = childDev.deleteOldChildDevices("check")
    	log.debug "deleteOldDevices(): child count to delete= ${count}"
    	return count
    }
}

def verifyChildrenDevices() {
    def msg = ""
    logTrace "deviceTypeNameValidate()"
    try {
        def childDev = getChildDevice(state?.spa["deviceId"])
        if (childDev) {
            def retMap = childDev.verifyChildrenDevicesMethod(true)
            logTrace "deviceTypeNameValidate() retMap = ${retMap}"
            if (retMap) {
                msg +="<table cellspacing='1' cellpadding='5' width='auto' height='auto' border='1'><tr><th align='center'>'Device Name/Label'</th><th align='center'>'Device Driver Name'</th><th align='center'>'Driver Verification Results'</th></tr>"
                retMap.sort().each { key, value ->
                    msg += "<tr><td>${key}</td><td>${value.typeName}</td><td>${value.status}</td></tr>"
                }
                msg +="</table>"
                logTrace "msg= ${msg}"
            }
        }
    } catch (Exception e) {
        logWarn "Critical State variables are missing, Resetting BWA and you must re-authenticate"
    }
    return msg
}

def statusOffline(errorMessage="BWA Cloud Server Unreachable") {
    updateLabel("Offline")
    def d = getChildDevice(state.spa["deviceId"])
    if (d) {
		d.sendEvent(name: "online", value: "Offline")
    	d.sendEvent(name: "spaStatus", value: UNKNOWN)
    	d.sendEvent(name: "cloudStatus", value: "${errorMessage}")
    }
}

def mainPage() {
    initVariableDefaults()

    dynamicPage(name: "mainPage", uninstall: true, nextPage: 'confirmPage', install: checkSpaState() ) {
        //Community Help Link
        section () {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community Support <u>WebLink</u> to ${app.name}")
            paragraph ("")
        }
        if (isInstalled()) {
            def deviceTypeNameValidateMsg = verifyChildrenDevices()
            logTrace "deviceTypeNameValidateMsg= ${deviceTypeNameValidateMsg}"
            if (deviceTypeNameValidateMsg) {
                section (sectionHeader("BWA Child Device Driver Verification Summary")) {
                    paragraph deviceTypeNameValidateMsg
                    if (deviceTypeNameValidateMsg.contains("-OLD")) {
                        paragraph "<i>You have OLD (pre v1.3.2 release) component child devices that need to be removed for this current version of the BWA Spa Manager app." +
                            "These OLD child devices have been safely renamed so you can exit this app and select the new devices in HE Rules and Pistons." +
                            "If you have migrated all dashboard tiles and RM references from the old to the new devicees, then select <b>'Delete OLD child devices'</b> below</i>"
                        input name: "DeleteOldDevices", type: "button", title: "Delete OLD child devices", backgroundColor: "red", textColor: "white", width: 4, submitOnChange: true
                    }
                }
            }
        }
        // BWA Authentification
        section(sectionHeader("BWA Authentication")) {
            href("authPage", title: fmtTitle("Cloud Authorization"), description: fmtDesc("${state.credentialStatus ? state.credentialStatus : ""}Click to enter BWA credentials"))
        }
        if (state.spaConfiguration) {
            section(sectionHeader("${state?.spa["deviceDisplayName"]} (BWA Username: ${state?.spa.username}) ${getImage("checkMarkGreen")}")) {
                href("confirmPage", title: fmtTitle("View your spa's mechanical configuration and name parent spa control device."), description: fmtDesc(spaManifest() + "\n" + "Click to generate a new or updated spa mechanical configuration.  (<i>This is not typically needed if this configuration is accurate</i>)"))
            }
        }

        if (state.spa) {
            // BWA Cloud Polling Options
            section(sectionHeader("How frequently do you want to poll the BWA cloud for changes? (Use a lower number if you care about trying to capture and respond to \"change\" events as they happen)")) {
                // BWA Cloud Puase?resume Button Options
                logTrace "atomicState.isPaused= ${atomicState.isPaused}"
                if (!atomicState.isPaused) {
                    input name: "pauseButton", type: "button", title: "Pause Polling", backgroundColor: "Green", textColor: "white", width: 4, submitOnChange: true
                } else {
                    input name: "resumeButton", type: "button", title: "Resume Polling", backgroundColor: "Crimson", textColor: "white", width: 4, submitOnChange: true
                }
                input name:"pollingInterval", title: fmtTitle("Polling Interval (in Minutes)"), type: "enum", required: true, multiple: false, defaultValue: 5, options: ["1", "5", "10", "15", "30"]
                if (!pollingModes) paragraph getFormat('text-red', 'No allowable polling modes have been selected below.  Panel refresh will only occur during a manual refresh from child device')
                input name: "pollingModes", type: "mode", title: fmtTitle("Allowable Hubitat modes for polling the BWA cloud for panel updates.  Please unselect those location modes to save polling cycles when not needed."),required: false, offerAll: true, multiple: true, submitOnChange: true
//                input name: "Test", type: "button", title: "Test", backgroundColor: "blue", textColor: "white", width: 4, submitOnChange: true
            }

            //Logging Options
            section(sectionHeader("BWA Logging Options")) {
                input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                    description: fmtDesc("Logs selected level and above"), defaultValue: 3, submitOnChange: true, options: LOG_LEVELS
                input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                    description: fmtDesc("Time to enable Debug/Trace logging"), defaultValue: 10, options: LOG_TIMES
            }
            // Custom App Name
            section (hideable:true, hidden: isInstalled(), sectionHeader("Name this instance of ${app.name}")) {
                String defaultName = "${app.name} - ${atomicState?.spa.username}"
                if (appUserLabel==null) {
                    logTrace "defaultName= ${defaultName}"
                    app.updateSetting("appUserLabel",[value: defaultName, type:"text"])
                }
                input name:"appUserLabel", type:"text", title: fmtTitle("Assign an app name"), width: 25, required:true, defaultValue: "${defaultName}", submitOnChange: true, description: fmtDesc("${defaultName}")
                updateLabel()
            }
        }
    }
}

def appButtonHandler(String btn) {
    logDebug "buttonNamePressed= ${btn}"
    switch(btn) {
        case "pauseButton":
        atomicState.isPaused = true
        setPollingSchedule()
        updateLabel("Paused")
        break
        case "resumeButton":
        atomicState.isPaused = false
        setPollingSchedule()
        updateLabel("Resuming Polling")
        break
        case "DeleteOldDevices":
        def childDev = getChildDevice(state.spa["deviceId"])
        if (childDev) {
	        childDev.deleteOldChildDevices(true)
        }
        break
        case 'RefreshConfig':
        break
    }
}

def confirmPage() {
    logDebug "==> confirmPage()"
    if (state.token == null) {
        log.warn ("BWA User notAuthenticated() state.token == null, You must login to BWA Cloud on the Main Page of the App before proceeding")
        dynamicPage(name: "confirmPage", nextPage: 'mainPage', uninstall: false, install: false) {
            section(sectionHeader("Not Logged In!")) {
                paragraph (getFormat("text-red","Please must login. Check your credentials and try again to login to BWA Cloud."))
            }
        }
        return
    }
    def spaConfiguration
    def response = getDeviceConfiguration()
    logDebug "Http: getDeviceConfiguration()=${response}"
    if (response == "Device Not Connected") {
        // Use cached spaConfiguration from state if possible
        if (state?.spaConfiguration != null) {
            spaConfiguration = state.spaConfiguration
            log.debug "spaConfiguration cached= ${spaConfiguration}"
        } else spaConfiguration = null
    } else {
        spaConfiguration = ParseDeviceConfigurationData(response)
        log.debug "spaConfiguration online= ${spaConfiguration}"
    }
    boolean uninstallBool = (spaConfiguration == null)
    dynamicPage(name: "confirmPage", uninstall: uninstallBool, nextPage: 'mainPage', install: false) {
        if (spaConfiguration != null) {
            state.spaConfiguration = spaConfiguration
            def deviceCount = spaConfiguration.count { key, value -> value == true }
            section (sectionHeader("Name your BWA Spa Parent Device")) {
                input(name: "spaParentDeviceName", type: "text", title: '', required: true,  submitOnChange: true, defaultValue: "Spa Parent")
            }
            section (sectionHeader("Found the following ${deviceCount} devices attached to your hot tub")) {
                def index = 1
                def spaManifest = "<ul>"
                spaConfiguration.each { k, v ->
                    if (v == true) {
                        spaManifest = spaManifest + "<li style='color:green;font-size:15px'>${k}</li>"
                    }
                }
                paragraph("${spaManifest}</ul>")
            }
        } else {
            section(sectionHeader("BWA Spa Response: '${response}'")) {
                def message = "<p style=color:red><img style=float:left;width:200px;height:200px; src='https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images/hottub-offline.jpg'>"
                message += "<h4 style=color:red text-align:center;><b>${state.HttpResponseExceptionReponse?:response} at ${new Date()}</b></h4><hr>"
                message += "<br><b><i>The BWA Spa Manager cannot be installed at this time.  Your spa is disconnected/offline and NOT reporting it's physical configuration and states of it's components (ie. pumps, lights, misters, etc).  You may use the 'Remove button' below to exit this application and return to try again when your spa is back online.</b></i></p>"
                message += "<h4 style=font-weight:bold;color:blue>Your BWA Spa is not connecting/responding to the BWA cloud server after <u><font color=red size='+2'>${state.refreshNumber}</font></u> attempt${(state.refreshNumber>1)?'s':''}.</h4>"
                paragraph message
                paragraph "<h5>Please verify that your spa is online by following the suggestions below:</h5>" +
                    "<ol style=font-family:verdana;'font-size:125%;'><li>Verify your spa is connecting to the BWA cloud by installing/using the <a href='https://www.balboawatergroup.com/bwa'>BWA Spa Control mobile app.</a></li>" +
                    "<li>If your spa is not reporting to the BWA cloud, power the spa off/on, and wait a few minutes to see that it has connectivity to the BWA cloud by verifying with the BWA Spa Control App</li>" +
                    "<li>If your spa is still not reporting to the BWA cloud, you may need to reboot your WiFi router for the spa to get an ip address and connectivity.</li>" +
                    "<li>Balboa recommends that your spa be only 25-50 feet from your WiFi access point for optimal connectivity.  Walls and other layers will reduce the signal.</li>" +
                    "<li>Here is a weblink to the <a href='https://www.balboawatergroup.com/getdoc.cfm?id=1623' target='_blank'><b>BWA Setting up your Wi-Fi.pdf</b></a></li>" +
                    "<li>You can contact Balboa for Technical support at techsupport@balboawater.com</li></ol>"
                if (state.refreshNumber < 4) {
                    input name: "RefreshConfig", type: "button", title: "Poll Spa Again", backgroundColor: "red", textColor: "white", width: 10
                }

            }
        }
    }
}

def authPage() {
    dynamicPage(name: "authPage", nextPage: "authResultPage", uninstall: true, install: false) {
        section () {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community Support <u>WebLink</u> to ${app.name}")
            paragraph ("")
        }
        section(sectionHeader("BWA Credentials")) {
            input("username", "username", title: "User ID", description: "BWA User ID", required: true)
            input("password", "password", title: "Password", description: "BWA Password", required: true)
        }
    }
}

def authResultPage() {
    logDebug  "Attempting login with specified credentials..."

    doLogin()
    logDebug "authResultPage() state.loginResponse: ${state.loginResponse}"

    // Check if login was successful
    if (state.token == null) {
        logDebug ("authResultPage() state.token == null")
        dynamicPage(name: "authResultPage", nextPage: "authPage", uninstall: false, install: false) {
            section(sectionHeader("${state.loginResponse}")) {
                paragraph (getFormat("text-red",getImage("button-red") + "→ Error Logging In '${settings.username}'.  Please re-check your BWA login credentials and try again."))
            }
        }
    } else {
        dynamicPage(name: "authResultPage", nextPage: "mainPage", uninstall: false, install: false) {
            section(sectionHeader("${state.loginResponse}")) {
                paragraph ("${getImage('checkMarkGreen')}${getFormat('text-green','Please click next to continue setting up your spa.')}")
            }
        }
    }
}

boolean checkSpaState() {
    boolean installBoolean = true
    if (state.spa == null && state.token?.trim()) {
        if (getSpa() == null) installBoolean = false
    }
    if (state.spaConfiguration == null) installBoolean = false
    return installBoolean
}

def spaManifest() {
    def spaManifest = "<b style=color:green;font-size:15px>"
    if (state?.spaConfiguration) {
        def spaConfiguration = state?.spaConfiguration.sort()
        spaConfiguration.each { k, v ->
            if (v == true) {
                spaManifest += "${k}, "
            }
        }
        spaManifest = "${spaManifest[0..spaManifest.size()-3]}</b>"
    } else {
        spaManifest = "<b style=color:red;font-size:15px>Select to set your Spa configutaion [Required]</b>"
    }
    return spaManifest
}


boolean doLogin(){
    def loggedIn = false
    def resp = doCallout("POST", "/users/login", [username: username, password: password])

    switch (resp.status) {
        case 504:
            atomicState.loginResponse = "BWA Server/Gateway Timeout"
            updateLabel("Login Error - ${state.loginResponse}")
            atomicState.credentialStatus = getFormat("text-red","[Offline]")
            break
        case 403:
            atomicState.loginResponse = "Access forbidden"
            updateLabel("Login Error - ${state.loginResponse}")
            atomicState.credentialStatus = getFormat("text-red","[Disconnected]")
            break
        case 401:
            atomicState.loginResponse = resp.data.message
            updateLabel("Login Error - ${state.loginResponse}")
            atomicState.credentialStatus = getFormat("text-red","[Disconnected]")
            break
        case 200:
            logInfo ("Successfully logged in.")
            loggedIn = true
            atomicState.loginResponse = "Connected"
        	updateLabel("Successfully logged in - ${state.loginResponse}")
            atomicState.token = resp.data.token
            atomicState.credentialStatus = getFormat("text-green","[Connected]")
            atomicState.loginDate = toStDateString(new Date())
            cacheSpaData(resp.data)
            logInfo ("Done caching SPA device data...")
            break
        default:
            logDebug (resp.data)
            atomicState.loginResponse = "Login unsuccessful"
            updateLabel("Disconnected")
            atomicState.credentialStatus = getFormat("text-red","[Disconnected]")
            break
    }

    logInfo ("loggedIn: ${loggedIn}, resp.status: ${resp.status}")
    return loggedIn
}

def reAuth() {
    if (!doLogin())
        doLogin() // timeout or other issue occurred, try one more time
}

def getSpa() {
    logDebug ("Getting Spa data from Balboa API...")
    def data = doCallout("POST", "/users/login", [username: username, password: password]).data
    logDebug "getSpa() data= ${data}"
    return cacheSpaData(data)
}

def cacheSpaData(spaData) {
    // save in state so we can re-use in settings
    if (spaData?.device == null) {
        logErr "cacheSpaData(): spaData object is null = '${spaData}'"
        updateLabel("Offline")
        return state?.spa
    } else {
        logInfo ("Saving Spa data in the state cache (username: ${spaData.username}, app.id: ${app.id}, device_id: ${spaData.device?.device_id})")
        state.spa = [:]
        state.spa["appId"] = app.id;
        state.spa["username"] = spaData?.username
        state.spa["deviceId"] = spaData.device?.device_id
        state.spa["deviceNetworkId"] = [app.id, spaData.device?.device_id].join('.')
        state.spa["deviceDisplayName"] = "Spa ${username}"
    }
    return spaData.device
}

def doCallout(calloutMethod, urlPath, calloutBody) {
    doCallout(calloutMethod, urlPath, calloutBody, "json", null)
}

def doCallout(calloutMethod, urlPath, calloutBody, contentType) {
    doCallout(calloutMethod, urlPath, calloutBody, contentType, null)
}

def doCallout(calloutMethod, urlPath, calloutBody, contentType, queryParams) {
    logDebug ("\"${calloutMethod}\"-ing ${contentType} to \"${urlPath}\"")
    def response
    def content_type
    switch(contentType) {
        case "xml":
            content_type = "application/xml"
            break
        case "json":
        default:
            content_type = "application/json"
            break
    }
    def params = [
        uri: BWGAPI_API_URL,
        path: "${urlPath}",
        query: queryParams,
        timeout: 300,
        headers: [
            Authorization: state.token?.trim() ? "Bearer ${state.token as String}" : null
        ],
        requestContentType: content_type,
        body: calloutBody
    ]

    try {
        switch (calloutMethod) {
            case "GET":
            logTrace "httpGet(${params.dump()})"
            httpGet(params) { resp ->
                response = resp
            }
            break
            case "PATCH":
            params.headers["x-http-method-override"] = "PATCH"
            // NOTE: break is purposefully missing so that it falls into the next case and "POST"s
            case "POST":
            logDebug "httpPost(params → ${params})"
            logDebug "Sending the commands to httpPost(${params})"
            httpPost(params) { resp ->
                response = resp
                logDebug "doCallout(httpPost) response.status = ${response?.status}"
                logDebug "doCallout(httpPost) response.data   = ${response?.data}"
            }
            break
            default:
                logErr "unhandled method"
            return [error: "unhandled method"]
            break
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logDebug "doCallout(): Http Error (${e.response.status}): ${e.response.statusLine.reasonPhrase}"
        atomicState.HttpResponseExceptionReponse = "http rc=${e.response.status}, ${e.response.statusLine.reasonPhrase}"
        statusOffline("Http Error (${e.response.status}): ${e.response.statusLine.reasonPhrase}")
        return [status: "${e.response.status}", data: "${e.response.statusLine.reasonPhrase}"]
    } catch (e) {
        logWarn "doCallout(): Something went wrong: ${e}"
        statusOffline("${e}")
        return [error: e.message]
    }
    switch (response.status) {
        case '200':
        if (response.data == "Device Not Connected") {
            statusOffline("Device Not Connected")
            return response
        }
        if (response?.data.toString().toLowerCase().contains('command received')) {
            logInfo "doCallout(): A 'command received' response has been received from a request to change a spa device setting."
        }
        updateLabel("Online")
        logDebug "response= ${response}"
        return response
        break
        default:
            logErr" A Web response of 200 was not handled: response.data => ${response.data}"
        break
    }
    logDebug "doCallout(): Changing spa state to Offline do to response.status = ${response.status} or response.data=${response.data}"
    statusOffline("${response.status}")
    try {
        if(response.hasError()) {
            logErr "doCallout(): error message: '${response.getErrorMessage()}'"
        }
    }
    catch (e) {
        logErr "doCallout(): No spa panel update data was returned from BWA cloud API server, BWA API cloud server responded: '${deviceData}'."
    }
    return response
}

def doCalloutAsync(callbackMethod, calloutMethod, urlPath, calloutBody, contentType="json", queryParams=null) {
    logDebug "doCalloutAsync(): ${calloutMethod}-ing ${contentType} to ${urlPath}"
    logTrace "calloutBody= ${displayXML(calloutBody)}"
    contentType ="application/${contentType?:'json'}"
//    return
    def params = [
        uri: BWGAPI_API_URL,
        path: "${urlPath}",
        query: queryParams,
        timeout: 300,
        headers: [
            Authorization: state.token?.trim() ? "Bearer ${state.token as String}" : null
        ],
        requestContentType: contentType,
        body: calloutBody
    ]
    def data = [startTime: new Date()]
    switch (calloutMethod) {
        case "GET":
        asynchttpGet(callbackMethod, params, data)
        break
        case "PATCH":
        params.headers["x-http-method-override"] = "PATCH"
        // NOTE: break is purposefully missing so that it falls into the next case and "POST"s
        case "POST":
        logDebug "asynchttpPost(): ${data}"
        asynchttpPost(callbackMethod, params, data)
        break
        default:
            logErr "unhandled doCalloutAsync() method: ${calloutMethod}"
        return [error: "unhandled method"]
        break
    }
}

boolean isValidSpaMessageType(encodedData) {
    def datastring = encodedData.toString()
    if (encodedData == null) return false
    if ( datastring.startsWith('Hf+') && datastring.endsWith('=') ) {
        logTrace "Valid encoded spa data: '${encodedData}'"
        return true
    } else {
        logTrace "Invalid encoded spa data: '${encodedData}'"
        return false
    }
}

def installed() {
    setLogLevel("Debug", "30 Minutes")
    logInfo "installed()"
    atomicState.isPaused = false
    initialize()
}

def updated() {
    logInfo "updated()"
    unsubscribe()
    initialize()
}

def initialize() {
    logInfo "initialize()"
    logInfo "Subscribing to location mode events"
    subscribe(location, 'mode', 'modeEventHandler')
    // Not sure when tokens expire, but will get a new one every 24 hours just in case by scheduling to reauthorize every day
    if(state.loginDate?.trim()) schedule(parseStDate(state.loginDate), reAuth)

    // Check for spa created child devices that are now unused because of a spa configuration change.  If state.spaConfiguration is empty, skip → Something is wrong!
    logInfo "The spa has 1 parent device and ${state?.spaConfiguration.findAll {element -> element.value}.size()} child devices → '${state?.spaConfiguration.findAll {element -> element.value}.keySet().sort()}"

    // Enable to 'true' below if you want to delete the child device that is no longer valid
    if (false) {
        if (state?.spaConfiguration.findAll {element -> element.value}.size()==0 || state?.spa["deviceNetworkId"]==null) {
            log.warn "Warning state.spa["deviceNetworkId"] => ${state.spa["deviceNetworkId"]}"
            log.warn "Warning: spaConfiguration list 'state.spaConfiguration' = ${state?.spaConfiguration} is null/empty. Skipping deleting any existing child devices.  Refresh the spa configuration to detetct pumps, lights, etc in BWA Spa Manager app!"
        } else {
            def delete = getChildDevices().findAll { !state.spa["deviceNetworkId"] }
            delete.each {
                logWarn "Deleting unused/orphan spa child device = ${it.label}"
                deleteChildDevice(it.deviceNetworkId)
            }
        }
    }
    def childDevices = []
    def childDevice
    try {
        childDevice = getChildDevice(state.spa["deviceId"])
        logDebug "Parent childDevice = ${childDevice}"
        if(!childDevice) {
            logInfo ("Adding device: ${settings.spaParentDeviceName} [${state.spa["deviceId"]}]")
            childDevice = addChildDevice(NAMESPACE, PARENT_DEVICE_NAME_PREFIX, state.spa["deviceId"], [label: settings.spaParentDeviceName])
            state.spa["deviceDisplayName"] = settings.spaParentDeviceName
            childDevice.parseDeviceData(state.spa)
            childDevice.createChildDevices(state.spaConfiguration)
        }
        childDevices.add(childDevice)
        childDevice.createChildDevices(state.spaConfiguration)
    } catch (e) {
        logErr "Error creating device: ${e}"
    }
//    setPollingSchedule()
    checkLogLevel()

    logInfo "Begin child device level call to syncLogLevel to logLevel: ${logLevel}, logLevelTime: ${logLevelTime})"
    childDevice.syncLogLevel(logLevel, logLevelTime)
}

void setPollingSchedule() {
    logDebug "setPollingSchedule()"
    switch(true) {
        case (atomicState.isPaused):
        logWarn "Application Polling has been paused in app preferences..."
        updateLabel("Paused")
        unschedule(pollChildren)
        break
        case (pollingModes == null):
        logInfo "No polling modes have been selected in ${app.name} 'Allowed Polling Modes' preference. Only command 'Refresh' from spa child devices will be available to get spa panel updates"
        unschedule(pollChildren)
        break
        case (pollingModes.contains(location.mode)):
        def childDev = getChildDevice(state.spa["deviceId"])
        //lets make sure we have a parent child device
        if(childDev) {
            // set up polling only if we have child devices
            logInfo "${childDev.label} will create a polling interval of ${pollingInterval} minute${pollingInterval != "1" ? 's' : ''}"
            childDev.parseDeviceData([pollingInterval : "Every ${pollingInterval} minute${pollingInterval != "1" ? 's' : ''} when in ${pollingModes.join(', ')} modes" ])
            // Run a panel update at least once
            runIn(5, pollChildren)
            logInfo "Setting BWA polling interval to ${pollingInterval} minute${pollingInterval != "1" ? 's' : ''} in ${pollingModes.join(", ")} modes"
            "runEvery${pollingInterval}Minute${pollingInterval != "1" ? 's' : ''}"("pollChildren")
        } else {
            logErr "Missing child device, polling schedule not activated"
            return
        }
        break
        default:
            logInfo "Polling has been deactivated, current mode of '${location.mode}' not in valid list of polling modes: '${pollingModes.join(", ")}'"
        unschedule(pollChildren)
    }
}

def modeEventHandler(evt) {
    logDebug "${evt.descriptionText}"
    setPollingSchedule()
}

// New pollChildren method using asynchttp refreshes for better performance
def pollChildren(refreshOverride=false) {
    logDebug "pollChildren(refreshOverride=${refreshOverride})"
    // Check for a location mode that allowed to poll per preference settings
    if (refreshOverride || pollingModes.contains(location.mode)) {
        logInfo ("Getting spa panel latest update...")
        def dataXML = getXmlRequestFilename('PanelUpdate')
        logTrace "pollChildrenAsync(): dataXML = ${displayXML(dataXML)}"
        doCalloutAsync("pollChildrenAsyncHandler", "POST", "/devices/sci", "${dataXML}", "xml")
    } else {
        logDebug "Skipping BWA Cloud polling.. Currennt '${location.mode}' mode is not a valid polling mode(s) of '${pollingModes.join(", ")}'"
        updateLabel("Polling paused in ${location.mode} mode")
    }
}

void pollChildrenAsyncHandler(response, data) {
    def duration
    use(TimeCategory){
        // Get delta mins from spa internal clock to hub's localtime
        duration = TimeCategory.minus(new Date(), data.startTime)
        logDebug "pollChildrenAsyncHandler(): BWA Cloud getPanel API duration= <font color=green><b>${duration}</b></font>"
        try {
            def minutes = (duration.minutes==0)?'':"${duration.minutes} minutes, "
            def seconds = "${duration.seconds + ((float) duration.millis/1000).round(2)} secs"
            duration = "${minutes}${seconds}"
        } catch(Exception ex) {
            logErr "TimeCategory: ${ex}"
        }
    }
    //*********************************************************
    //     Uncomment statement below to stop run away loop
    // return
    //*********************************************************
    def childPollcount = atomicState.childPollcount?:0
    childPollcount += 1
    boolean pollRetry = true
    def childDev = getChildDevice(state.spa['deviceId'])

    switch(response.status) {
        case '200':
        logDebug("pollChildrenAsyncHandler(200): data= ${data}")
        def deviceData = response?.xml
        switch (true) {
            case (isValidSpaMessageType(deviceData)):
            logDebug "==> pollChildrenAsyncHandler(): valid XML panel state response → <b>${deviceData}</b>"
            // Normal response.status for a panel update, but the response.xml might not be a valid encoded hex value
            byte[] decoded = deviceData.decodeBase64()
            logTrace "pollChildrenAsyncHandler(): spaStatusHexArray= ${hexbytwo(decoded.encodeHex().toString())}"
            childDev.parseDeviceData(['cloudResponseTime':"${duration}"])
            logDebug "pollChildrenAsyncHandler(): sending panel state array to ${childDev.displayName}"
            childDev.parsePanelData(deviceData)
            childPollcount = 0
            atomicState.childPollcount = 0
            updateLabel("Online")
            childDev.parseDeviceData(['cloudStatus':"Panel State Updated"])
            logTrace "pollChildrenAsyncHandler(): Spa online: panel state device data returned, reset childPollcount= ${childPollcount}"
            pollRetry=false
            break
            case response.data.toString().contains('Device'):
            logErr "pollChildrenAsyncHandler(http rc=${response.status}): <font color=red><b>${response.data}</b></font>"
            childDev.parseDeviceData(['cloudStatus':"${response.data.toString()}"])
            childDev.parsePanelData(deviceData)
            break
            default:
                logErr "pollChildrenAsyncHandler(http rc=${response.status}): Unknown 200 server response: Not a known panel message type"
            childDev.parseDeviceData(['cloudStatus':'Unknown 200 server response: Not a known panel message type'])
            childDev.parsePanelData(deviceData)
            response.properties.each {
                logErr "pollChildrenAsyncHandler(http rc=${response.status}): ${it}"
            }
        }
        break
        case '504':
        logWarn "<font color=red><b>504: ${response?.errorMessage}</b></font> - Will retry poll"
        statusOffline("${response?.errorMessage} - Retrying")
        break
        default:
            response.properties.each {
                logWarn "pollChildrenAsyncHandler(http rc=${response.status}): ${it}"
                if(response.hasError()) {
                    logErr " pollChildrenAsyncHandler(http rc=${response.status}): Asynchttp error message: '${response.getErrorMessage()}'"
                    response.properties.each {
                        logDebug "pollChildrenAsyncHandler(http rc=${response.status}): ${it}"
                    }
                }
            }
        break
    }
    if (pollRetry) {
        // No panel update data was returned from BWA cloud server.  We must assume it is 'Offline' and update the respective child devices to reflect unknown state of spa
        // childDevice.parsePanelData(response?.xml?:response.data)
        if (childPollcount <= 2) {
            logWarn ("pollChildrenAsyncHandler(): BWA Cloud server did not return valid data for the SPA after ${childPollcount}/2 BWA cloud poll(s), Retry....")
            atomicState.childPollcount = childPollcount
            runIn(10, pollChildren, [overwrite: true, data: refreshOverride=true])
        } else {
            logWarn ("pollChildrenAsyncHandler(): BWA Cloud did not successfully return any valid data for the SPA, retry polling stopped after 2 times. Is your BWA Spa Offline?")
            statusOffline("BWA Cloud did not successfully return any valid data for the SPA")
            atomicState.childPollcount = 0
        }
    }
}


// Get device configuration
def getDeviceConfiguration() {
    state.refreshNumber += 1
    logDebug ("Getting DeviceConfiguration")
    def resp = doCallout("POST", "/devices/sci", getXmlRequestFilename("DeviceConfiguration", true), "xml")
    logDebug "getDeviceConfiguration(): resp=${resp.data}"
    return resp.data
}

def hexbytwo(text) {
    List<String> strings = new ArrayList<String>();
    def hexmap = [:]
    int index = 0;
    int index1 = 0
    while (index < text.length()) {
        strings.add(text.substring(index, Math.min(index + 2,text.length())));
        if (index >= 8) {
            hexmap << [(index1) : "<font color='red'>" + text.substring(index, Math.min(index + 2,text.length())).toUpperCase() + "</font>"]
            index1 += 1;
        }
        index += 2;
    }
    logTrace "panel hexmap= ${hexmap}"
    return strings
}

// Decode the encoded configuration data received from Balboa
def ParseDeviceConfigurationData(encodedData) {
    logDebug ("encodedData: '${encodedData}'")
    byte[] decoded = encodedData.decodeBase64()
    def spaStatusHexArray = hexbytwo(decoded.encodeHex().toString())
    logTrace "spaStatusHexArray= ${spaStatusHexArray}"
    if ( (encodedData != null) && (spaStatusHexArray[3] != VALID_SPA_STATUS_BYTE) ) {
        logDebug "ParseDeviceConfigurationData() changing spa state to Offline"
        statusOffline("BWA Cloud did not successfully return any valid data for the SPA")
        log.warn encodedData
        atomicState.HttpResponseExceptionReponse = encodedData.toString()
        return null
    }
    atomicState.status = "Online"
    def returnValue = [:]

    returnValue["Pump0"] = (decoded[7] & 128) != 0 ? true : false

    returnValue["Pump1"] = (decoded[4] & 3) != 0 ? true : false
    returnValue["Pump2"] = (decoded[4] & 12) != 0 ? true : false
    returnValue["Pump3"] = (decoded[4] & 48) != 0 ? true : false
    returnValue["Pump4"] = (decoded[4] & 192) != 0 ? true : false

    returnValue["Pump5"] = (decoded[5] & 3) != 0 ? true : false
    returnValue["Pump6"] = (decoded[5] & 192) != 0 ? true : false

    returnValue["Light1"] = (decoded[6] & 3) != 0 ? true : false
    returnValue["Light2"] = (decoded[6] & 192) != 0 ? true : false

    returnValue["Blower"] = (decoded[7] & 1) != 0 ? true : false

    returnValue["Aux1"] = (decoded[8] & 1) != 0 ? true : false
    returnValue["Aux2"] = (decoded[8] & 2) != 0 ? true : false
    returnValue["Mister"] = (decoded[8] & 16) != 0 ? true : false

    logDebug "ParseDeviceConfigurationData() returnValue= ${returnValue}"
    return returnValue
}

// Get panel update
def getPanelUpdate() {
    logInfo ("Getting spa panel latest update...")
    def resp = doCallout("POST", "/devices/sci", getXmlRequestFilename("PanelUpdate"), "xml")
    return resp?.data
}

def getXmlRequestFilename(fileName, cache=false) {
    def dataXML = "<sci_request version=\"1.0\"><file_system cache=\"${cache}\"><targets><device id=\"${state.spa['deviceId']}\"/></targets><commands><get_file path=\"${fileName}.txt\"/></commands></file_system></sci_request>"
    logTrace "getXmlRequestFilename(): dataXML= ${displayXML(dataXML)}"
    return dataXML
}

def getXmlRequest(data) {
    logDebug ("getXmlRequest(): data: ${data}")
    def dataXML = "<sci_request version='1.0'><data_service><targets><device id='${state.spa['deviceId']}'/></targets><requests>"
    def postXML = "</requests></data_service></sci_request>"
    data.each { spamap ->
        spamap.each { key, value ->
            dataXML += "<device_request target_name='${key}'>${value}</device_request>"
        }
    }
    dataXML += postXML
    logTrace "getXmlRequest(): dataXML= ${displayXML(dataXML)}"
    return dataXML
}

def sendCommand(data) {
    logDebug ("sendCommand: data: ${data}")
    def dataXML = getXmlRequest(data)
    logDebug "dataXML= ${displayXML(dataXML)}"
    logInfo "def resp = doCallout('POST', '/devices/sci', ${displayXML(dataXML)}, xml)"
    def response = doCallout("POST", "/devices/sci", dataXML, "xml")
    logDebug "sendCommand response.data= ${response?.data}, response.status= ${response?.status}"
    return response
}

def displayXML(dataXML) {
    return groovy.xml.XmlUtil.escapeXml(dataXML)
}

def isoFormat() {
    return "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
}

def toStDateString(date) {
    return date.format(isoFormat())
}

def parseStDate(dateStr) {
    return dateStr?.trim() ? timeToday(dateStr) : null
}

void updateLabel(status=null) {
    if (status==null) status = atomicState?.status
    def label = "${appUserLabel} <span style=color:"
    switch (status) {
        case 'Paused':
        label += 'Crimson'
        break
        case 'Offline':
        label += 'Red'
        break
        case 'Online':
        label += 'Green'
        break
        default:
            label += "Orange"
    }
    label += ">(${status})</span>"

    if (label != app.label) {
        app.updateLabel(label)
        log.debug "app.Label updated = ${label}"
    }

    if (atomicState?.status != status) {
        logDebug "atomicState.status updated = ${status}"
        atomicState.status = status
    }
}
