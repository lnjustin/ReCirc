
/**
 *  ReCirc
 *
*  Copyright 2024 lnjustin
 *
 *
 * For usage information & change log: https://github.com/SANdood/Green-Smart-HW-Recirculator
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * Change Log
 * -----------
 * v.0.1.1 - update momentary relay settings and make delay configurable
 * v.0.1.0 - Beta release
 *
 */

import groovy.transform.Field
 
definition(
	name:		"ReCirc",
	namespace: 	"lnjustin",
	author: 	"lnjustin",
	description: "Controller for Hot Water recirculation system.",
	category: 	"Green Living",
	iconUrl: 	"https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
	iconX2Url:	"https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png"
)

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"
@Field daysOfWeekList = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
@Field daysOfWeekShortMap = ["Sunday":"SUN", "Monday":"MON", "Tuesday":"TUE", "Wednesday":"WED", "Thursday":"THU", "Friday":"FRI", "Saturday":"SAT"]
@Field daysOfWeekMap = ["Sunday":1, "Monday":2, "Tuesday":3, "Wednesday":4, "Thursday":5, "Friday":6, "Saturday":7]
@Field months = ["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"]
@Field monthsMap = [
    "JAN" : 1,
    "FEB" : 2,
    "MAR" : 3,
    "APR" : 4,
    "MAY" : 5,
    "JUN" : 6,
    "JUL" : 7,
    "AUG" : 8,
    "SEP" : 9,
    "OCT" : 10,
    "NOV" : 11,
    "DEC" : 12]
@Field days29 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29]
@Field days30 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30]    
@Field days31 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31]

preferences {
	page( name: "setupApp" )
    page name: "schedulePage", install: false, uninstall: false, nextPage: "setupApp" 
}

def setupApp() {
	dynamicPage(name: "setupApp", title: versionLabel, install: true, uninstall: true) {

		section() {
            header()
            paragraph getInterface("header", " Hot Water Recirculator Physical Control")
            input name: "recircRelay", type: "capability.switch", title: "Physical Relay to toggle the on/off state of the recirculator", multiple: false, required: true
            if (recircRelay) input name: "recircRelayMomentary", type: "bool", title: "Make the Physical Relay a momentary switch?", multiple: false, required: true, submitOnChange: true
            if (recircRelayMomentary) input name: "momentaryDelay", type: "number", title: "Delay before turning off Physical Relay (seconds)", required: true
            input name: "recircSensedState", type: "capability.switch", title: "Switch representing sensed state of the recirculator", multiple: false, required: true
        }
        
		section() {
            paragraph getInterface("header", " Recirculator On-Demand Manual Control")
			input name: "manualOnSwitch", type: "capability.switch", title: "ON Switch: Momentary Switch to manually turn on the recirculator", multiple: false, required: false
            paragraph getInterface("note", "Demanding the recirculator to turn on using this switch will turn the recirculator on even if the Hubitat hub is in a mode in which the recirculator is to be off and even if the recirculator is currently scheduled to be off.")
            input name: "manualOnDuration", type: "number", title: "On Duration (minutes)", required: false, defaultValue: 20, multiple: false, width: 6
            input name: "manualOnCoolDownPeriod", type: "number", title: "Minimum minutes, if any, between manual on periods?", required: false, defaultValue: 0, multiple: false, width: 6
            
            input name: "manualOffSwitch", type: "capability.switch", title: "OFF Switch: Momentary Switch to manually turn off the recirculator", multiple: false, required: false
            paragraph getInterface("note", "Demanding the recirculator to turn off using this switch will turn the recirculator off even if the Hubitat hub is in a mode in which the recirculator is to be on, even if the recirculator is currently scheduled to be on, and even if dynamic triggers would otherwise trigger the recirculator to be on.")
        }

        section() {
            paragraph getInterface("header", " Recirculator Mode Controls")
	        input name: "offModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator off", multiple: true, required: false, width: 6
            if (offModes) input name: "modesTurnOffDelay", type: "number", title: "After how long of a delay, if any, since entering the mode(s) (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            paragraph getInterface("note", "In these modes, the recirculator will remain off, even if the recirculator would otherwise be scheduled to be on and even if dynamic triggers would otherwise trigger the recirculator to turn on."), width: 12
              

	        input name: "onModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator on", multiple: true, required: false, width: 6
            if (offModes) input name: "modesTurnOnDelay", type: "number", title: "After how long of a delay, if any, since entering the mode(s) (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            paragraph getInterface("note", "In these modes, the recirculator will remain on, even if the recirculator would otherwise be scheduled to be off and even if dynamic triggers would otherwise trigger the recirculator to turn off."), width: 12
                    

        }

        section() {
            paragraph getInterface("header", " Recirculator Schedules")
            href(name: "SchedulePage", title: getInterface("boldText", "Configure Time-Based Schedule(s)"), description: getSchedulesDescription() ?: "", required: false, page: "schedulePage", image:  (getSchedulesEnumList() ? checkMark : xMark))
        }

		section() {
            paragraph getInterface("header", " Recirculator Dynamic Trigger events")
            paragraph "Specify events to trigger the recirculator to turn on, if the Hubitat hub is in a mode that allows the recirculator to be on and if the recirculator is not currently scheduled to be off."

            paragraph ""
			input name: "motionSensors", type: "capability.motionSensor", title: "On when motion is detected in any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.motionSensors) {
                input name: "turnOffWhenMotionStops", type: "bool", title: "Off when motion stops everywhere?", defaultValue: false, submitOnChange: true
                if (turnOffWhenMotionStops) input name: "turnOffWhenMotionStopsDelay", type: "number", title: "How long of a delay, if any, until turning off after motion stops (minutes)?", required: true, defaultValue: 0, multiple: false
            }

            paragraph ""
			input name: "arrivePresenceSensors", type: "capability.presenceSensor", title: "On when presence arrives at any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.arrivePresenceSensors) {
                input name: "turnOffWhenAllNotPresent", type: "bool", title: "Off when all presence sensors are not present?", defaultValue: false, submitOnChange: true
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllNotPresentDelay", type: "number", title: "How long of a delay, if any, until turning off after all presence sensors are not present (minutes)?", required: true, defaultValue: 0, multiple: false
            }

            paragraph ""
			input name: "departPresenceSensors", type: "capability.presenceSensor", title: "On when presence departs from any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.departPresenceSensors) {
                input name: "turnOffWhenAllPresent", type: "bool", title: "Off when all presence sensors are present?", defaultValue: false, submitOnChange: true
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllPresentDelay", type: "number", title: "How long of a delay, if any, until turning off after all presence sensors are present (minutes)?", required: true, defaultValue: 0, multiple: false
            }

			paragraph ""
			input name: "openContactSensors", type: "capability.contactSensor", title: "On when any of these things open", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.openContactSensors) {
				input name: "turnOffWhenReclose", type: "bool", title: "Off when all of these things re-close?", defaultValue: false, submitOnChange: true
                if (turnOffWhenReclose) input name: "turnOffWhenRecloseDelay", type: "number", title: "How long of a delay, if any, until turning off after all of these things close (minutes)?", required: true, defaultValue: 0, multiple: false
			}
			
			paragraph ""
			input name: "closeContactSensors", type: "capability.contactSensor", title: "On when any of these things close", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.closeContactSensors) {
				input name: "turnOffWhenReopen", type: "bool", title: "Off when all of these things re-open?", defaultValue: false, submitOnChange: true
                if (turnOffWhenReopen) input name: "turnOffWhenReopenDelay", type: "number", title: "How long of a delay, if any, until turning off after all of these things open (minutes)?", required: true, defaultValue: 0, multiple: false
			}

			paragraph ""
			input name: "onSwitches", type: "capability.switch", title: "On with any of these switches", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.onSwitches) {
				input name: "turnOffWithSwitches", type: "bool", title: "Off when all switches turn off?", defaultValue: false, submitOnChange: true
                if (turnOffWithSwitches) input name: "turnOffWithSwitchesDelay", type: "number", title: "How long of a delay, if any, until turning off recirculator after all switches turn off (minutes)?", required: true, defaultValue: 0, multiple: false
			}

			paragraph ""
			input name: "tempTriggerSensors", type: "capability.temperatureMeasurement", title: "On when outside temperature drops", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.tempTriggerSensors) {
                input name: "onWhenBelowTemp", type: "number", title: "On when at least one sensor falls below this temp", required: true, width: 6
                input name: "offWhenAboveTemp", type: "number", title: "Off when all sensors rise above this temp", required: true, width: 6
			}

            paragraph ""
			input name: "accelerationSensors", type: "capability.accelerationSensor", title: "On when any of these things move", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (accelerationSensors) {
				input name: "turnOffWhenStopsMoving", type: "bool", title: "Off when all stop moving?",  defaultValue: false
                if (turnOffWhenStopsMoving) input name: "turnOffWhenStopsMovingDelay", type: "number", title: "How long of a delay, if any, until turning off recirculator after all stop moving (minutes)?", required: true, defaultValue: 0
            }

			paragraph ""
			input name: "flumeDevice", type: "device.FlumeDevice", title: "On when Flume Device Detects Flow", multiple: false, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.flumeDevice) {
				input name: "turnOffWhenFlowStops", type: "bool", title: "Off when flow stops?", defaultValue: false, submitOnChange: true
                if (turnOffWhenFlowStops) input name: "turnOffWhenFlowStopsDelay", type: "number", title: "How long of a delay, if any, until turning off recirculator after flow stops(minutes)?", required: true, defaultValue: 0, multiple: false
			}

/*
            paragraph ""
            input name: "waterTemperatureSensor", type: "capability.temperatureMeasurement", title: "On when this water temperature sensor is below threshold?", multiple: false, required: false
            if (waterTemperatureSensor) {
                input name: "thresholdWaterTemperature", type: "number", title: "Temperature Threshold", defaultValue: 105, required: true
                input name: "offWhenTempAtTarget", type: "bool", title: "Off at target temp?", defaultValue: true
                if (offWhenTempAtTarget) {
                    input name: "targetWaterTemperature", type: "number", title: "Temperature Target", defaultValue: 120, required: true
                    input name: "offONLYWhenTempAtTarget", type: "bool", title: "Override Max On-Time Duration to Keep On Until Hit Target?", defaultValue: true
                }
            }
            */
            paragraph getInterface("subHeader", " Dynamic Trigger Settings")
            input name: "triggerOnMaxDuration", type: "number", title: "Maximum Duration of triggered on period (minutes)?", required: false, defaultValue: 30, multiple: false, width: 4
            input name: "maxDurationExtendable", type: "bool", title: "Maximum Duration Extendable with Continued Triggers?", defaultValue: false, required: true, width: 4
            input name: "triggerOnCoolDownPeriod", type: "number", title: "Minimum Duration between end of one triggered on period and start of next triggered on period (minutes)?", required: false, defaultValue: 0, multiple: false, width: 4
    
		}

        section() {
            paragraph getInterface("header", " Settings")
            input name: "simulationEnable", type: "bool", title: "Simulate Operation with notifications only?", defaultValue: false, submitOnChange: true
            if (simulationEnable) input name: "simulateNotificationDevices", type: "capability.notification", title: "Simulation Notification Device(s)", required: true, multiple: true
            input name: "notificationDevices", type: "capability.notification", title: "Notify these devices when turned on or off", required: false, multiple: true
            input name: "logEnable", type: "bool", title: "Enable logging", defaultValue: false, submitOnChange: true
            if (logEnable) {
                input name: "logTypes", type: "enum", title: "Log Type(s)", multiple: true, options: ["Trace", "Debug", "Error", "Warning"], submitOnChange: true
                input name: "logTimed", type: "bool", title: "Disable debug logging in 30 minutes?", defaultValue: false, submitOnChange: true
            }
        }

        section() {
            footer()
        }
	}
}


def schedulePage() {
	dynamicPage(name: "schedulePage") {
        section() {
            header()
            paragraph getInterface("header", " Program Schedules")
            paragraph getInterface("note", "") 
            if (state.scheduleMap) {
                logDebug("ScheduleMap: ${state.scheduleMap}", "Debug")
                for (j in state.scheduleMap.keySet()) {
                    paragraph getInterface("subHeaderWithRightLink", (settings["schedule${j}Name"] ?: " New Schedule"), buttonLink("deleteSchedule${j}", "<div style='float:right;vertical-align: middle; margin: 4px; transform: translateY(-30%); top: 50%;'><b><font size=3>Delete</font></b></div><iconify-icon icon='material-symbols:delete-outline'  style='color: #ff2600; top: 50%; transform: translateY(5%); display:inline-block; float:right; vertical-align: middle;'></iconify-icon>","red", 20)) + "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
                    input name: "schedule${j}Name", type: "text", title: "Schedule Name", required: true, width: 12, submitOnChange: true                    

                    input(name:"schedule${j}StartMonth", type:"enum", options:months, title: "Start Month", required: true, width: 2)
                    input(name:"schedule${j}StartDay", type:"enum", options:getNumDaysInMonth(settings["schedule{j}StartMonth"]), title: "Start Day", required: true, width: 2)                            
                    input(name:"schedule${j}StopMonth", type:"enum", options:months, title: "Stop Month", required: true, width: 2)
                    input(name:"schedule${j}StopDay", type:"enum", options:getNumDaysInMonth(settings["schedule{j}StopMonth"]), title: "Stop Day", required: true, width: 2)
                    input name:"schedule${j}DaysOfWeek", type: "enum", title: "Schedule Days of Week", options: daysOfWeekList, multiple: true, required: true, width: 4
                    
                    displayPeriodTable(j)
                    
                    if (state.addingPeriod == j) {
                        def midnight = (new Date().clearTime())
                        input name: "addPeriodStart", type: "time", title: "Start", required: true, width: 2, submitOnChange: true
                        if (state.initializeAddPeriod) app.updateSetting("addPeriodStart",[type:"time",value: midnight]) 
                        input name: "addPeriodEnd", type: "time", title: "End", required: true, width: 2, submitOnChange: true
                        if (state.initializeAddPeriod) app.updateSetting("addPeriodEnd",[type:"time",value: midnight]) 
                        input name: "confirmPeriodToAdd", type: "button", title: "Add", width: 2
                        input name: "cancelPeriodToAdd", type: "button", title: "Cancel", width: 2
                        state.initializeAddPeriod = false
                    }
                    else if (state.editingPeriod != null) {
                        def sched = state.editingPeriod?.schedule
                        def per = state.editingPeriod?.period
                        if (sched == j) {
                            def periodMap = state.scheduleMap[sched][per]
                            input name: "editPeriodStart", type: "time", title: "Start", required: true, width: 2, submitOnChange: true
                            if (state.initializeEditing && periodMap.start) app.updateSetting("editPeriodStart",[type:"time",value: periodMap.start]) 
                            input name: "editPeriodEnd", type: "time", title: "End", required: true, width: 2, submitOnChange: true
                            if (state.initializeEditing && periodMap.end) app.updateSetting("editPeriodEnd",[type:"time",value: periodMap.end]) 
                            input name: "confirmPeriodToEdit", type: "button", title: "Save", width: 2
                            input name: "cancelPeriodToEdit", type: "button", title: "Cancel", width: 2    
                            state.initializeEditing = false     
                        }              
                    }
                    
                }
            }
            paragraph getInterface("line")
            input name: "addSchedule", type: "button", title: "Add Schedule", width: 3  
        }

        section() {
            footer()
        }
    }
}   

String displayPeriodTable(scheduleId) {
	String offIcon = "<iconify-icon icon='material-symbols:circle-outline'  style='color: red'></iconify-icon>"
    String onIcon = "<iconify-icon icon='carbon:circle-filled'  style='color: green'></iconify-icon>"
    String editIcon = "<iconify-icon icon='bx:edit'></iconify-icon>"
    String deleteIcon = "<iconify-icon icon='material-symbols:delete-outline'  style='color: red'></iconify-icon>"

    String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style>"
    
    str += "<div style='overflow-x:auto'>"
    str += "Time Periods"
    str += "<table class='mdl-data-table tstat-col' style=';border:2px solid black'>"
	
	str += "<thead><tr style='border-bottom:2px solid black'>"
	str += "<th>Start</th><th>End</th><th>On/Off</th>"
    str += "<th>$editIcon</th>"
    str += "<th><iconify-icon icon='material-symbols:delete-outline'></iconify-icon></th>"
	str += "</tr></thead>"

	state.scheduleMap[(scheduleId)].eachWithIndex { item, index ->
        str += "<tr style='border-bottom:2px solid black'>"
        str += '<td>' + buttonLink("editPeriod:$scheduleId:$index", item && item.start ? toDateTime(item.start).format("hh:mm a") : "Set Start", "#1A77C9") + '</td>'
        str += '<td>' + buttonLink("editPeriod:$scheduleId:$index", item && item.end ? toDateTime(item.end).format("hh:mm a") : "Set End", "#1A77C9") + '</td>'
        str += '<td>' + buttonLink("toggleOnOff:$scheduleId:$index", (item?.state && item?.state == "on" ? onIcon : offIcon), "#1A77C9") + '</td>'
        str += '<td>' + buttonLink("editPeriod:$scheduleId:$index", editIcon, "#1A77C9") + '</td>'
        str += '<td>' + buttonLink("deletePeriod:$scheduleId:$index", deleteIcon, "#1A77C9") + '</td>'
        str += "</tr>"
	}
	str += "</tr>"

    str += "</table><table class='mdl-data-table tstat-col' style=';border:none'>"
    str += "<tr>"
    str += "<td style='border-left:2px solid black; border-right:2px solid black; border-bottom:2px solid black; border-top:none'>"
    str += buttonLink("addPeriod${scheduleId}", "<iconify-icon icon='zondicons:add-solid'  style='display:inline-block; color: green; vertical-align: middle;'></iconify-icon>","green", 20)
	str += '</td>'
    str += '<td style="border:none; text-align:middle; vertical-align:middle">' + buttonLink("addPeriod${scheduleId}", '<iconify-icon icon="fluent:arrow-left-12-filled" width="25" height="25"  style="color: green; transform: translateY(25%);"></iconify-icon><b><font color=green size=3> Add Time Period</font></b>', 20) + '</td>'
    str += "</tr>"
    str += "</table></div>"
    paragraph str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = 15) {
	"<div style='display:inline-block;vertical-align: middle;' class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div style='display:inline-block;vertical-align:middle'><div class='submitOnChange' onclick='buttonClick(this)' style='display:inline-block;vertical-align: middle;color:$color;cursor:pointer;font-size:${font}px'>$linkText</div></div style='display:inline-block;vertical-align: middle;'><input type='hidden' name='settings[$btnName]' value=''>"
}

String logo(String width='75') {
    return '<img width="' + width + 'px" style="display: block;margin-left: auto;margin-right: auto;margin-top:0px;" border="0" src="' + getLogoPath() + '">'
}

def header() {
    paragraph logo('100')
}

def getLogoPath() {
    return "https://github.com/lnjustin/App-Images/blob/master/ReCirc/reCirc.png?raw=true"
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center"><img width="25px" border="0" src="' + getLogoPath() + '"> &copy; 2024 lnjustin.<br>'
}

void appButtonHandler(btn) {
    switch (btn) {
        case "addSchedule":
            addScheduleIndex()
            break
        case "cancelPeriodToAdd":
            state.addingPeriod = null
            state.editingPeriod = null
            break
    }
    if (btn.contains("deleteSchedule")) {
        deleteScheduleIndex(btn.minus("deleteSchedule"))
    }
    else if (btn.contains("addPeriod")) {
        //   addPeriodIndex(btn.minus("addPeriod"))
        state.addingPeriod = btn.minus("addPeriod") // set addingPeriod state variable to index of schedule to which adding period
        state.initializeAddPeriod = true
    }
    else if (btn.contains("confirmPeriodToAdd")) {
        confirmAddPeriod()
    }
    else if (btn.contains("cancelPeriodToAdd")) {
        state.addingPeriod = null
        state.editingPeriod = null
        state.initializeAddPeriod = false
    }
    else if (btn.contains("confirmPeriodToEdit")) {
        confirmEditPeriod()
    }  
    else if (btn.contains("cancelPeriodToEdit")) {
        state.editingPeriod = null
        state.addingPeriod = null
        state.initializeEditing = false
    }
    else if (btn.contains("editPeriod")) {
        List indices = btn.tokenize(":")
        def sched = indices[1]
        def per = indices[2] as Integer
        state.editingPeriod = [schedule: sched, period: per]
        state.initializeEditing = true
        state.addingPeriod = null
    } 
    else if (btn.contains("toggleOnOff")) {
        List indices = btn.tokenize(":")
        def sched = indices[1]
        def per = indices[2] as Integer
        state.scheduleMap[(sched)][per].state = (state.scheduleMap[(sched)][per].state == "on") ? "off" : "on"
    }
    else if (btn.contains("deletePeriod")) {
        List indices = btn.tokenize(":")
        def sched = indices[1]
        def per = indices[2] as Integer
        state.scheduleMap[(sched)].remove(per)
    }
}

def addScheduleIndex() {
    if (!state.scheduleMap) state.scheduleMap = [:]
    if (state.nextScheduleIndex == null) state.nextScheduleIndex = -1
    state.nextScheduleIndex++
    state.scheduleMap[(state.nextScheduleIndex)] = []
}

def deleteScheduleIndex(indexToDelete) {
    logDebug("deleteScheduleIndex: ${indexToDelete} in scheduleMap: ${state.scheduleMap}", "Debug")
    if (state.scheduleMap != null) state.scheduleMap.remove(indexToDelete)
}

def confirmAddPeriod() {
    def periodMap = [:]
    periodMap.start = settings["addPeriodStart"]
    periodMap.end = settings["addPeriodEnd"]
    periodMap.state = "on"
    state.scheduleMap[(state.addingPeriod)].add(periodMap)    
    state.addingPeriod = null
    state.editingPeriod = null
    state.initializeAddPeriod = false
}

def cancelAddPeriod() {
    state.addingPeriod = null
    state.editingPeriod = null
    state.initializeAddPeriod = false
}

def confirmEditPeriod() {
    def periodMap = [:]
    periodMap.start = settings["editPeriodStart"]
    periodMap.end = settings["editPeriodEnd"]
    periodMap.state = state.scheduleMap[(state.editingPeriod.schedule)][(state.editingPeriod.period)].state
    state.scheduleMap[(state.editingPeriod.schedule)][(state.editingPeriod.period)] = periodMap
    logDebug("periodStart = ${settings["editPeriodStart"]} periodEnd = ${settings["editPeriodEnd"]} edited ScheduleMap = ${state.scheduleMap}", "Debug")
    state.editingPeriod = null
    state.addingPeriod = null
    state.initializeEditing = null
}

def getNumDaysInMonth(month) {
    def days = days31
    if (month && month == "FEB") days = days29
    else if (month && (month == "APR" || month == "JUN" || month == "SEP" || month == "NOV")) days = days30
    return days        
}

def getSchedulesEnumList() {
    def list = []
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            list.add(settings["schedule${j}Name"])
        }
    }
    return list
}

def getSchedulesDescription() {
    def schedules = getSchedulesEnumList()
    def description = ""
    for (def i=1; i <= schedules.size(); i++) {
         description += schedules.get(i-1) 
        if (i != schedules.size()) description += ", "
    }
    return description
}

def installed() {
	logDebug("Installed with settings: ${settings}", "Debug")

	initialize()
}

def updated() {
	logDebug("Updated with settings: ${settings}", "Debug")

	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	logDebug("Initializing", "Trace")

    if (manualOnSwitch) subscribe(manualOnSwitch, "switch.on", manualOnHandler)
    if (manualOffSwitch) subscribe(manualOffSwitch, "switch.on", manualOffHandler)
    
	if (offModes) subscribe(location, locationModeHandler)

    if (state.scheduleMap) scheduleSchedules()

    if (waterTemperatureSensor) subscribe(waterTemperatureSensor, "temperature", waterTempHandler)

    if (motionSensors) {
        subscribe(motionSensors, "motion.active", handleTriggerOnEvent)
        if (turnOffWhenMotionStops) subscribe(motionSensors, "motion.inactive", handleMotionOff)
    }

    if (arrivePresenceSensors) {
        subscribe(arrivePresenceSensors, "presence.present", handleTriggerOnEvent)
        if (turnOffWhenAllNotPresent) subscribe(arrivePresenceSensors, "presence.notPresent", handleArrivePresenceOff)
    }

    if (departPresenceSensors) {
        subscribe(departPresenceSensors, "presence.notPresent", handleTriggerOnEvent)
        if (turnOffWhenAllPresent) subscribe(departPresenceSensors, "presence.present", handleDepartPresenceOff)
    }

    if (openContactSensors) {
        subscribe(openContactSensors, "contact.open", handleTriggerOnEvent)
        if (turnOffWhenReclose) subscribe(openContactSensors, "contact.closed", handleOpenContacteOff)
    }

    if (closeContactSensors) {
        subscribe(closeContactSensors, "contact.closed", handleTriggerOnEvent)
        if (turnOffWhenReopen) subscribe(closeContactSensors, "contact.open", handleCloseContactOff)
    }

    if (onSwitches) {
        subscribe(onSwitches, "switch.on", handleTriggerOnEvent)
        if (turnOffWithSwitches) subscribe(onSwitches, "switch.off", handleOnSwitchOff)
    }

    if (tempTriggerSensors) {
        subscribe(tempTriggerSensors, "temperature", triggerTempHandler)
    }

    if (accelerationSensors) {
        subscribe(accelerationSensors, "acceleration.active", handleTriggerOnEvent)
        if (turnOffWhenStopsMoving) subscribe(accelerationSensors, "acceleration.inactive", handleAccelerationOff)
    }

    if (flumeDevice) {
        subscribe(flumeDevice, "flowStatus.running", handleTriggerOnEvent)
        if (turnOffWhenFlowStops) subscribe(flumeDevice, "flowStatus.stopped", handleFlumeOff)
    }

    state.simulatedRecirculatorState = "off"
	
    initializeDebugLogging()
}

def initializeDebugLogging() {
    if (logEnable && logTimed) runIn(1800, disableDebugLogging)
}

def disableDebugLogging() {
    logDebug("Disabling Debug Logging", "Trace")
    app.updateSetting("logEnable",[value:"false",type:"bool"])
    app.updateSetting("logTimed",[value:"false",type:"bool"])
}

def scheduleSchedules() {
    for (j in state.scheduleMap.keySet()) {
        
        def daysOfWeek = settings["schedule${j}DaysOfWeek"]
        for (i=0; i<daysOfWeek.size(); i++) {
            daysOfWeek[i] = daysOfWeekShortMap[daysOfWeek[i]]
        }
        def daysOfWeekChron = daysOfWeek.join(",")
        logDebug("Scheduling schedule ${settings["schedule${j}Name"]} for ${settings["schedule${j}DaysOfWeek"]}", "Debug")
        // schedule update for beginning and end of each time window, on selected days of the week, but irrespective of dates. When triggered, will check if current day is within the date window at that time and ignore if not
        state.scheduleMap[(j)].eachWithIndex { item, index ->
            def start = toDateTime(item?.start)
            def startHour = start.format("H")
            def startMin = start.format("m")
            def startChron = "0 ${startMin} ${startHour} ? * ${daysOfWeekChron}"
            schedule(startChron, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
            logDebug("Scheduled start of period ${index} with chron string ${startChron}", "Debug")

            def end = toDateTime(item?.end)
            def endHour = end.format("H")
            def endMin = end.format("m")
            def endChron = "0 ${endMin} ${endHour} ? * ${daysOfWeekChron}"
            schedule(endChron, handlePeriodStop, [data: [scheduleId: j, periodId: index], overwrite: false])
            logDebug("Scheduled end of period ${index} with chron string ${endChron}", "Debug")
        }
    }
}

def turnRecirculatorOn() {
    if (!isRecirculatorOn()) {
        if (settings["simulationEnable"]) {
            state.simulatedRecirculatorState = "on"
            simulateNotificationDevices?.deviceNotification("Simulation: Recirculator On")
        }
        else {
            recircRelay.on()
            if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
            notificationDevices?.deviceNotification("Recirculator On")
        }
        state.lastOnStart = (new Date()).getTime() 
    }
}

def turnRecirculatorOff() {
    if (!isRecirculatorOff()) {
        if (settings["simulationEnable"]) {
            state.simulatedRecirculatorState = "off"
            simulateNotificationDevices?.deviceNotification("Simulation: Recirculator Off")
        }
        else {
            recircRelay.on()
            if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
            notificationDevices?.deviceNotification("Recirculator Off")
        }
        unschedule("delayedOffHandler")
        logDebug("Turned off recirculator and canceled any pending delayed turning off of the recirculator.", "Debug")
    }
}

def makeRelayMomentary() {
    recircRelay.off()
}

def manualOnHandler(evt) {     
    if (!isRecirculatorOn()) {
        def coolDownPeriodSecs = manualOnCoolDownPeriod ? (manualOnCoolDownPeriod.toInteger() * 60) : 60
        if (state.lastManualOnTime) {
            hasCoolDownPeriodPast = haveSecondsPast(state.lastManualOnTime, coolDownPeriodSecs)
            if (hasCoolDownPeriodPast == false) {
                def minsLeft = manualOnCoolDownPeriod - howManyMinsPastSince(state.lastManualOnTime)
                logDebug("Minimum number of minutes between manual on periods not met. Not triggering recirculator. Try again in ${minsLeft} minutes.", "Warn")
                return
            }
        }
        turnRecirculatorOn()
        logDebug("Manually turned recirculator on.", "Debug")
        state.lastManualOnTime = (new Date()).getTime()  
        if (settings["manualOnDuration"] && settings["manualOnDuration"]) runIn((settings["manualOnDuration"] * 60), "manualOnTimeout", [overwrite: true])
    }
    else {
        logDebug("Attempt to manually turn on recirculator detected, but recirculator is already on. Nothing to do.", "Warn")
    }
}

def manualOnTimeout() {
    state.lastManualOnTime = null
    turnRecirculatorOff()
    logDebug("Max duration reached for manually turned on recirculator. Turned off.", "Debug")
}

def manualOffHandler(evt) {
    if (!isRecirculatorOff()) {
        turnRecirculatorOff()
        logDebug("Manually turned recirculator off.", "Debug")
    }
    else logDebug("Attempt to manually turn off recirculator detected, but recirculator is already off. Nothing to do.", "Warning")
}

def isRecirculatorOn() {
    if (settings["simulationEnable"] == true) return state.simulatedRecirculatorState == "on"
    else return recircSensedState.currentSwitch == "on"
}

def isRecirculatorOff() {
    if (settings["simulationEnable"] == true) return state.simulatedRecirculatorState == "off"
    else return recircSensedState.currentSwitch == "off"
}

def delayedModeOffHandler() {
    // delay in turning recirculator off may have changed the situation. Before turn it off, check again to make sure it is still ok to turn off.
    if (doesModeAllowRecirculatorOff()) {
        turnRecirculatorOff()
        logDebug("Recirculator turned off after having waited for some time since the mode was changed.", "Debug")
    }
    else logDebug("Situation has changed since delaying turning off of the recirculator. Not turning off yet.", "Debug")
}

def delayedModeOnHandler() {
    // delay in turning recirculator on may have changed the situation. Before turn it on, check again to make sure it is still ok to turn on.
    if (doesModeAllowRecirculatorOn()) turnRecirculatorOn()
    else logDebug("Situation has changed since delaying turning on of the recirculator. Not turning on yet.", "Debug")
}

def locationModeHandler(evt) {
	if (onModes) {
        if (evt.value in onModes) {
            if (settings["modesTurnOnDelay"] > 0) runIn((settings["modesTurnOnDelay"]*60), delayedTurnRecirculatorOn, [overwrite: false])
            else turnRecirculatorOn()
		}
    }
	if (offModes) {
        if (evt.value in offModes) {
            if (settings["modesTurnOffDelay"] > 0) runIn((settings["modesTurnOffDelay"]*60), delayedTurnRecirculatorOff, [overwrite: false])
            else turnRecirculatorOff()
		}
    }
}

Boolean doesModeAllowRecirculatorOn() {
    def answer = true
    if (settings["offModes"] && location?.currentMode in settings["offModes"]) answer = false
    return answer
}

Boolean doesModeAllowRecirculatorOff() {
    def answer = true
    if (settings["onModes"] && location?.currentMode in settings["onModes"]) answer = false
    return answer
}


// SCHEDULES
def handlePeriodStart(data) {
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    if (!isTodayWithinScheduleDates(scheduleId)) return // schedule not applicable to today's date
    if (!isScheduledDayofWeek(scheduleId)) return // schedule not applicable to today's day of the week

    // scheduled period start is applicable for today, so handle accordingly
    if (state.scheduleMap[(scheduleId)] && state.scheduleMap[(scheduleId)][periodId] && state.scheduleMap[(scheduleId)][periodId].state == "on") {
        // turn recirculator on unless Hubitat mode dictates that the recirculator be off, no matter dynamic triggers (prioritizes time schedule over dynamic triggers)
        if (recircSensedState.currentSwitch != "on") {
            if (doesModeAllowRecirculatorOn()) turnRecirculatorOn()
            else logDebug("Recirculator is scheduled to turn on now, but mode does not allow it to be turned on. Nothing to do.", "Debug")
        }
        else logDebug("Recirculator is scheduled to turn on now, but recirculator is already on. Nothing to do.", "Debug")
    }
    else if (state.scheduleMap[(scheduleId)] && state.scheduleMap[(scheduleId)][periodId] && state.scheduleMap[(scheduleId)][periodId].state == "off") {
        // turn recirculator off unless Hubitat mode dictates that the recirculator be on, no matter dynamic triggers (prioritizes time schedule over dynamic triggers)
        if (recircSensedState.currentSwitch != "off") {
            if (doesModeAllowRecirculatorOff()) turnRecirculatorOff()
            else logDebug("Recirculator is scheduled to turn off now, but mode does not allow it to be turned off. Nothing to do.", "Debug")
        }
        else logDebug("Recirculator is scheduled to turn off now, but recirculator is already off. Nothing to do.", "Debug")

    }
}

def handlePeriodStop(data) {
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    if (!isTodayWithinScheduleDates(scheduleId)) return // schedule not applicable to today's date
    if (!isScheduledDayofWeek(scheduleId)) return // schedule not applicable to today's day of the week

    // scheduled period stop is applicable for today, so scheduled period has now ended. Update from trigger state
    updateFromTriggerState()
}

def isTimeOfDayWithinPeriod(scheduleId, periodId) {
    def answer = false
    def start = toDateTime(state.scheduleMap[(scheduleId)][periodId].start)
    def end = toDateTime(state.scheduleMap[(scheduleId)][periodId].end)
    if (start && end && timeOfDayIsBetween(start, end, new Date(), location.timeZone)) answer = true
    return answer
}

Boolean doSchedulesAllowRecirculatorState(onOrOffState) {
    def answer = true
    def compareState = ""
    if (onOrOffState == "On") compareState = "Off"
    else if (onOrOffState == "Off") compareState = "On"
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(k)) {
                // schedule applicable to today
                state.scheduleMap[(j)].eachWithIndex { item, index ->
                    // check if time periods of schedule allow for recirculator to be on right now
                    if (isTimeOfDayWithinPeriod(j, index)) {
                        if (state.scheduleMap[(j)][index].state == compareState) answer = false
                    }
                }
            }
        }
    }
    return answer
}

def isWithinTime(time1, time2) {
    def withinTime = false
    
    def windowStart = new Date(time1)
    def windowEnd = new Date(time2)
    if (timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
        withinTime = true
    }
    return withinTime
}

def isTodayWithinScheduleDates(scheduleId) {
    def withinDates = false
    def today = timeToday(null, location.timeZone)
	def month = today.month+1
	def day = today.date
    
    def startMonth = settings["schedule${scheduleId}StartMonth"]
    def startDay = settings["schedule${scheduleId}StartDay"]
    def stopMonth = settings["schedule${scheduleId}StopMonth"]
    def stopDay = settings["schedule${scheduleId}StopDay"]

    def sMonth = monthsMap[startMonth]
    def sDay = startDay.toInteger()
    def eMonth = monthsMap[stopMonth]
    def eDay = stopDay.toInteger()
        
    if ((sMonth != null && sDay != null) && ((month == sMonth && day >= sDay) || month > sMonth))  {
        if ((eMonth != null && eDay != null) && ((month == eMonth && day <= eDay) || month < eMonth)) {
		     withinDates = true
        }
    }
    return withinDates
}

def isScheduledDayofWeek(scheduleId) {
    def answer = false    
    def dayOfWeek = (new Date()).format('EEEE') 
    if(settings["schedule${scheduleId}DaysOfWeek"] && settings["schedule${scheduleId}DaysOfWeek"].contains(dayOfWeek)) answer = true 
    return answer
}

def getDayOfWeek(Date date) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(date)
    def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)    
    logDebug("Converted ${date} to day of week = ${dayOfWeek}")
    return dayOfWeek
}

// DYNAMIC TRIGGERS
def handleTriggerOnEvent(evt) {
    logDebug("handleTriggerTriggered() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
    if (doesModeAllowRecirculatorOn() && doSchedulesAllowRecirculatorState("On")) {
        if (isRecirculatorOff() || (isRecirculatorOn() && settings["maxDurationExtendable"] == true)) {
            // trigger on with device either if recirculator is off or if recirculator is on but the on duration period should be extended by this device trigger|
            logDebug("${evt.device?.label} (${evt.device?.id}) triggered on period. Cancelled any pending delayed turn off of recirculator.", "Debug")
            unschedule("delayedOffHandler")
            triggerOn()
        }
        else logDebug("${evt.device?.label} triggered on period but recirculator is already on and maximum duration of the on period is not configured in app settings to extend the maximum on period duration", "Debug")
    }
    else logDebug("${evt.device?.label} triggered on period but the Hubitat mode or the recirculator schedule does not allow the recirculator to turn on", "Debug")
}

def handleMotionOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenMotionStopsDelay"] ?: 0)
}

def handleArrivePresenceOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenAllNotPresentDelay"] ?: 0)
}

def handleDepartPresenceOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenAllPresentDelay"] ?: 0)
}

def handleOpenContacteOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenRecloseDelay"] ?: 0)
}

def handleCloseContactOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenReopenDelay"] ?: 0)
}

def handleOnSwitchOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWithSwitchesDelay"] ?: 0)
}

def handleAccelerationOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenStopsMovingDelay"] ?: 0)
}

def handleFlumeOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenFlowStopsDelay"] ?: 0)
}

def handleTriggerOffEvent(evt, turnOffDelay) {
    logDebug("handleTriggerOffEvent() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
    if (doesModeAllowRecirculatorOff() && doSchedulesAllowRecirculatorState("Off")) {
        if (isRecirculatorOn()) {
            if (turnOffDelay > 0) {
                runIn(turnOffDelay * 60, "delayedOffHandler", [overwrite: false, data: [device: "${evt.device?.label}"]])
                logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period after delay. Scheduled recirculator to turn off in " + turnOffDelay + " minutes.", "Debug")
            }
            else triggerOff()
        }
        else logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period but recirculator is already off. Nothing to do.", "Debug")
    }
    else logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period but the Hubitat mode or the recirculator schedule now does not allow the recirculator to turn off", "Debug")
}

def delayedOffHandler(data) {
    logDebug("Executing delayeOffHandler() triggered by ${data.device}...", "Trace")
    if (doesModeAllowRecirculatorOff() && doSchedulesAllowRecirculatorState("Off")) {
        // trigger off could be from a delayed action, so make sure that mode and schedule still allow to turn off
        triggerOff()
    }
    else logDebug("Delayed turn off triggered by ${data.device} but the Hubitat mode or the recirculator schedule now does not allow the recirculator to turn off. Keeping on.", "Debug")
}

def triggerOn() {   
    logDebug("Executing triggerOn()...", "Trace")
    if (isRecirculatorOff() && state.lastTriggeredPeriodEndedAt) {
        def coolDownPeriodSecs = triggerOnCoolDownPeriod ? (triggerOnCoolDownPeriod.toInteger() * 60) : 60
        def hasCoolDownPeriodPast = haveSecondsPast(state.lastTriggeredPeriodEndedAt, coolDownPeriodSecs)
        if (hasCoolDownPeriodPast == false) {
            def minsLeft = triggerOnCoolDownPeriod.toInteger() - howManyMinsPastSince(state.lastTriggeredPeriodEndedAt)
            logDebug("Minimum number of minutes between dynamically triggered on periods not met (${minsLeft} minutes left in cooldown period). Not triggering recirculator, but will check again in ${minsLeft} minutes.", "Warning")
            runIn(minsLeft * 60, updateFromTriggerState)
            return
        }
    }
    
    if (isRecirculatorOff()) state.lastTriggerOnStart = (new Date()).getTime() // time when the triggered on period starts, no matter if other triggers occur during the period
    state.lastTriggeredOnAt = (new Date()).getTime()  // last time a trigger occurred, either at the start of the on period or in the middle of it
    
    turnRecirculatorOn() // will only turn on if already off

    if (settings["triggerOnMaxDuration"] && settings["triggerOnMaxDuration"]) runIn((settings["triggerOnMaxDuration"] * 60), "handleTriggerOnMaxDurationReached", [overwrite: true])
    // because triggerOn is called if the recirculator is already on and maxDurationExtendable == true, then this call to overwrite any existing triggerOnTimeout method will extend the max duration on period, to be the max duration on period from the start of the latest trigger
}

def handleTriggerOnMaxDurationReached() {
    logDebug("Executing handleTriggerOnMaxDurationReached() ...", "Trace")
    if (doesModeAllowRecirculatorOff() && doSchedulesAllowRecirculatorState("Off")) triggerOff(true)
    else logDebug("Triggered on period timed out with max on duration, but now Hubitat mode or recirculator time schedule dictates that the recirculator be on. Leaving recirculator on.", "Debug")
}

def triggerOff(maxDurationReached = false) {
    logDebug("Executing triggerOff()...", "Trace")
    if (maxDurationReached || areAllTriggersOff()) {
        if (!isRecirculatorOff()) state.lastTriggeredPeriodEndedAt = (new Date()).getTime()
        turnRecirculatorOff()
    }
    else logDebug("Triggerd off, but either max duration is not reached or all triggers are not off. Keeping on.", "Debug")
}

def areAllTriggersOff() {
    answer = true
    def activeTriggers = []

    def list = motionSensors?.findAll { it?.latestValue("motion") == "active" }
	if (list && list.size() > 0) activeTriggers += list
    
    list = arrivePresenceSensors?.findAll { it?.latestValue("presence") == "present" }
	if (list && list.size() > 0) activeTriggers += list

    list = departPresenceSensors?.findAll { it?.latestValue("presence") == "not present" }
	if (list && list.size() > 0) activeTriggers += list

    list = openContactSensors?.findAll { it?.latestValue("contact") == "open" }
	if (list && list.size() > 0) activeTriggers += list
    
    list = closeContactSensors?.findAll { it?.latestValue("contact") == "closed" }
	if (list && list.size() > 0) activeTriggers += list
    
    list = onSwitches?.findAll { it?.latestValue("switch") == "on" }
	if (list && list.size() > 0) activeTriggers += list

    list = tempTriggerSensors?.findAll { it?.latestValue("temperature") < onWhenBelowTemp }
    if (list && list.size() > 0) activeTriggers += list

    if (flumeDevice?.latestValue("flowStatus") == "running") activeTriggers += flumeDevice

    if (activeTriggers.size() > 0) {
        answer = false
        logDebug("Active Triggers: " + activeTriggers, "Debug")
    }

    return answer
}

def triggerTempHandler(evt) {
    def anyBelowLowList = tempTriggerSensors?.findAll { it?.latestValue("temperature") < onWhenBelowTemp }
    def anyBelowHighList = tempTriggerSensors?.findAll { it?.latestValue("temperature") <= offWhenAboveTemp }
	
    if (anyBelowLowList && anyBelowLowList.size() > 0) {
        // at least one temp sensor falls below the lower threshold
        if (state.lowTemp == null || state.lowTemp == false) handleTriggerOnEvent(evt) // trigger on if it's the first time temp falls below threshold
        state.lowTemp = true
    }
    else if (state.lowTemp == true && anyBelowHighList && anyBelowHighList.size() == 0) {
        // after at least one temp sensor falls below the lower threshold, now all temp sensors have risen above the upper threshold
            state.lowTemp = false
            handleTriggerOffEvent(evt, 0)
    }
}

def updateFromTriggerState() {
    if (isRecirculatorOff() && !areAllTriggersOff() && doesModeAllowRecirculatorOn() && doSchedulesAllowRecirculatorState("On")) {
        // check current state of trigger devices and turn recirculator on if state of trigger devices indicates it should be on and mode allows it to be on
        triggerOn()
    }
    else if (isRecirculatorOn() && areAllTriggersOff() && doesModeAllowRecirculatorOff() && doSchedulesAllowRecirculatorState("Off")) {
        triggerOff()
    }
}

private Boolean haveSecondsPast(timestamp, seconds=1) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.getTime()
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().getTime() - timestamp) > (seconds * 1000)
}

private def howManyMinsPastSince(timestamp) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.getTime()
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().getTime() - timestamp) / (1000 * 60)
}

def getInterface(type, txt="", link="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "error": 
            return "<div style='color:#ff0000;font-weight: bold;'>${txt}</div>"
            break
        case "note": 
            return "<div style='color:#333333;font-size: small;'>${txt}</div>"
            break
        case "subField":
            return "<div style='color:#000000;background-color:#ededed;'>${txt}</div>"
            break     
        case "subHeader": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "subHeaderWithRightLink": 
            return "<div style='align-items:center; top: 50%; color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'><div style='display:inline-block;'> ${txt}</div><div style='float:right; display:inline-block;  text-align:center; top: 50%; vertical-align:middle'>${link}</div></div>"
            break
        case "subSection1Start": 
            return "<div style='color:#000000;background-color:#d4d4d4;border: 0px solid'>"
            break
        case "subSection2Start": 
            return "<div style='color:#000000;background-color:#e0e0e0;border: 0px solid'>"
            break
        case "subSectionEnd":
            return "</div>"
            break
        case "boldText":
            return "<b>${txt}</b>"
            break
        case "boldRedText":
            return "<b><font color=red>${txt}</font></b>"
            break
        case "boldGreenText":
            return "<b><font color=green>${txt}</font></b>"
            break
        case "link":
            return '<a href="' + link + '" target="_blank" style="color:#51ade5">' + txt + '</a>'
            break
    }
} 

def logDebug(msg, type="All") 
{
    if (logEnable && (logTypes == null || logTypes == "All" || logTypes.contains(type)))
    {
        log.debug(msg)
    }
}

