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
    version: "0.0.1",
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

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}
