/*******************************************************************
*** SanderSoft - Core App/Device Helpers                        ***
/*******************************************************************/

library (
    base: "app",
    author: "Kurt Sanders",
    category: "Apps",
    description: "Core functions for Balboa Hot Tub Local Driver.",
    name: "Balboa-Hot-Tub-API-Library",
    namespace: "kurtsanders",
    documentationLink: "https://github.com/KurtSanders/",
    version: "0.0.4",
    disclaimer: "This library is only for use with SanderSoft Apps and Drivers."
)

@Field static final String AUTHOR_NAME          	= "Kurt Sanders"
@Field static final String NAMESPACE            	= "kurtsanders"
@Field static final String COMM_LINK            	= "https://community.hubitat.com/t/release-bwa-spamanager-cloud-control-direct-local-tcp/151421"
@Field static final String GITHUB_LINK          	= "https://github.com/KurtSanders/HBBWASpaManager"
@Field static final List   ONOFF                  	= ["On", "Off"]
@Field static final Map    TEMP_REPORTING_DELTA    	= [(1):"± 1°",(2):"± 2°",(3):"± 3°",(4):"± 4°",(5):"± 5°",(10):"± 10°",(15):"± 15°"]
@Field static final String UNKNOWN 					= "unknown"
@Field static final List   HEATMODES 				= ["Ready", "Rest"]
@Field static final List   TEMPRANGES 				= ["High", "Low"]
@Field static final List   LIGHTSMODES 				= ["off", "on"]
@Field static final int    SOCKET_CONNECT_READ_DELAY 	= 150
@Field static final String MESSAGE_DELIMITER 		= '7E'
@Field static final String CHANNEL 					= '0ABF'

@Field static final String SET_HEAT_MODE 			= '7E070ABF115100C87E'
@Field static final String SET_TEMP_RANGE	 		= '7E070ABF115000DD7E'
@Field static final String SET_SOAK_MODE 			= '7E070ABF111D006F7E'
@Field static final String SET_AUX2 				= '7E070ABF111700ED7E'
@Field static final String SET_AUX1 				= '7E070ABF111600F87E'
@Field static final String SET_LIGHT2 				= '7E070ABF111200AC7E'
@Field static final String SET_LIGHT1 				= '7E070ABF111100937E'
@Field static final String SET_MISTER 				= '7E070ABF110E00077E'
@Field static final String SET_BLOWER 				= '7E070ABF110C002D7E'
@Field static final String SET_PUMP6 				= '7E070ABF1109006C7E'
@Field static final String SET_PUMP5 				= '7E070ABF110800797E'
@Field static final String SET_PUMP4 				= '7E070ABF110700BA7E'
@Field static final String SET_PUMP3 				= '7E070ABF110600AF7E'
@Field static final String SET_PUMP2 				= '7E070ABF110500907E'
@Field static final String SET_PUMP1 				= '7E070ABF110400857E'
@Field static final String GET_FILTER				= '7E080ABF22010000347E'
@Field static final String GET_SETTINGS				= '7E080ABF22040000f47E'
@Field static final String GET_SYSTEM				= '7E080ABF22020000897E'
@Field static final String GET_DEVICES				= '7E080ABF22000001587E'
@Field static final String GET_MODULE				= '7E050ABF04777E'
@Field static final String NOTHING_TO_SEND			= '7E050ABF077E7E'
@Field static final List SUPPORTED_PUMP_SPEED_MODES = ["off", "low", "high"]
@Field static final List THERMO_STAT_OPERATING_STATES = ["heating", "idle", "pending heat"]
@Field static       List SWITCH_DEVICES_OPTIONS		= ['Pump1','Pump2','Pump3','Pump4','Pump5','Pump6','Light1','Light2']
@Field static final List THERMO_STAT_MODES 			= ["off","heat"]
@Field static final List THERMO_STAT_FAN_MODES 		= ["off"]
@Field static final Integer MAX_MESSAGES			= 6
@Field static final Map TEMPERATURE_VALID_RANGES_F 	= [
        'low' : [50,80],
        'high': [80,104]
        ]
@Field static final Map TOGGLE_ITEM_HEX_CODE 		= [
	'Pump1'				:'04',
	'Pump2'				:'05',
	'Pump3'				:'06',
	'Pump4'				:'07',
	'Pump5'				:'08',
	'Pump6'				:'09',
	'Blower'			:'0C',
	'Mister'			:'0E',
	'Light1'			:'11',
	'Light2'			:'12',
	'Aux1'				:'16',
	'Aux2'				:'17',
	'soakMode'			:'1D',
	'tempRange'			:'50',
	'heatMode'			:'51'
]

@Field static final Map POLLING_OPTIONS         	= [
    'off'  			   :'Off', 
    'runEvery1Minute'  :'Every 01 Minute',
    'runEvery5Minutes' :'Every 05 Minutes',
    'runEvery10Minutes':'Every 10 Minutes',
    'runEvery15Minutes':'Every 15 Minutes',
    'runEvery30Minutes':'Every 30 Minutes',
    'runEvery1Hour'    :'Every 01 Hour',
    'runEvery3Hours'   :'Every 03 Hours'
    ]
@Field static final Map BALBOA_MESSAGE_TYPES   		= [
    '13':[ 
    	'name' 		:'Status',
    	'color'		:'purple',
        'program'	:'parsePanelData'
    	],
    '2E':[
        'name' 		:'Configuration',
    	'color'		:'brown',
        'program'	:'ParseDeviceConfigurationData'
    	],
    '22':[
        'name' 		:'Settings',
    	'color'		:'blue'
    	],
    '23':[
        'name' 		:'Filter',
    	'color'		:'orange',
        'program'	:'parseFilterResponse'
    	],
    '24':[
        'name' 		:'System',
    	'color'		:'orange',
        'program'	:'parseInformationResponse'
    	],
    '25':[
        'name' 		:'Preference',
    	'color'		:'blue'
    	],
    '26':[
        'name' 		:'WiFi Module',
    	'color'		:'aqua'
    	]
    ]

def help() {
    section("${getImage('instructions')} <b>${app.name} Online Documentation</b>", hideable: true, hidden: true) {
        paragraph "<a href='${GITHUB_LINK}#readme' target='_blank'><h4 style='color:DodgerBlue;'>Click this link to view Online Documentation for ${app.name}</h4></a>"
    }
}

String fmtHelpInfo(String str) {
    String info = "${PARENT_DEVICE_NAME} v${VERSION}"
    String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"
    if (device) {   
        return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
            "<div style='text-align: center; position: absolute; top: 30px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
    } else {
        return "<div style='text-align: center; position: absolue; top: 0px; right: 80px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"

    }
}

def createDataChildDevice(namespace, typeName, deviceNetworkId) {    
    logDebug "In createDataChildDevice()"
    def statusMessageD = ""
    def rc
    if(!getChildDevice(deviceNetworkId)) {
        logInfo "In createDataChildDevice - Child device not found - Creating device: ${typeName}"
        try {
            rc = addChildDevice(namespace, typeName, deviceNetworkId, ["name": "${typeName}", "label": "${typeName}", isComponent: false])
            logInfo "In createDataChildDevice - Child device has been created! (${typeName})"
            statusMessageD = "<b>Device has been been created. (${typeName})</b>"
        } catch (e) { logErr "Unable to create device - ${e}" }
    } else {
        statusMessageD = "<b>Device Name (${typeName}) already exists.</b>"
    }
    logInfo "${statusMessageD}"
    return rc
}

// Thanks to author: "Jean P. May Jr." for these following Hubitat local file access methods
// importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/libraries/localFileMethods.groovy",


HashMap securityLogin(){
    def result = false
    try{
        httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
		)
		{ resp ->
//			log.debug resp.data?.text
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
		    	}
		}
    }catch (e){
			log.error "Error logging in: ${e}"
			result = false
            cookie = null
    }
	return [result: result, cookie: cookie]
}


Boolean fileExists(fName){

    uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                return true;
            } else {
                return false;
            }
        }
    } catch (exception){
        if (exception.message == "Not Found"){
            log.debug("File DOES NOT Exists for $fName)");
        } else {
            log.error("Find file $fName) :: Connection Exception: ${exception.message}");
        }
        return false;
    }

}

String readFile(fName){
    if(security) cookie = securityLogin().cookie
    uri = "http://${location.hub.localIP}:8080/local/${fName}"


    def params = [
        uri: uri,
        contentType: "text/html",
        textParser: true,
        headers: [
				"Cookie": cookie,
                "Accept": "application/octet-stream"
            ]
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {       
              // return resp.data
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Error: ${exception.message}"
        return null;
    }
}

Boolean appendFile(fName,newData){
    try {
        fileData = (String) readFile(fName)
        fileData = fileData.substring(0,fileData.length()-1)
        return writeFile(fName,fileData+newData)
    } catch (exception){
        if (exception.message == "Not Found"){
            return writeFile(fName, newData)      
        } else {
            log.error("Append $fName Exception: ${exception}")
            return false
        }
    }
}

Boolean writeFile(String fName, String fData) {
    byte[] fDataB = fData.getBytes("UTF-8")
    return writeImageFile(fName, fDataB, "text/html")   
}

Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    return retStat
}

String readExtFile(fName){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               return delim
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null;
    }
}

HashMap readImage(imagePath){   
    def imageData
    if(security) cookie = securityLogin().cookie   

    if(debugEnabled) log.debug "Getting Image $imagePath"
    httpGet([
        uri: "$imagePath",
        contentType: "*/*",
        headers: [
            "Cookie": cookie
        ],
        textParser: false]){ response ->
            if(debugEnabled) log.debug "${response.properties}"
            imageData = response.data 
            if(debugEnabled) log.debug "Image Size (${imageData.available()} ${response.headers['Content-Length']})"

            def bSize = imageData.available()
            def imageType = response.contentType 
            byte[] imageArr = new byte[bSize]
            imageData.read(imageArr, 0, bSize)
            if(debugEnabled) log.debug "Image size: ${imageArr.length} Type:$imageType"  
            return [iContent: imageArr, iType: imageType]
        }    
}

Boolean writeImageFile(String fName, byte[] fData, String imageType) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();
    bDataTop = "--${encodedString}\r\nContent-Disposition: form-data; name=\"uploadFile\"; filename=\"${fName}\"\r\nContent-Type:${imageType}\r\n\r\n" 
    bDataBot = "\r\n\r\n--${encodedString}\r\nContent-Disposition: form-data; name=\"folder\"\r\n\r\n--${encodedString}--"
    byte[] bDataTopArr = bDataTop.getBytes("UTF-8")
    byte[] bDataBotArr = bDataBot.getBytes("UTF-8")
    
    ByteArrayOutputStream bDataOutputStream = new ByteArrayOutputStream();

    bDataOutputStream.write(bDataTopArr);
    bDataOutputStream.write(fData);
    bDataOutputStream.write(bDataBotArr);

    byte[] postBody = bDataOutputStream.toByteArray();

    
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
            requestContentType: "application/octet-stream",
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString"
			], 
            body: postBody,
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
            if(debugEnabled) log.debug "writeImageFile ${resp.properties}"
            logTrace resp.data.status 
            return resp.data.success
		}
	}
	catch (e) {
		log.error "Error writing file $fName: ${e}"
	}
	return false
}

@SuppressWarnings('unused')
String deleteFile(fName){
    bodyText = JsonOutput.toJson(name:"$fName",type:"file")
    params = [
        uri: "http://127.0.0.1:8080",
	path: "/hub/fileManager/delete",
        contentType:"text/plain",
        requestContentType:"application/json",
        body: bodyText
        ]
    httpPost(params) { resp ->
        return resp.data.toString()
    }
}

//Logging Functions
def logMessage(String msg) {
    // app
    if (app) {
        return "<span style='color: blue'>${app.name}</span>: ${msg}"
    }
    // device
    return "<span style='color: orange'>${device.name}</span>: ${msg}"
}

void logErr(String msg) {
    if (!app && device.typeName == PARENT_DEVICE_NAME) makeEvent("spaSessionStatus",msg)
    if (logLevelInfo.level>=1) log.error "${logMessage(msg)}"
}
void logWarn(String msg) {
    if (logLevelInfo.level>=2) log.warn "${logMessage(msg)}"
}
void logInfo(String msg) {
    if (!app && device.typeName == PARENT_DEVICE_NAME) makeEvent("spaSessionStatus",msg)
    if (logLevelInfo.level>=3) log.info "${logMessage(msg)}"
}
void logDebug(String msg) {
        if (logLevelInfo.level>=4) log.debug "${logMessage(msg)}"
}
void logTrace(String msg) {
        if (logLevelInfo.level>=5) log.trace "${logMessage(msg)}"
}
