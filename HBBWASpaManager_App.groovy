/*
 *  Hubitat BWA Spa Manager
 *  -> App
 *
 *  Copyright 2023 Kurt Sannders
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
 *  CHANGE HISTORY
 *  VERSION     DATE            NOTES
 *  0.9.0       2020-01-30      Initial release with basic access and control of spas
 *  1.0.0       2020-01-31      Updated icons and bumped version to match DTH version
 *  1.0.1b      2020-09-17      Modified to now work with Hubitat
 *  1.1.0       2020-10-11      Major rewrite: Now downloads hot tub config, supports child devices, major refactoring, etc.
 *  1.2.0       2023-11-11      V1.2.0 code stream maintained by Kurt Sanders
 *                              Moved hardcoded logging in app to UI preferences and
 *                              Added logging expire timeout logic
 *                              Added App to HPM Public Install App
 *                              Added several error traps for unexpected conditions, like spa offline, BWA Cloud errors, etc.
 *  1.2.1       2023-12-05      V1.2.1 Added missing checkLogLevel() for logging timeout.
 */

import groovy.transform.Field
import groovyx.net.http.HttpResponseException

@Field static String AUTHOR_NAME               = "Kurt Sanders"
@Field static String NAMESPACE                 = "kurtsanders"
@Field static String PARENT_DEVICE_NAME        = "BWA Spa Manager"
@Field static final String VERSION             = "1.2.1"
@Field static final String COMM_LINK           = "https://community.hubitat.com/t/release-hb-bwa-spamanager/128842"
@Field static final String GITHUB_LINK         = "https://github.com/KurtSanders/HBBWASpaManager"
@Field static final String GITHUB_IMAGES_LINK  = "https://raw.githubusercontent.com/KurtSanders/HBBWASpaManager/master/images"

definition(
    name: PARENT_DEVICE_NAME,
    namespace: NAMESPACE,
    author: "Kurt Sanders",
    description: "Access and control your BWA Spa.",
    category: "Health & Wellness",
    iconUrl:   "${GITHUB_IMAGES_LINK}/hot-tub.png",
    iconX2Url: "${GITHUB_IMAGES_LINK}/hot-tub.png",
    iconX3Url: "${GITHUB_IMAGES_LINK}/hot-tub.png",
    singleInstance: false
) {
}

preferences {
    page(name: "mainPage")
    page(name: "authPage")
    page(name: "authResultPage")
    page(name: "confirmPage")
}

@Field static String BWGAPI_API_URL = "https://bwgapi.balboawater.com/"
@Field static String PARENT_DEVICE_NAME_PREFIX = "HB BPA SPA Parent"

def confirmPage() {
    def devConfig = getDeviceConfiguration(state.spa["deviceId"])
    def spaConfiguration = ParseDeviceConfigurationData(devConfig)
    logDebug "confirmPage() spaConfiguration.dump(): ${spaConfiguration}"

    dynamicPage(name: "confirmPage", uninstall: true, install: true) {
        section (sectionHeader("Name your BWA Spa Device")) {
            input(name: "spaParentDeviceName", type: "text", title: fmtTitle("Spa Parent Device Name:"), required: false, defaultValue: state.spa["deviceDisplayName"], description: fmtDesc(state.spa["deviceDisplayName"]))
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
            section(sectionHeader("Spa Not Responding")) {
                paragraph getFormat("text-red","${state.HttpResponseExceptionReponse}")
                paragraph "The Spa is not responding to BWA cloud, please check to see if the spa is online"
            }
        }
    }
}

def mainPage() {
    // Get spa if we don't have it already
    if (state.spa == null && state.token?.trim()) {
        getSpa()
    }

    dynamicPage(name: "mainPage", nextPage: "confirmPage", uninstall: false, install: false) {
        if (state.spa) {
            //Help Link
            section () {
                input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community <u>WebLink</u> to ${app.name}")
                paragraph ("")
            }
            section(sectionHeader("Found the following Spa (you can change the device name on the next page:")) {
                paragraph("${state.spa["deviceDisplayName"]} ${getImage("checkMarkGreen")}")
            }
            section(sectionHeader("BWA Authentication")) {
                href("authPage", title: fmtTitle("Cloud Authorization"), description: fmtDesc("${state.credentialStatus ? state.credentialStatus+"\n" : ""}Click to enter BWA credentials"))
            }
            section(sectionHeader("How frequently do you want to poll the BWA cloud for changes? (Use a lower number if you care about trying to capture and respond to \"change\" events as they happen)")) {
                input(name: "pollingInterval", title: fmtTitle("Polling Interval (in Minutes)"), type: "enum", required: true, multiple: false, defaultValue: 5, options: ["1", "5", "10", "15", "30"])
                input name: "pollingModes", type: "mode", title: fmtTitle("Poll BWA cloud only when in one of these modes"),required: true, defaultValue: location.mode, multiple: true, submitOnChange: false            }
        }
        section(sectionHeader("BWA Logging Options")) {
            //Logging Options
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
                description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
        }
        section (sectionHeader("Name this instance of ${app.name}")) {
            label name: "name", title: fmtTitle("Assign a name for this app"), required: false, defaultValue: app.name, description: fmtDesc(app.name), submitOnChange: true
        }
    }
}

def authPage() {
    dynamicPage(name: "authPage", nextPage: "authResultPage", uninstall: false, install: false) {
        section("BWA Credentials") {
            input("username", "username", title: fmtTitle("User ID"), description: fmtDesc("BWA User ID"), required: true)
            input("password", "password", title: fmtTitle("Password"), description: fmtDesc("BWA Password"), required: true)
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
            section("${state.loginResponse}") {
                paragraph ("Please check your credentials and try again.")
            }
        }
    } else {
        logDebug ("authResultPage() state.token != null")
        dynamicPage(name: "authResultPage", nextPage: "mainPage", uninstall: false, install: false) {
            section("${state.loginResponse}") {
                paragraph ("Please click next to continue setting up your spa.")
            }
        }
    }
}

boolean doLogin(){
    def loggedIn = false
    def resp = doCallout("POST", "/users/login", [username: username, password: password])

    switch (resp.status) {
        case 403:
            state.loginResponse = "Access forbidden"
            state.credentialStatus = getFormat("text-red","[Disconnected]")
            state.token = null
            state.spas = null
            break
        case 401:
            state.loginResponse = resp.data.message
            state.credentialStatus = getFormat("text-red","[Disconnected]")
            state.token = null
            state.spas = null
            break
        case 200:
            logInfo ("Successfully logged in.")
            loggedIn = true
            state.loginResponse = "Logged in"
            state.token = resp.data.token
            state.credentialStatus = getFormat("text-green","[Connected]")
            state.loginDate = toStDateString(new Date())
            cacheSpaData(resp.data.device)
            logInfo ("Done caching SPA data.")
            break
        default:
            logDebug (resp.data)
            state.loginResponse = "Login unsuccessful"
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
    logDebug "==> data= ${data}"
    return cacheSpaData(data.device)
}

def cacheSpaData(spaData) {
    // save in state so we can re-use in settings
    logDebug ("Saving Spa data in the state cache (app.id: ${app.id}, device_id: ${spaData.device_id})...")
    state.spa = [:]
    state.spa["appId"] = app.id;
    state.spa["deviceId"] = spaData.device_id
    state.spa["deviceNetworkId"] = [app.id, spaData.device_id].join('.')
    state.spa["deviceDisplayName"] = "Spa " + spaData.device_id[-8..-1]
    return spaData
}

def doCallout(calloutMethod, urlPath, calloutBody) {
    doCallout(calloutMethod, urlPath, calloutBody, "json", null)
}

def doCallout(calloutMethod, urlPath, calloutBody, contentType) {
    doCallout(calloutMethod, urlPath, calloutBody, contentType, null)
}

def doCallout(calloutMethod, urlPath, calloutBody, contentType, queryParams) {
    logDebug ("\"${calloutMethod}\"-ing ${contentType} to \"${urlPath}\"")
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
        headers: [
            Authorization: state.token?.trim() ? "Bearer ${state.token as String}" : null
        ],
        requestContentType: content_type,
        body: calloutBody
    ]

    def result
    try {
        switch (calloutMethod) {
            case "GET":
                httpGet(params) { resp ->
                    result = resp
                }
                break
            case "PATCH":
                params.headers["x-http-method-override"] = "PATCH"
                // NOTE: break is purposefully missing so that it falls into the next case and "POST"s
            case "POST":
            	httpPost(params) { resp ->
                	result = resp
                }
                break
            default:
                logErr "unhandled method"
                return [error: "unhandled method"]
                break
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        state.HttpResponseExceptionReponse = "${e.response.status} ${e.response.statusLine.reasonPhrase}"
        logWarn state.HttpResponseExceptionReponse
        return e.response.success
    } catch (e) {
        logWarn "Something went wrong: ${e}"
        return [error: e.message]
    }

    logDebug "==> Post result= ${result.data}"
    return result
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    // Not sure when tokens expire, but will get a new one every 24 hours just in case by scheduling to reauthorize every day
    if(state.loginDate?.trim()) schedule(parseStDate(state.loginDate), reAuth)

    def delete = getChildDevices().findAll { !state.spa["deviceNetworkId"] }
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }

    def childDevices = []
    try {
        def childDevice = getChildDevice(state.spa["deviceId"])
        if(!childDevice) {
            logInfo ("Adding device: ${settings.spaParentDeviceName} [${state.spa["deviceId"]}]")
            childDevice = addChildDevice(NAMESPACE, PARENT_DEVICE_NAME_PREFIX, state.spa["deviceId"], [label: settings.spaParentDeviceName])
            state.spa["deviceDisplayName"] = settings.spaParentDeviceName
            childDevice.parseDeviceData(state.spa)
            childDevice.createChildDevices(state.spaConfiguration)
        }
        childDevices.add(childDevice)
    } catch (e) {
        logErr "Error creating device: ${e}"
    }

    // set up polling only if we have child devices
    if(childDevices.size() > 0) {
        pollChildren()
        "runEvery${pollingInterval}Minute${pollingInterval != "1" ? 's' : ''}"("pollChildren")
    } else unschedule(pollChildren)
    checkLogLevel()
}

def pollChildren(refreshOverride=false) {
    logDebug ("pollChildren()...")
    // Check for a location mode that allowed to poll per preference settings
    if (refreshOverride || pollingModes.contains(location.mode)) {
        def devices = getChildDevices()
        devices.each {
            def deviceId = it.currentValue("deviceId", true)
            if (deviceId == null) {
                logWarn ("Error, deviceId was null. Didn't actually poll the server or spa or BWA cloud is Offline? Retrying in 5 seconds...")
                runIn(5, pollChildren)
                return
            }
            def deviceData = getPanelUpdate(deviceId)
            if (deviceData != null) {
                it.parsePanelData(deviceData)
            } else {
                logWarn ("BWA Cloud did not successfully return any data for the SPA, Retrying in 5 secords....")
                runIn(5, pollChildren)
            }
        }
    } else {
        logDebug "Skipping BWA Cloud polling.. Currennt '${location.mode}' mode is not a valid polling mode(s) of '${pollingModes.join(", ")}'"
    }
}

// Get device configuration
def getDeviceConfiguration(device_id) {
    logDebug ("Getting device configuration for ${device_id}")
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(device_id, "DeviceConfiguration"), "xml")
    if (resp != false){
        return resp.data
    }
    return resp
}

// Decode the encoded configuration data received from Balboa
def ParseDeviceConfigurationData(encodedData) {
    logTrace ("encodedData: '${encodedData}'")
    if (encodedData == null || encodedData == false) return
    byte[] decoded = encodedData.decodeBase64()
    logTrace ("decoded: '${decoded}'")
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

    returnValue["Blower"] = (decoded[7] & 0) != 0 ? true : false

    returnValue["Aux1"] = (decoded[8] & 1) != 0 ? true : false
    returnValue["Aux2"] = (decoded[8] & 2) != 0 ? true : false
    returnValue["Mister"] = (decoded[8] & 16) != 0 ? true : false

    return returnValue
}

// Get panel update
def getPanelUpdate(device_id) {
    logDebug ("Getting panel update for ${device_id}")
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(device_id, "PanelUpdate"), "xml")
    return resp.data
}

def getXmlRequest(deviceId, fileName) {
    return "<sci_request version=\"1.0\"><file_system cache=\"false\"><targets><device id=\"${deviceId}\"/></targets><commands><get_file path=\"${fileName}.txt\"/></commands></file_system></sci_request>"
}

def sendCommand(deviceId, targetName, data) {
    logDebug ("sending ${targetName}:${data} command for ${deviceId}")
    def resp = doCallout("POST", "/devices/sci", getXmlRequest(deviceId, targetName, data), "xml")
    return resp.data
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
	String info =     "${PARENT_DEVICE_NAME} v${VERSION}"
	String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
	String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
	String topLink =  "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"
    return "<div style='text-align: center; position: absolute; top: 0px; left: 400px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}
def getImage(type) {
    if(type == "Blank")          return "<img src=${GITHUB_IMAGES_LINK}/blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "<img src=${GITHUB_IMAGES_LINK}/checkMarkGreen2.png height=30 width=30>"
    if(type == "optionsGreen")   return "<img src=${GITHUB_IMAGES_LINK}/options-green.png height=30 width=30>"
    if(type == "optionsRed")     return "<img src=${GITHUB_IMAGES_LINK}/options-red.png height=30 width=30>"
    if(type == "instructions")   return "<img src=${GITHUB_IMAGES_LINK}/instructions.png height=30 width=30>"
}

def getFormat(type, myText="") {
    if(type == "header-blue") return "<div style='color:#ffffff;font-weight: bold;background-color:#309bff;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "header-red")  return "<div style='color:#ffffff;font-weight: bold;background-color:#ff0000;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line")        return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title")       return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    if(type == "text-green")  return "<div style='color:green'>${myText}</div>"
    if(type == "text-red")    return "<div style='color:red'>${myText}</div>"
}

def help() {
    section("${getImage('instructions')} <b>${app.name} Online Documentation</b>", hideable: true, hidden: true) {
        paragraph "<a href='${GITHUB_LINK}#readme' target='_blank'><h4 style='color:DodgerBlue;'>Click this link to view Online Documentation for ${app.name}</h4></a>"
    }
}

def sectionHeader(title){
    return getFormat("header-blue", "${getImage("Blank")}"+" ${title}")
}

/*******************************************************************
 ***** Logging Functions
********************A************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Off", 1:"Error", 2:"Warn", 3:"Info", 4:"Debug", 5:"Trace"]
@Field static final Map LOG_TIMES  = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]
@Field static final String LOG_DEFAULT_LEVEL = 0

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:LOG_DEFAULT_LEVEL, type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"0", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 1 && levelInfo.time > 0) {
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
	Integer level = settings.logLevel as Integer ?: 0
	Integer time = settings.logLevelTime as Integer ?: 0
	return [level: level, time: time]
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 1) {
		device.updateSetting("logLevel",[value:"0", type:"enum"])
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${app.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=2) log.warn "${app.name}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=3) log.info "${app.name}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=4) log.debug "${app.name}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=5) log.trace "${app.name}: ${msg}"
}