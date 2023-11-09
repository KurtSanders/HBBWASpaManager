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
 *                              Added driver to HPM Public Install App
 *
 */

import groovy.transform.Field

@Field static String NAMESPACE = "kurtsanders"

@Field static String SWITCH_CHILD_DEVICE_NAME_PREFIX = "HB BWA SPA Switch"

@Field static String PARENT_DEVICE_NAME = "HB BWA SPA Switch"
@Field static final String VERSION = "1.2.0"

metadata {
    definition (name: SWITCH_CHILD_DEVICE_NAME_PREFIX, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Switch"
        capability "Refresh"

        attribute "switch", "enum", ["on", "off"]
        attribute "balboaAPIButtonNumber", "number"

        command "on"
        command "off"
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
    parent?.refresh()
}