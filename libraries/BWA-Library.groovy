/*******************************************************************
*** SanderSoft - Core App/Device Helpers                        ***
/*******************************************************************/

library (
    base: "app",
    author: "Kurt Sanders",
    category: "Apps",
    description: "Core functions for BWA Spa Manager Suite.",
    name: "BWA-Library",
    namespace: "kurtsanders",
    documentationLink: "https://github.com/KurtSanders/HBBWASpaManager?tab=readme-ov-file#hb-bwa-spamanager",
    version: "0.0.4",
    disclaimer: "This library is only for use with SanderSoft Apps and Drivers."
)

@Field static String AUTHOR_NAME                   = "Kurt Sanders"
@Field static String NAMESPACE                     = "kurtsanders"
@Field static final String COMM_LINK               = "https://community.hubitat.com/t/release-hb-bwa-spamanager-app/128842"
@Field static final String GITHUB_LINK             = "https://github.com/KurtSanders/HBBWASpaManager"

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
            "<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
    } else {
        return "<div style='text-align: center; position: absolue; top: 0px; right: 80px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"

    }
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
    if (app) {
        return "<span style='color: blue'>${app.name}</span>: ${msg}"
    } else {
        def color
        switch (device.typeName) {
         case ~/.*Parent$/:
            color = 'red'
            break
         case ~/.*Switch$/:
            color = 'green'
            break
         case ~/.*Pump$/:
            color = 'purple'
            break
         case ~/.*Thermostat$/:
            color = 'Brown'
            break
         default:
            color = 'orange'
            break
        }
        return "<span style=color:${color}>${device.name}</span>: ${msg}"
    }
}

void logErr(String msg) {
    if (logLevelInfo.level>=1) log.error "${logMessage(msg)}"
}
void logWarn(String msg) {
    if (logLevelInfo.level>=2) log.warn "${logMessage(msg)}"
}
void logInfo(String msg) {
    if (logLevelInfo.level>=3) log.info "${logMessage(msg)}"
}
void logDebug(String msg) {
        if (logLevelInfo.level>=4) log.debug "${logMessage(msg)}"
}
void logTrace(String msg) {
        if (logLevelInfo.level>=5) log.trace "${logMessage(msg)}"
}