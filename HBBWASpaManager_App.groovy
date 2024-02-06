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
import groovyx.net.http.HttpResponseException

@Field static String PARENT_DEVICE_NAME            = "BWA Spa Manager"
@Field static final String VERSION                 = "1.3.0"
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

def mainPage() {
    // Get spa data if we don't have it already, set the install flag
    dynamicPage(name: "mainPage", uninstall: true, nextPage: "confirmPage", install: checkSpaState() ) {
        // Set Logging On Initially
        if (state?.logLevel==null || state.logLevelTime == null) {
            setLogLevel("Info", "30 Minutes")
            logInfo "Setting initial application level logging to 'Info' for 30 Minutes..."
        }
        //Help Link
        section () {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community <u>WebLink</u> to ${app.name}")
            paragraph ("")
        }
        section(sectionHeader("BWA Authentication")) {
            href("authPage", title: fmtTitle("Cloud Authorization"), description: fmtDesc("${state.credentialStatus ? state.credentialStatus : ""}Click to enter BWA credentials"))
        }
        if (state.spa) {
            section(sectionHeader("${state?.spa["deviceDisplayName"]} (BWA Username: ${state?.spa.username}) ${getImage("checkMarkGreen")}")) {
                href("confirmPage", title: fmtTitle("View your spa's mechanical configuration and name parent spa control device."), description: fmtDesc(spaManifest() + "\n" + "Click to generate a new spa mechanical configuration (not needed if correct)"))
            }

            // BWA Cloud Polling Options
            section(sectionHeader("How frequently do you want to poll the BWA cloud for changes? (Use a lower number if you care about trying to capture and respond to \"change\" events as they happen)")) {
                input(name: "pollingInterval", title: fmtTitle("Polling Interval (in Minutes)"), type: "enum", required: true, multiple: false, defaultValue: 5, options: ["1", "5", "10", "15", "30"])
                input name: "pollingModes", type: "mode", title: fmtTitle("Poll BWA cloud only when in one of these modes"),required: true, offerAll: true, defaultValue: location.mode, multiple: true, submitOnChange: false            }

            //Logging Options
            section(sectionHeader("BWA Logging Options")) {
                input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                    description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
                input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
                    description: fmtDesc("Time to enable Debug/Trace logging"), defaultValue: 30, options: LOG_TIMES
            }

            // Custom App Name
            section (sectionHeader("Name this instance of ${app.name}")) {
                String defaultName = "${app.name} - ${state?.spa.username}"
                logDebug "defaultName= ${defaultName}"
                if (state.displayName) {
                    defaultName = state.displayName
                    app.updateLabel(defaultName)
                }
                label title: fmtTitle("Assign an app name"), required: false, defaultValue: "${defaultName}", description: fmtDesc("${defaultName}")
            }
        }
    }
}

def appButtonHandler(String buttonName) {
    logDebug "buttonName= ${buttonName}"
    if (buttonName== "RefreshSpaData") {
    }
}

def confirmPage() {
    if (state.token == null) {
        log.warn ("BWA User notAuthenticated() state.token == null, You must login to BWA Cloud on the Main Page of the App before proceeding")
        dynamicPage(name: "confirmPage", uninstall: false, install: false) {
            section(sectionHeader("Not Logged In!")) {
                paragraph (getFormat("text-red","Please must login. Check your credentials and try again to login to BWA Cloud."))
            }
        }
    } else {
        def devConfig = getDeviceConfiguration(state.spa["deviceId"])
        logDebug "def devConfig= ${devConfig}"
        def spaConfiguration = ParseDeviceConfigurationData(devConfig)

        dynamicPage(name: "confirmPage", uninstall: false, install: false) {
            if (spaConfiguration != null) {
                section (sectionHeader("Name your BWA Spa Device")) {
                    input(name: "spaParentDeviceName", type: "text", title: fmtTitle("Spa Parent Device Name:"), required: false, defaultValue: state.spa["deviceDisplayName"], description: fmtDesc(state.spa["deviceDisplayName"]))
                }
            }
            if (spaConfiguration != null) {
                state.spaConfiguration = spaConfiguration
                def deviceCount = spaConfiguration.count { key, value -> value == true }
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
                section(sectionHeader("BWA Spa Not Responding")) {
                    paragraph "<div style='color:red'>${getImage('button-red')} → <b>${state.HttpResponseExceptionReponse}</b></div>"
                    para
                    paragraph "Your BWA Spa is not connecting/responding to the BWA cloud. Please verify that your spa is online by following the suggestions below:\n" +
                    "<ol><li>Verify your spa is connecting to the BWA cloud by installing/using the <a href='https://www.balboawatergroup.com/bwa'>BWA Spa Control mobile app.</a></li>" +
                    "<li>If your spa is not reporting to the BWA cloud, power the spa off/on, and wait a few minutes to see that it has connectivity to the BWA cloud by verifying with the BWA Spa Control App</li>" +
                    "<li>If your spa is still not reporting to the BWA cloud, you may need to reboot your WiFi router for the spa to get an ip address and connectivity.</li>" +
                    "<li>Balboa recommends that your spa be only 25-50 feet from your WiFi access point for optimal connectivity.  Walls and other layers will reduce the signal.</li>" +
                    "<li>You can contact Balboa for Technical support at techsupport@balboawater.com</li></ol>"

                }
            }
        }
    }
}

def authPage() {
    dynamicPage(name: "authPage", nextPage: "authResultPage", uninstall: true, install: false) {
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
        logDebug ("authResultPage() state.token != null")
        dynamicPage(name: "authResultPage", nextPage: "mainPage", uninstall: false, install: false) {
            section(sectionHeader("${state.loginResponse}")) {
                paragraph ("${getImage('checkMarkGreen')}${getFormat('text-green','Please click next to continue setting up your spa.')}")
            }
        }
    }
}

def checkSpaState() {
    def installBoolean = true
    if (state.spa == null && state.token?.trim()) {
        if (getSpa() == null) installBoolean = false
    }
    if (state.spaConfiguration == null) installBoolean = false
    logDebug "installBoolean= ${installBoolean}"
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
            state.loginResponse = "BWA Server/Gateway Timeout"
            updateLabel("Login Error - ${state.loginResponse}")
            state.credentialStatus = getFormat("text-red","[Offline]")
            state.token = null
            state.spas = null
            break
        case 403:
            state.loginResponse = "Access forbidden"
            updateLabel("Login Error - ${state.loginResponse}")
            state.credentialStatus = getFormat("text-red","[Disconnected]")
            state.token = null
            state.spas = null
            break
        case 401:
            state.loginResponse = resp.data.message
            updateLabel("Login Error - ${state.loginResponse}")
            state.credentialStatus = getFormat("text-red","[Disconnected]")
            state.token = null
            state.spas = null
            break
        case 200:
            logInfo ("Successfully logged in.")
            loggedIn = true
            state.loginResponse = "Connected"
            updateLabel("Logged in")
            state.token = resp.data.token
            state.credentialStatus = getFormat("text-green","[Connected]")
            state.loginDate = toStDateString(new Date())
            cacheSpaData(resp.data)
            logInfo ("Done caching SPA device data...")
            break
        default:
            logDebug (resp.data)
            state.loginResponse = "Login unsuccessful"
            updateLabel("Disconnected")
            state.credentialStatus = getFormat("text-red","[Disconnected]")
            state.token = null
            state.spas = null
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
        updateLabel("Online")
        state.spa = [:]
        state.spa["appId"] = app.id;
        state.spa["username"] = spaData?.username
        state.spa["deviceId"] = spaData.device?.device_id
        state.spa["deviceNetworkId"] = [app.id, spaData.device?.device_id].join('.')
        state.spa["deviceDisplayName"] = "Spa ${username}"
//        state.spa["deviceDisplayName"] = "Spa " + spaData.device?.device_id[-8..-1]
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
    def result
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
                    result = resp
                }
                break
            case "PATCH":
                params.headers["x-http-method-override"] = "PATCH"
                // NOTE: break is purposefully missing so that it falls into the next case and "POST"s
            case "POST":
                logTrace "httpPost(params → ${params})"
            	httpPost(params) { resp ->
                	result = resp
                    logDebug "doCallout(httpPost) result.status = ${result?.status}"
                    logDebug "doCallout(httpPost) result.data   = ${result?.data}"
                }
                break
            default:
                logErr "unhandled method"
                return [error: "unhandled method"]
                break
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logDebug "Http Error (${e.response.status}): ${e.response.statusLine.reasonPhrase}"
        state.HttpResponseExceptionReponse = "http rc=${e.response.status}, ${e.response.statusLine.reasonPhrase}"
        updateLabel("Offline")
        return [status: "${e.response.status}", data: "${e.response.statusLine.reasonPhrase}"]
    } catch (e) {
        logWarn "Something went wrong: ${e}"
        updateLabel("Offline")
        return [error: e.message]
    }

    if ( (result?.status=='200') && isValidSpaMessageType(result?.data) ) {
        byte[] decoded = result.data.decodeBase64()
        def spaStatusHexArray = hexbytwo(decoded.encodeHex().toString())
        logDebug "spaStatusHexArray= ${spaStatusHexArray}"
        updateLabel("Online")
    }
    return result
}

def isValidSpaMessageType(encodedData) {
    def datastring = encodedData.toString()
    if (encodedData == null) return false
    if ( datastring.startsWith('Hf+') && datastring.endsWith('=') ) {
        logDebug "Valid encoded spa data: '${encodedData}'"
        return true
    } else {
        logDebug "Invalid encoded spa data: '${encodedData}'"
        return false
    }
}

def installed() {
    logInfo "installed()"
    initialize()
}

def updated() {
    logInfo "updated()"
    unsubscribe()
    initialize()
}

def initialize() {
    logInfo "initialize()"
    // Not sure when tokens expire, but will get a new one every 24 hours just in case by scheduling to reauthorize every day
    if(state.loginDate?.trim()) schedule(parseStDate(state.loginDate), reAuth)

    def delete = getChildDevices().findAll { !state.spa["deviceNetworkId"] }
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
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
    logInfo "${childDevice.label} has a polling interval of ${pollingInterval} minute${pollingInterval != "1" ? 's' : ''}"
    childDevice.parseDeviceData([pollingInterval : "${pollingInterval} minute${pollingInterval != "1" ? 's' : ''}" ])

    // set up polling only if we have child devices
    if(childDevices.size() > 0) {
        pollChildren()
        logInfo "Setting BWA polling interval to ${pollingInterval} minute${pollingInterval != "1" ? 's' : ''}"
        "runEvery${pollingInterval}Minute${pollingInterval != "1" ? 's' : ''}"("pollChildren")
    } else unschedule(pollChildren)
    updateLabel('Online')
    checkLogLevel()
}

def pollChildren(refreshOverride=false) {
    def childPollcount = state.childPollcount?:0
    childPollcount += 1
    // Check for a location mode that allowed to poll per preference settings
    if (refreshOverride || pollingModes.contains(location.mode)) {
        logInfo ("pollChildren() Attempt#: ${childPollcount}...")
        def devices = getChildDevices()
        devices.each {
            def deviceId = it.currentValue("deviceId", true)
            if (deviceId == null) {
                updateLabel("Offline")
                if (childPollcount <= 5) {
                    logWarn ("Error, deviceId was null. Didn't actually poll the server or spa or BWA cloud is Offline? Retrying in 5 seconds...")
                    runIn(5, pollChildren)
                } else {
                    logWarn "The Spa Parent deviceId was null after 5 polls, retry polling stopped..."
                    state.childPollcount = 0
                }
                return
            }
            def deviceData = getPanelUpdate(deviceId)
            logDebug "pollchildren deviceData= ${deviceData}"

            if ( (deviceData != null) || isValidSpaMessageType(deviceData)) {
                byte[] decoded = deviceData.decodeBase64()
                def spaStatusHexArray = hexbytwo(decoded.encodeHex().toString())
                logDebug "it.parsePanelData spaStatusHexArray= ${spaStatusHexArray}"
                it.parsePanelData(deviceData)
                childPollcount = 0
                state.childPollcount = 0
                updateLabel("Online")
                logDebug "Spa online: device data returned, reset childPollcount= ${childPollcount}"
            } else {
                updateLabel("Offline")
                it.parsePanelData(deviceData)
                if (childPollcount <= 5) {
                    logWarn ("BWA Cloud server did not successfully return any data for the SPA after ${childPollcount}/5 BWA cloud poll(s), Retrying in 1 minute....")
                    state.childPollcount = childPollcount
                    runIn(60, pollChildren, [overwrite: true, data: refreshOverride])
                } else {
                    logWarn ("BWA Cloud did not successfully return any valid data for the SPA, retry polling stopped. Is the BWA cloud Offline?")
                    state.childPollcount = 0
                    updateLabel("Offline")
                }
            }
        }
    } else {
        logDebug "Skipping BWA Cloud polling.. Currennt '${location.mode}' mode is not a valid polling mode(s) of '${pollingModes.join(", ")}'"
        updateLabel("Polling paused in ${location.mode} mode")
    }
}

// Get device configuration
def getDeviceConfiguration(device_id) {
    logDebug ("Getting device configuration for ${device_id}")
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(device_id, "DeviceConfiguration"), "xml")
    logDebug "getDeviceConfiguration(${device_id}) resp= ${resp}"
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
    logDebug "hexmap= ${hexmap}"
    return strings
}

// Decode the encoded configuration data received from Balboa
def ParseDeviceConfigurationData(encodedData) {
    logDebug ("encodedData: '${encodedData}'")
    byte[] decoded = encodedData.decodeBase64()
    def spaStatusHexArray = hexbytwo(decoded.encodeHex().toString())
    logDebug "spaStatusHexArray= ${spaStatusHexArray}"
    if ( (encodedData != null) && (spaStatusHexArray[3] != VALID_SPA_STATUS_BYTE) ) {
        updateLabel("Offline")
        log.warn encodedData
        state.HttpResponseExceptionReponse = encodedData.toString()
        return null
    }
    state.status = "Online"
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
def getPanelUpdate(device_id) {
    logInfo ("Getting panel update...")
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(device_id, "PanelUpdate"), "xml")
    return resp?.data
}

def getXmlRequest(deviceId, fileName) {
    return "<sci_request version=\"1.0\"><file_system cache=\"false\"><targets><device id=\"${deviceId}\"/></targets><commands><get_file path=\"${fileName}.txt\"/></commands></file_system></sci_request>"
}

def sendCommand(deviceId, targetName, data) {
    logDebug ("sendCommand: ${targetName} → ${data}")
    logTrace "XML → ${groovy.xml.XmlUtil.escapeXml(getXmlRequest(deviceId, targetName, data))}"
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(deviceId, targetName, data), "xml")
    logDebug "sendCommand resp.data= ${resp?.data}"
    return resp?.data
}

def getXmlRequest(deviceId, targetName, data) {
    return "<sci_request version=\"1.0\"><data_service><targets><device id=\"${deviceId}\"/></targets><requests><device_request target_name=\"${targetName}\">${data}</device_request></requests></data_service></sci_request>"
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

void updateLabel(status) {

    // Store the user's original label in state.displayName
    if (state.displayName==null) {
        def displayName = "${app.label} - ${username}"
        state?.displayName = displayName
        app.updateLabel(displayName)
        return
    }
    if (!app.label.contains("<span") && state?.displayName != app.label) {
        state.displayName = app.label
    }
    String label = "${state?.displayName} <span style=color:"

    switch (status) {
        case ~/.*Login.*/:
        case 'Offline':
        label += 'red'
        break
        case 'Online':
        label += 'green'
        break
        default:
            label += "orange"
    }
    label += ">(${status})</span>"
    app.updateLabel(label)

    if (state?.status != status) {
        logDebug "App label Updated = ${label}"
        state.status = status
    }
}