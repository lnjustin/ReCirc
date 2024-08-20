
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
 * v.0.2.6 - reverted to stateless recirc on/off control while maintaining water temp substate
 * v.0.2.5 - added ability to prioritize a schedule over select mode(s)
 * v.0.2.4 - fix shorter delayed off from triggering off when still have longer delayed off pending
 * v.0.2.3 - bug fixes
 * v.0.2.2 - make sensed state switch optional
 * v.0.2.1 - bug fixes; early version of water temp sensor handling
 * v.0.2.0 - bug fixes with modes and schedules; optimized to unsubscribe from trigger events if mode or schedule active; added support for schedules that wrap around to new year
 * v.0.1.2 - bug fix with schedules
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
            input name: "recircSensedState", type: "capability.switch", title: "Switch representing sensed state of the recirculator", multiple: false, required: false
            paragraph getInterface("note", " If the physical relay is a momentary switch, such as with a dry contact relay, specifying a sensed state switch will safeguard against undesirable toggling if the relay state gets out of sync with the recirculator.")
        }
        
		section() {
            paragraph getInterface("header", " Recirculator On-Demand Manual Control")
			input name: "manualOnSwitch", type: "capability.switch", title: "ON Switch: Momentary Switch to manually turn on the recirculator", multiple: false, required: false
            paragraph getInterface("note", "Demanding the recirculator to turn on using this switch will turn the recirculator on even if the Hubitat hub is in a mode in which the recirculator is to be off and even if the recirculator is currently scheduled to be off.")
            input name: "manualOnDuration", type: "number", title: "On Duration (minutes)", required: false, defaultValue: 20, multiple: false, width: 6
            input name: "manualOnCoolDownPeriod", type: "number", title: "Minimum off duration, if any, before can manually turn on", required: false, defaultValue: 0, multiple: false, width: 6
            
            input name: "manualOffSwitch", type: "capability.switch", title: "OFF Switch: Momentary Switch to manually turn off the recirculator", multiple: false, required: false
            paragraph getInterface("note", "Demanding the recirculator to turn off using this switch will turn the recirculator off even if the Hubitat hub is in a mode in which the recirculator is to be on, even if the recirculator is currently scheduled to be on, and even if dynamic triggers would otherwise trigger the recirculator to be on.")
        }

        section() {
            paragraph getInterface("header", " Recirculator Mode Controls")
	        input name: "offModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator off", multiple: true, required: false, width: 6
            if (offModes) input name: "modesTurnOffDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            paragraph getInterface("note", "In these modes, the recirculator will remain off, even if the recirculator would otherwise be scheduled to be on and even if dynamic triggers would otherwise trigger the recirculator to turn on."), width: 12
              

	        input name: "onModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator on", multiple: true, required: false, width: 6
            if (offModes) input name: "modesTurnOnDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
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
                input name: "motionSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "turnOffWhenMotionStops", type: "bool", title: "Off when motion stops everywhere?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenMotionStops) input name: "turnOffWhenMotionStopsDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
            }

            paragraph ""
			input name: "arrivePresenceSensors", type: "capability.presenceSensor", title: "On when presence arrives at any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.arrivePresenceSensors) {
                input name: "arrivePresenceSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "turnOffWhenAllNotPresent", type: "bool", title: "Off when all presence sensors are not present?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllNotPresentDelay", type: "number", title: "After how long of a delay, if any  (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
            }

            paragraph ""
			input name: "departPresenceSensors", type: "capability.presenceSensor", title: "On when presence departs from any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.departPresenceSensors) {
                input name: "departPresenceSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "turnOffWhenAllPresent", type: "bool", title: "Off when all presence sensors are present?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllPresentDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
            }

			paragraph ""
			input name: "openContactSensors", type: "capability.contactSensor", title: "On when any of these things open", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.openContactSensors) {
                input name: "openContactSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
				input name: "turnOffWhenReclose", type: "bool", title: "Off when all of these things re-close?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenReclose) input name: "turnOffWhenRecloseDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}
			
			paragraph ""
			input name: "closeContactSensors", type: "capability.contactSensor", title: "On when any of these things close", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.closeContactSensors) {
                input name: "closeContactSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
				input name: "turnOffWhenReopen", type: "bool", title: "Off when all of these things re-open?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenReopen) input name: "turnOffWhenReopenDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}

			paragraph ""
			input name: "onSwitches", type: "capability.switch", title: "On with any of these switches", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.onSwitches) {
                input name: "onSwitchesPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
				input name: "turnOffWithSwitches", type: "bool", title: "Off when all switches turn off?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWithSwitches) input name: "turnOffWithSwitchesDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}

			paragraph ""
			input name: "customDevices1", type: "capability.*", title: "On when a specified attribute of any of these devices changes to a specified value", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
            paragraph getInterface("note", "Example use: Dishwasher smart plug device using 'Zooz Power Switch with States' driver to specify when dishwasher is idle or running. If select multiple devices, all devices must have the same attribute.")
			if (settings.customDevices1) {
                def enumOptions = getEnumOptions(settings["customDevices1"])
                input name: "customDevice1Attribute", type: "enum", title: "Select attribute...", options: enumOptions, multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice1AttributeValue", type: "string", title: "On when attribute value change to:", multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice1Priority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "turnOffWithCustomDevice1", type: "bool", title: "Off when the specified attribute of all of the devices changes away from the specific value?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWithCustomDevice1) input name: "turnOffWithCustomDevice1Delay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}

			paragraph ""
			input name: "customDevices2", type: "capability.*", title: "On when a specified attribute of any of these devices changes to a specified value", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
            paragraph getInterface("note", "Example use: Washing Machine smart plug device using 'Zooz Power Switch with States' driver to specify when washing machine is idle or running. If select multiple devices, all devices must have the same attribute.")
			if (settings.customDevices2) {
                def enumOptions = getEnumOptions(settings["customDevices2"])
                input name: "customDevice2Attribute", type: "enum", title: "Select attribute...", options: enumOptions, multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice2AttributeValue", type: "string", title: "On when attribute value change to:", multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice2Priority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "turnOffWithCustomDevice2", type: "bool", title: "Off when the specified attribute of all of the devices changes away from the specific value?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWithCustomDevice2) input name: "turnOffWithCustomDevice2Delay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}

			paragraph ""
			input name: "tempTriggerSensors", type: "capability.temperatureMeasurement", title: "On when outside temperature drops", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.tempTriggerSensors) {
                input name: "tempTriggerSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
                input name: "onWhenBelowTemp", type: "number", title: "On when at least one sensor falls below this temp", required: true, width: 6
                input name: "offWhenAboveTemp", type: "number", title: "Off when all sensors rise above this temp", required: true, width: 6
			}

            paragraph ""
			input name: "accelerationSensors", type: "capability.accelerationSensor", title: "On when any of these things move", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (accelerationSensors) {
                input name: "accelerationSensorsPriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
				input name: "turnOffWhenStopsMoving", type: "bool", title: "Off when all stop moving?",  defaultValue: false
                if (turnOffWhenStopsMoving) input name: "turnOffWhenStopsMovingDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0
            }

			paragraph ""
			input name: "flumeDevice", type: "device.FlumeDevice", title: "On when Flume Device Detects Flow", multiple: false, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.flumeDevice) {
                input name: "flumePriority", type: "enum", title: "Priority", options: ["low":"Below Modes and Schedules", "medium":"Below Modes, Above Schedules", "high":"Above Modes and Schedules"], defaultValue: "low", required: false, multiple: false, width: 4
                paragraph "", width: 8
				input name: "turnOffWhenFlowStops", type: "bool", title: "Off when flow stops?", defaultValue: false, submitOnChange: true, width: 5
                if (turnOffWhenFlowStops) input name: "turnOffWhenFlowStopsDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 7
			}

            paragraph getInterface("subHeader", " Dynamic Trigger Settings")
            input name: "triggerOnMaxDuration", type: "number", title: "Maximum Duration of triggered on period (minutes)?", required: false, defaultValue: 30, multiple: false, width: 4
            input name: "maxDurationExtendable", type: "bool", title: "Maximum Duration Extendable with Continued Triggers?", defaultValue: false, required: true, width: 4
            input name: "triggerOnCoolDownPeriod", type: "number", title: "Minimum Duration between end of one triggered on period and start of next triggered on period (minutes)?", required: false, defaultValue: 0, multiple: false, width: 4
    
		}

        section() {
            
            paragraph getInterface("header", " Water Temperature Control")
            paragraph getInterface("note", " When set to recirculate, some recirculation systems automatically turn on/off as needed to keep the water hot without needlessly staying on continuously. If your recriculation system does not do this automatically, you can use the controls below to do it via Hubitat. Be sure to set parameters in a way that will avoid ping-pong of on/off state.")
            input name: "waterTempControlType", type: "enum", options: ["singleValue" : "Value of single temp sensor", "differenceValue" : "Temp Sensor 2 - Temp Sensor 1"], title: "Control Based On...", required: false
            if (waterTempControlType == "singleValue") {
                input name: "waterTempSensor1", type: "capability.temperatureMeasurement", title: (waterTempControlType == "singleValue") ? "Water Temperature Sensor" : "Water Temperature Sensor 1", multiple: false, required: false
                input name: "turnOffTemp", type: "number", title: "Turn Off When Temp Reaches", defaultValue: 120, required: true
                input name: "turnOnTemp", type: "number", title: "Turn On When Temp Falls To", defaultValue: 100, required: true
            }
            else if (waterTempControlType == "differenceValue") {
                input name: "waterTempSensor1", type: "capability.temperatureMeasurement", title: (waterTempControlType == "singleValue") ? "Water Temperature Sensor" : "Water Temperature Sensor 1", multiple: false, required: false
                input name: "waterTempSensor2", type: "capability.temperatureMeasurement", title: "Water Temperature Sensor 2", multiple: false, required: true
                input name: "turnOnDiffTemp", type: "number", title: "Turn On When Temp Difference Reaches", defaultValue: 20, required: true
                input name: "turnOffDiffTemp", type: "number", title: "Turn Off When Temp Difference Falls To", defaultValue: 0, required: true
            }
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
                for (j in state.scheduleMap.keySet()) {
                    paragraph getInterface("subHeaderWithRightLink", (settings["schedule${j}Name"] ?: " New Schedule"), buttonLink("deleteSchedule${j}", "<div style='float:right;vertical-align: middle; margin: 4px; transform: translateY(-30%); top: 50%;'><b><font size=3>Delete</font></b></div><iconify-icon icon='material-symbols:delete-outline'  style='color: #ff2600; top: 50%; transform: translateY(5%); display:inline-block; float:right; vertical-align: middle;'></iconify-icon>","red", 20)) + "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
                    input name: "schedule${j}Name", type: "text", title: "Schedule Name", required: true, width: 12, submitOnChange: true                    

                    input(name:"schedule${j}StartMonth", type:"enum", options:months, title: "Start Month", required: true, width: 2)
                    input(name:"schedule${j}StartDay", type:"enum", options:getNumDaysInMonth(settings["schedule{j}StartMonth"]), title: "Start Day", required: true, width: 2)                            
                    input(name:"schedule${j}StopMonth", type:"enum", options:months, title: "Stop Month", required: true, width: 2)
                    input(name:"schedule${j}StopDay", type:"enum", options:getNumDaysInMonth(settings["schedule{j}StopMonth"]), title: "Stop Day", required: true, width: 2)
                    input name:"schedule${j}DaysOfWeek", type: "enum", title: "Schedule Days of Week", options: daysOfWeekList, multiple: true, required: true, width: 4
                    input name:"schedule${j}DeprioritizedModes", type: "mode", title: "Prioritize Schedule Over These Modes...", options: location?.getModes(), multiple: true, required: false, width: 12
                    
                    displayPeriodTable(j)
                    
                    if (state.addingPeriod == j) {
                        def midnight = (new Date().clearTime())
                        input name: "addPeriodStart", type: "time", title: "Start", required: true, width: 2, submitOnChange: true
                        if (state.initializeAddPeriod) app.updateSetting("addPeriodStart",[type:"time",value: midnight]) 
                        input name: "addPeriodEnd", type: "time", title: "End", required: true, width: 2, submitOnChange: true
                        if (state.initializeAddPeriod) app.updateSetting("addPeriodEnd",[type:"time",value: midnight]) 
                        input name: "confirmPeriodToAdd", type: "button", title: "Add", width: 2
                        input name: "cancelPeriodToAdd", type: "button", title: "Cancel", width: 2
                        paragraph getInterface("note", " Period must start and end on the same day, with the start time occurring before the end time.")
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
                            paragraph getInterface("note", " Period must start and end on the same day, with the start time occurring before the end time.")
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

def getEnumOptions(settingObject) {
    def options = []
    settingObject.each { dev ->
        def attributes = dev.getSupportedAttributes()
        logDebug("Attributes: " + attributes, "Debug")
        attributes.each { att ->
            logDebug("Attribute Name: " + att.name + " indexOf = " + options.indexOf(att.name), "Debug")
            if (options.indexOf(att.name) == -1) options.add(att.name)
        }
    }
    logDebug("Enum Options: " + options, "Debug")    
    return options
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
    
	if (offModes || onModes) subscribe(location, "mode", locationModeHandler)

    if (state.scheduleMap) {
        scheduleSchedules(false, true) // schedule start/end of any period applicable for today
        schedule("00 59 23 ? * *", scheduleSchedules) // at end of each day, schedule any applicable periods for the next day
    } 

    setRecirculatorOnOffOnInitialize()

    updateTriggerSubscriptionsAndDelayedEvents()

    if (isRecirculatorOn()) subscribeWaterTempSensors()
    else unsubscribeWaterTempSensors()

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

def subscribeDynamicTriggers() {
    logDebug("Subscribing to dynamic trigger events.", "Debug")
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

    if (customDevices1 && customDevice1Attributes) {
        subscribe(customDevices1, customDevice1Attributes, handleCustomDevices1)
    }

    if (customDevices2 && customDevice2Attributes) {
        subscribe(customDevices2, customDevice2Attributes, handleCustomDevices2)
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
}

def unsubscribeDynamicTriggers() {
    logDebug("Unsubscribing from dynamic trigger events.", "Debug")
     if (motionSensors) {
        unsubscribe(motionSensors, "motion.active")
        if (turnOffWhenMotionStops) unsubscribe(motionSensors, "motion.inactive")
    }

    if (arrivePresenceSensors) {
        unsubscribe(arrivePresenceSensors, "presence.present")
        if (turnOffWhenAllNotPresent) unsubscribe(arrivePresenceSensors, "presence.notPresent")
    }

    if (departPresenceSensors) {
        unsubscribe(departPresenceSensors, "presence.notPresent")
        if (turnOffWhenAllPresent) unsubscribe(departPresenceSensors, "presence.present")
    }

    if (openContactSensors) {
        unsubscribe(openContactSensors, "contact.open")
        if (turnOffWhenReclose) unsubscribe(openContactSensors, "contact.closed")
    }

    if (closeContactSensors) {
        unsubscribe(closeContactSensors, "contact.closed")
        if (turnOffWhenReopen) unsubscribe(closeContactSensors, "contact.open")
    }

    if (onSwitches) {
        unsubscribe(onSwitches, "switch.on")
        if (turnOffWithSwitches) unsubscribe(onSwitches, "switch.off")
    }

    if (customDevices1 && customDevice1Attributes) {
        unsubscribe(customDevices1, customDevice1Attributes)
    }

    if (customDevices2 && customDevice2Attributes) {
        unsubscribe(customDevices2, customDevice2Attributes)
    }

    if (tempTriggerSensors) {
        unsubscribe(tempTriggerSensors, "temperature")
    }

    if (accelerationSensors) {
        unsubscribe(accelerationSensors, "acceleration.active")
        if (turnOffWhenStopsMoving) unsubscribe(accelerationSensors, "acceleration.inactive")
    }

    if (flumeDevice) {
        unsubscribe(flumeDevice, "flowStatus.running")
        if (turnOffWhenFlowStops) unsubscribe(flumeDevice, "flowStatus.stopped")
    }   
}

def setRecirculatorOnOffOnInitialize() {
    def prioritizedScheduleId = getSchedulePrioritizedOverCurrentMode()
    if (prioritizedScheduleId != -1) updateFromPrioritizedSchedule(prioritizedScheduleId)
    else if (inAnySpecifiedMode()) updateFromMode()
    else if (inAnyScheduledTimePeriod()) updateFromScheduledTimePeriod()
    else updateFromTriggerState()
}

def updateTriggerSubscriptionsAndDelayedEvents() {
    def isModeActive = inAnySpecifiedMode()
    def isSchedActive = inAnyScheduledTimePeriod()

    if (!isModeActive && !isSchedActive) subscribeDynamicTriggers()
    else {
        unsubscribeDynamicTriggers()
        cancelDelayedTriggerEvents()
    }
    
/*
    if (motionSensors) {        
        if (motionSensorsPriority == "high" || (motionSensorsPriority == "medium" && !isModeActive) || (!isModeActive && !isSchedActive)) { 
            subscribe(motionSensors, "motion.active", handleTriggerOnEvent)
            if (turnOffWhenMotionStops) subscribe(motionSensors, "motion.inactive", handleMotionOff)
        }
        else {
            unsubscribe(motionSensors, "motion.active")
            if (turnOffWhenMotionStops) unsubscribe(motionSensors, "motion.inactive")
        }
    }
*/
/*
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

    if (customDevices1 && customDevice1Attributes) {
        subscribe(customDevices1, customDevice1Attributes, handleCustomDevices1)
    }

    if (customDevices2 && customDevice2Attributes) {
        subscribe(customDevices2, customDevice2Attributes, handleCustomDevices2)
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
    */
}

def cancelDelayedTriggerEvents() {
    unschedule("delayedOffHandler")
    state.pendingDelayedOffTriggers = []
    unschedule("updateFromTriggerState")
    unschedule("handleTriggerOnMaxDurationReached")
}

def subscribeWaterTempSensors() {
    logDebug("Subscribing to any water temperature sensor events.", "Debug")
    if (waterTempControlType != null) {
        if (waterTempSensor1) subscribe(waterTempSensor1, "temperature", waterTempHandler)
        if (waterTempSensor2) subscribe(waterTempSensor2, "temperature", waterTempHandler)
    }
}

def unsubscribeWaterTempSensors() {
    logDebug("Unsubscribing from any water temperature sensor events.", "Debug")
    unsubscribe(waterTempSensor1, "temperature")
    unsubscribe(waterTempSensor2, "temperature")
}

def scheduleSchedules(onlyTomorrow = true, onlyToday = false) {
    def now = new Date()
    for (j in state.scheduleMap.keySet()) {
        if ((onlyTomorrow == false && onlyToday == false) || (onlyTomorrow == true && isTomorrowWithinScheduleDates(j) && isTomorrowScheduledDayofWeek(j)) || (onlyToday == true && isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j))) {
            def shortDays = settings["schedule${j}DaysOfWeek"]
            for (i=0; i<shortDays.size(); i++) {
                shortDays[i] = daysOfWeekShortMap[shortDays[i]]
            }
            def daysOfWeekChron = shortDays.join(",")
        //  logDebug("Scheduling schedule ${settings["schedule${j}Name"]} for ${settings["schedule${j}DaysOfWeek"]}", "Debug")
            // schedule update for beginning and end of each time window, on selected days of the week, but irrespective of dates. When triggered, will check if current day is within the date window at that time and ignore if not
            state.scheduleMap[(j)].eachWithIndex { item, index ->
                def start = toDateTime(item?.start)
                if (onlyToday) {
                    def startToday = getTodayAtSameTime(start) // schedule tomorrow, rather than today, so that accounts for possibility of schedule starting at midnight
                    if (startToday.after(now)) {
                        runOnce(startToday, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
                        logDebug("Scheduled start of period ${index} for schedule " +  settings["schedule${j}Name"] + " for " + startToday, "Debug")
                    }
                    else logDebug("Start of period ${index} for schedule " +  settings["schedule${j}Name"] + " has already happened today. Nothing to schedule.", "Debug")
                }
                else if (onlyTomorrow) {
                    def startTomorrow = getTomorrowAtSameTime(start) // schedule tomorrow, rather than today, so that accounts for possibility of schedule starting at midnight
                    runOnce(startTomorrow, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
                    logDebug("Scheduled start of period ${index} for schedule " +  settings["schedule${j}Name"] + " for " + startTomorrow, "Debug")
                }
                else {
                    def startHour = start.format("H")
                    def startMin = start.format("m")
                    def startChron = "0 ${startMin} ${startHour} ? * ${daysOfWeekChron}"
                    runOnce(startChron, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
                    logDebug("Scheduled start of period ${index} for schedule " +  settings["schedule${j}Name"] + " with chron string " + startChron, "Debug")
                }
            
                def end = toDateTime(item?.end)
                if (onlyToday) {
                    def endToday = getTodayAtSameTime(end)
                    if (endToday.after(now)) {
                        runOnce(endToday, handlePeriodStop, [data: [scheduleId: j, periodId: index], overwrite: false])
                        logDebug("Scheduled start of period ${index} for schedule " +  settings["schedule${j}Name"] + " for " + endToday, "Debug")
                    }
                    else logDebug("End of period ${index} for schedule " +  settings["schedule${j}Name"] + " has already happened today. Nothing to schedule.", "Debug")
                }
                else if (onlyTomorrow) {
                    def endTomorrow = getTomorrowAtSameTime(end)
                    runOnce(endTomorrow, handlePeriodStop, [data: [scheduleId: j, periodId: index], overwrite: false])
                    logDebug("Scheduled start of period ${index} for schedule " +  settings["schedule${j}Name"] + " for " + endTomorrow, "Debug")
                }
                else {
                    def endHour = end.format("H")
                    def endMin = end.format("m")
                    def endChron = "0 ${endMin} ${endHour} ? * ${daysOfWeekChron}"
                    schedule(endChron, handlePeriodStop, [data: [scheduleId: j, periodId: index], overwrite: false])
                    logDebug("Scheduled end of period ${index} for schedule " +  settings["schedule${j}Name"] + " with chron string " + endChron, "Debug")
                }  
            }
        }
    }
}

def makeRelayMomentary() {
    recircRelay.off()
}

def waterTempHandler(evt) {
    def substate = getRecirculatorSubState()
    if (substate == "on" && !isRecirculatorOn()) {
        if (settings["simulationEnable"]) simulateNotificationDevices?.deviceNotification("Simulation: Recirculator On Cycle Until Come Up To Temp")
        else {
            recircRelay.on()
            if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
            notificationDevices?.deviceNotification("Recirculator On Cycle Until Come Up To Temp")
        }
    }
    else if (substate == "off" && !isRecirculatorOff()) {
        if (settings["simulationEnable"]) simulateNotificationDevices?.deviceNotification("Simulation: Recirculator Off Cycle While Up To Temp")
        else {
            recircRelay.on()
            if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
            notificationDevices?.deviceNotification("Recirculator Off Cycle While Up To Temp")
        }
    }
}

def getRecirculatorSubState() {
    def subState = null
    if (waterTempControlType == "singleValue" && settings["waterTempSensor1"] && settings["turnOffTemp"] && settings["turnOnTemp"]) {
        def temp = settings["waterTempSensor1"].latestValue("temperature")
        if (temp >= settings["turnOffTemp"]) {
            subState = "off"
            state.coolDownWaterTemp = true
        }
        else if (temp < settings["turnOffTemp"] && temp > settings["turnOnTemp"]) {
            if (state.coolDownWaterTemp == null || !state.coolDownWaterTemp) subState = "on" // haven't reached turn off temp yet
            else if (state.coolDownWaterTemp == true) subState = "off" // have reached turn off temp and need to cool down
        }
        else if (temp <= settings["turnOnTemp"]) {
            subState = "on"
            state.coolDownWaterTemp = false
        }
    }
    else if (waterTempControlType == "differenceValue" && settings["waterTempSensor1"] && settings["waterTempSensor2"]) {
        def diff = settings["waterTempSensor2"].latestValue("temperature") - settings["waterTempSensor1"].latestValue("temperature")
        if (diff <= settings["turnOffDiffTemp"]) {
            subState = "off"
            state.coolDownWaterTemp = true
        }
        else if (diff > settings["turnOffDiffTemp"] && diff < settings["turnOnDiffTemp"]) {
            if (state.coolDownWaterTemp == null || !state.coolDownWaterTemp) subState = "on" // haven't reached turn off temp diff yet
            else if (state.coolDownWaterTemp == true) subState = "off" // have reached turn off diff temp and need to cool down
        }
        else if (diff >= settings["turnOnDiffTemp"]) {
            subState = "on"
            state.coolDownWaterTemp = false
        }        
    }
    else subState = null
    return substate
}

def turnRecirculatorOn() {
    if (!isRecirculatorOn()) {
        def substate = null
        if (waterTempControlType != null) {
            state.coolDownWaterTemp = null
            substate = getRecirculatorSubState()
        }
        if (substate == null || substate == "on") {
            if (settings["simulationEnable"]) {
                simulateNotificationDevices?.deviceNotification("Simulation: Recirculator On" + substate == "on" ? " Until Come Up To Temp." : "")
            }
            else {
                recircRelay.on()
                if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
                notificationDevices?.deviceNotification("Recirculator On" + substate == "on" ? " Until Come Up To Temp." : "")
            }
        }
        else if (substate == "off") logDebug("Recirculator called to turn on, but already up to temp. Will turn on when needed to reach temp.", "Debug")
        subscribeWaterTempSensors()
    }
    else {
        logDebug("Recirculator called to turn on, but it is already on. Nothing to do.", "Debug")
    }
}

def turnRecirculatorOff() {
    if (!isRecirculatorOff()) {
        state.onPeriodLastEndedAt = (new Date()).getTime()
        cancelDelayedTriggerEvents()
        logDebug("Set recirculator State to off and unscheduled all delayedOffHandlers.", "Debug")
        if (settings["simulationEnable"]) {
            simulateNotificationDevices?.deviceNotification("Simulation: Recirculator Off")
        }
        else {
            recircRelay.on()
            if (recircRelayMomentary && momentaryDelay) runIn(momentaryDelay, makeRelayMomentary)
            notificationDevices?.deviceNotification("Recirculator Off")
        }
        unsubscribeWaterTempSensors()
    } else {
        logDebug("Recirculator called to turn off, but it is already off. Nothing to do.", "Debug")
    }
}

def manualOnHandler(evt) {     
    if (!isRecirculatorOn()) {
        def coolDownPeriodSecs = manualOnCoolDownPeriod ? (manualOnCoolDownPeriod.toInteger() * 60) : 60
        if (state.onPeriodLastEndedAt) {
            hasCoolDownPeriodPast = haveSecondsPast(state.onPeriodLastEndedAt, coolDownPeriodSecs)
            if (hasCoolDownPeriodPast == false) {
                def secsLeft = coolDownPeriodSecs - howManySecsPastSince(state.onPeriodLastEndedAt)
                logDebug("Minimum duration between manual on periods not met. Not triggering recirculator. Try again in ${secsLeft} seconds.", "Warning")
                return
            }
        }
        turnRecirculatorOn()
        logDebug("Manually turned recirculator on.", "Debug")
        if (settings["manualOnDuration"] && settings["manualOnDuration"]) runIn((settings["manualOnDuration"] * 60), "manualOnTimeout", [overwrite: true])
    }
    else {
        logDebug("Attempt to manually turn on recirculator detected, but recirculator is already on. Nothing to do.", "Warning")
    }
}

def manualOnTimeout() {
    state.lastManualOnTime = null
    def prioritizedScheduleId = getSchedulePrioritizedOverCurrentMode()
    if (prioritizedScheduleId != -1) updateFromPrioritizedSchedule(prioritizedScheduleId)
    else if (onModes && evt.value in onModes) turnRecirculatorOn()
    else if (inAnyScheduledTimePeriod()) updateFromScheduledTimePeriod()
    else turnRecirculatorOff()
    updateTriggerSubscriptionsAndDelayedEvents()
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
    if (recircSensedState != null) return recircSensedState.currentSwitch == "on"
    else return recircRelay.currentSwitch == "on"
}

def isRecirculatorOff() {
    if (recircSensedState != null) return recircSensedState.currentSwitch == "off"
    else return recircRelay.currentSwitch == "off"
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
    logDebug("Handling location mode event ${evt.value}", "Debug")
    def anyPrioritizedSchedule = getSchedulePrioritizedOverCurrentMode()
	if (onModes && evt.value in onModes && anyPrioritizedSchedule == -1) {
        if (settings["modesTurnOnDelay"] > 0) {
            logDebug("Detected mode ${evt.value}. Turning on in " + settings["modesTurnOnDelay"] + " minutes", "Debug")
            runIn((settings["modesTurnOnDelay"]*60), delayedModeOnHandler, [overwrite: false])
        }
        else turnRecirculatorOn()
    }
	else if (offModes && evt.value in offModes && anyPrioritizedSchedule == -1) {
        if (settings["modesTurnOffDelay"] > 0) {
            logDebug("Detected mode ${evt.value}. Turning off in " + settings["modesTurnOffDelay"] + " minutes", "Debug")
            runIn((settings["modesTurnOffDelay"]*60), delayedModeOffHandler, [overwrite: false])
        }
        else turnRecirculatorOff()
    }
    else updateFromScheduledTimePeriod() // in case mode changed after a time period has already started
    updateTriggerSubscriptionsAndDelayedEvents()
}

def updateFromMode() {
    logDebug("Updating recirculator based on mode.", "Debug")
    if (settings["offModes"] && location?.getMode() in settings["offModes"]) turnRecirculatorOff()
    else if (settings["onModes"] && location?.getMode() in settings["onModes"]) turnRecirculatorOn()
}

Boolean doesModeAllowRecirculatorOn() {
    def answer = true
    if (settings["offModes"] && location?.getMode() in settings["offModes"]) answer = false
    return answer
}

Boolean doesModeAllowRecirculatorOff() {
    def answer = true
    if (settings["onModes"] && location?.getMode() in settings["onModes"]) answer = false
    return answer
}

Boolean inAnySpecifiedMode() {
    def answer = false
    if (settings["onModes"] && location?.getMode() in settings["onModes"]) answer = true
    if (settings["offModes"] && location?.getMode() in settings["offModes"]) answer = true
    return answer
}

// SCHEDULES
def updateFromScheduledTimePeriod() {
    logDebug("Updating Recirculator State From Scheduled Time Period.", "Debug")
    def foundMatch = false
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j)) {
                // schedule applicable to today
                state.scheduleMap[(j)].eachWithIndex { item, index ->
                    if (isTimeOfDayWithinPeriod(j, index)) {
                        if (state.scheduleMap[(j)][index].state == "on") {
                            logDebug("Recirculator scheduled to be on. Turning on.", "Debug")
                            turnRecirculatorOn()
                        }
                        else if (state.scheduleMap[(j)][index].state == "off") {
                            turnRecirculatorOff()
                            logDebug("Recirculator scheduled to be off. Turning off.", "Debug")
                        }
                        if (foundMatch == false) foundMatch = true
                        else if (foundMatch == true) logDebug("Multiple overlapping time periods. Setting recirculator to state of first matching time period.", "Warning")
                    }
                }
            }
        }
    }
}

def updateFromPrioritizedSchedule(scheduleId) {
    logDebug("Updating Recirculator State From Time Period In " + settings["schedule${scheduleId}Name"] + " That is Prioritized Over the Current Mode.", "Debug")
    def foundMatch = false
    state.scheduleMap[(scheduleId)].eachWithIndex { item, index ->
        if (isTimeOfDayWithinPeriod(scheduleId, index)) {
            if (state.scheduleMap[(scheduleId)][index].state == "on") {
                logDebug("Recirculator scheduled to be on. Turning on.", "Debug")
                turnRecirculatorOn()
            }
            else if (state.scheduleMap[(scheduleId)][index].state == "off") {
                turnRecirculatorOff()
                logDebug("Recirculator scheduled to be off. Turning off.", "Debug")
            }
            if (foundMatch == false) foundMatch = true
            else if (foundMatch == true) logDebug("Multiple overlapping time periods. Setting recirculator to state of first matching time period.", "Warning")
        }
    }    
}

def handlePeriodStart(data) {
    
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    def isSchedulePrioritized = isSchedulePrioritizedOverCurrentMode(scheduleId)
    def inAnyScheduledTimePeriod = inAnyScheduledTimePeriod(scheduleId)

    logDebug("Handling Start of Period " + periodId + " for schedule " + settings["schedule${scheduleId}Name"], "Debug")

    if (inAnySpecifiedMode() && (!isSchedulePrioritized || (isSchedulePrioritized && !inAnyScheduledTimePeriod))) {
        logDebug("Hub is in a mode (${location?.getMode()}) that already dictates recirculator state. Nothing more to do at start of period.", "Debug")
        return
    }

    if (!isTodayWithinScheduleDates(scheduleId)) return // schedule not applicable to today's date
    if (!isScheduledDayofWeek(scheduleId)) return // schedule not applicable to today's day of the week

    // scheduled period start is applicable for today, so handle accordingly
    if (state.scheduleMap[(scheduleId)] && state.scheduleMap[(scheduleId)][periodId] && state.scheduleMap[(scheduleId)][periodId].state == "on") {
        // turn recirculator on unless Hubitat mode dictates that the recirculator be off, no matter dynamic triggers (prioritizes time schedule over dynamic triggers)
        if (!isRecirculatorOn()) {
            if (doesModeAllowRecirculatorOn() || isSchedulePrioritized) turnRecirculatorOn()
            else logDebug("Recirculator is scheduled to turn on now, but mode does not allow it to be turned on. Nothing to do.", "Debug")
        }
        else logDebug("Recirculator is scheduled to turn on now, but recirculator is already on. Nothing to do.", "Debug")
    }
    else if (state.scheduleMap[(scheduleId)] && state.scheduleMap[(scheduleId)][periodId] && state.scheduleMap[(scheduleId)][periodId].state == "off") {
        // turn recirculator off unless Hubitat mode dictates that the recirculator be on, no matter dynamic triggers (prioritizes time schedule over dynamic triggers)
        if (!isRecirculatorOff()) {
            if (doesModeAllowRecirculatorOff() || isSchedulePrioritized) turnRecirculatorOff()
            else logDebug("Recirculator is scheduled to turn off now, but mode does not allow it to be turned off. Nothing to do.", "Debug")
        }
        else logDebug("Recirculator is scheduled to turn off now, but recirculator is already off. Nothing to do.", "Debug")

    }
    updateTriggerSubscriptionsAndDelayedEvents()
}

def handlePeriodStop(data) {
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    if (inAnySpecifiedMode()) {
        logDebug("Hub is in a mode (${location?.getMode()}) that already dictates recirculator state. Nothing more to do at end of period.", "Debug")
        return
    }

    logDebug("Handling End of Period " + periodId + " for schedule " + settings["schedule${scheduleId}Name"], "Debug")

    if (!isTodayWithinScheduleDates(scheduleId)) return // schedule not applicable to today's date
    if (!isScheduledDayofWeek(scheduleId)) return // schedule not applicable to today's day of the week

    // scheduled period stop is applicable for today, so scheduled period has now ended. Update from trigger state
    updateFromTriggerState()
    updateTriggerSubscriptionsAndDelayedEvents()
}

def isTimeOfDayWithinPeriod(scheduleId, periodId) {
    def answer = false
    def startReference = toDateTime(state.scheduleMap[(scheduleId)][periodId].start)
    def start = getTodayAtSameTime(startReference)
    def endReference = toDateTime(state.scheduleMap[(scheduleId)][periodId].end)
    def end = getTodayAtSameTime(endReference)
    if (start && end && timeOfDayIsBetween(start, end, new Date(), location.timeZone)) answer = true
 //   logDebug("Schedule " + settings["schedule${scheduleId}Name"] + " Period ${periodId} starts at ${start} and ends at ${end}. Current time between start and end? ${answer}", "Debug")
    return answer
}

Boolean doSchedulesAllowRecirculatorState(onOrOffState) {
    def answer = true
    def compareState = ""
    if (onOrOffState == "on") compareState = "off"
    else if (onOrOffState == "off") compareState = "on"
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j)) {
                // schedule applicable to today
             //   logDebug("Schedule " + settings["schedule${j}Name"] + " applicable to today. Checking Time Periods.", "Debug")
                state.scheduleMap[(j)].eachWithIndex { item, index ->
                    // check if time periods of schedule allow for recirculator to be on right now
                    if (isTimeOfDayWithinPeriod(j, index)) {
                        if (state.scheduleMap[(j)][index].state == compareState) {
                            answer = false
                            return answer
                        }
                    }
                }
            }
            else logDebug("Schedule " + settings["schedule${j}Name"] + " not applicable to today. Skipping time period checks.", "Debug")
        }
    }
    return answer
}

Boolean isSchedulePrioritizedOverCurrentMode(scheduleId) {
    def answer = false
    if (settings["schedule${scheduleId}DeprioritizedModes"] && location?.getMode() in settings["schedule${scheduleId}DeprioritizedModes"]) answer = true
    logDebug("schedule${scheduleId}DeprioritizedModes = " + settings["schedule${scheduleId}DeprioritizedModes"] + "location?.getMode() = " + location?.getMode() + " answer = " + location?.getMode() in settings["schedule${scheduleId}DeprioritizedModes"], "Debug")
    return answer
}

def getSchedulePrioritizedOverCurrentMode() {
    def prioritizedScheduleIndex = -1
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j) && isSchedulePrioritizedOverCurrentMode(j)) {
                // schedule applicable to today and is prioritized over current mode
                state.scheduleMap[(j)].eachWithIndex { item, index ->
                    if (isTimeOfDayWithinPeriod(j, index)) {
                        logDebug("Schedule " + settings["schedule${j}Name"] + " has an active period and is prioritized over the current mode.", "Debug")
                        if (prioritizedScheduleIndex == -1) prioritizedScheduleIndex = j
                        else logDebug("Schedule " + settings["schedule${j}Name"] + " has an active period and is prioritized over the current mode, but there are multiple prioritized schedules/periods that are active. Ignoring this one.", "Debug")
                    }
                }
            }
        }
    }    
    if (prioritizedScheduleIndex == -1) logDebug("No Schedules with an active period found to be prioritized over the current mode.", "Debug")
    return prioritizedScheduleIndex
}

Boolean inAnyScheduledTimePeriod() {
    logDebug("inAnyScheduledTimePeriod()...Checking if any scheduled time period active.", "Trace")
    def answer = false
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j)) {
                // schedule applicable to today
                logDebug("Schedule " + settings["schedule${j}Name"] + " applicable to today.", "Debug")
                if (inAnyScheduledTimePeriod(j)) {
                    answer = true
                    logDebug("Found active time period for schedule " + settings["schedule${j}Name"] + ".", "Debug")
                }
                else logDebug("No active time period for schedule " + settings["schedule${j}Name"] + ".", "Debug")
            }
            else logDebug("Schedule " + settings["schedule${j}Name"] + " not applicable to today. Skipping time period checks.", "Debug")
        }
    }
    return answer    
}

Boolean inAnyScheduledTimePeriod(scheduleId) {
    def answer = false
    if (state.scheduleMap) {
        state.scheduleMap[(scheduleId)].eachWithIndex { item, index ->
            if (isTimeOfDayWithinPeriod(scheduleId, index)) {
                answer = true
                logDebug("Currently in Period ${index} for schedule " + settings["schedule${scheduleId}Name"], "Debug")
                return answer
            }
            else logDebug("Currently not in Period ${index} for schedule " + settings["schedule${scheduleId}Name"], "Debug")
        }
    }
    return answer    
}

def getTodayAtSameTime(timeReferenceDate) {
    Date today = new Date()
    def timeMap = getTimeMapFromDateTime(timeReferenceDate)
    def atHour = timeMap.hour
    def atMinutes = timeMap.minutes
    Date todayAtTime = today.copyWith(hourOfDay: atHour, minute: atMinutes, second: 0)
    return todayAtTime
}

def getTomorrowAtSameTime(timeReferenceDate) {
    Date today = new Date()
    Date tomorrow = today + 1
    def timeMap = getTimeMapFromDateTime(timeReferenceDate)
    def atHour = timeMap.hour
    def atMinutes = timeMap.minutes
    Date tomorrowAtTime = tomorrow.copyWith(hourOfDay: atHour, minute: atMinutes, second: 0)
    return tomorrowAtTime
}

def getTimeMapFromDateTime(dateTime) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(dateTime)
    def hour = cal.get(Calendar.HOUR_OF_DAY)
    def minutes = cal.get(Calendar.MINUTE)
    return [hour: hour, minutes: minutes]
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

    if (sMonth != null && sDay != null && eMonth != null && eDay != null) {
        if ((sMonth < eMonth) || (sMonth == eMonth && sDay < eDay)) {
            // start day occurs before end day (schedule stays within a single year)
            if ((month == sMonth && day >= sDay) || month > sMonth)  {
                if ((month == eMonth && day <= eDay) || month < eMonth) {
                    withinDates = true
                }
            }
        }
        else if ((sMonth > eMonth) || (sMonth == eMonth && sDay > eDay)) {
            // start day occurs after end day (schedule wraps around to new year)
            if ((month == sMonth && day >= sDay) || month > sMonth) {
                withinDates = true
            }
            else if ((month == eMonth && day <= eDay) || month < eMonth)  {
                withinDates = true
            }
        }
        else if (sMonth == eMonth && sDay == eDay) logDebug("Start day and End day of schedule the same. Invalid schedule.", "Warning")
    }
    else logDebug("schedule dates have null value. Aborting schedule check.", "Warning")
        
    return withinDates
}

def isTomorrowWithinScheduleDates(scheduleId) {
    def withinDates = false
    def today = timeToday(null, location.timeZone)
    def tomorrow = today + 1
	def month = tomorrow.month+1
	def day = tomorrow.date
    
    def startMonth = settings["schedule${scheduleId}StartMonth"]
    def startDay = settings["schedule${scheduleId}StartDay"]
    def stopMonth = settings["schedule${scheduleId}StopMonth"]
    def stopDay = settings["schedule${scheduleId}StopDay"]

    def sMonth = monthsMap[startMonth]
    def sDay = startDay.toInteger()
    def eMonth = monthsMap[stopMonth]
    def eDay = stopDay.toInteger()

    if (sMonth != null && sDay != null && eMonth != null && eDay != null) {
        if ((sMonth < eMonth) || (sMonth == eMonth && sDay < eDay)) {
            // start day occurs before end day (schedule stays within a single year)
            if ((month == sMonth && day >= sDay) || month > sMonth)  {
                if ((month == eMonth && day <= eDay) || month < eMonth) {
                    withinDates = true
                }
            }
        }
        else if ((sMonth > eMonth) || (sMonth == eMonth && sDay > eDay)) {
            // start day occurs after end day (schedule wraps around to new year)
            if ((month == sMonth && day >= sDay) || month > sMonth) {
                withinDates = true
            }
            else if ((month == eMonth && day <= eDay) || month < eMonth)  {
                withinDates = true
            }
        }
        else if (sMonth == eMonth && sDay == eDay) logDebug("Start day and End day of schedule the same. Invalid schedule.", "Warning")
    }
    else logDebug("schedule dates have null value. Aborting schedule check.", "Warning")
        
    return withinDates
}

def isScheduledDayofWeek(scheduleId) {
    def answer = false    
    def today = (new Date()).format('EEEE') 
    def shortToday = daysOfWeekShortMap[today]
    if(settings["schedule${scheduleId}DaysOfWeek"] && (settings["schedule${scheduleId}DaysOfWeek"].contains(today) || settings["schedule${scheduleId}DaysOfWeek"].contains(shortToday))) answer = true 
    return answer
}

def isTomorrowScheduledDayofWeek(scheduleId) {
    def answer = false    
    def today = new Date()
    def tomorrow = (today + 1).format('EEEE') 
    def shortTomorrow = daysOfWeekShortMap[tomorrow]
    if(settings["schedule${scheduleId}DaysOfWeek"] && (settings["schedule${scheduleId}DaysOfWeek"].contains(tomorrow) || settings["schedule${scheduleId}DaysOfWeek"].contains(shortTomorrow))) answer = true 
    return answer
}

def getDayOfWeek(Date date) {
    Calendar cal = Calendar.getInstance()
    cal.setTimeZone(location.timeZone)
    cal.setTime(date)
    def dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)    
    return dayOfWeek
}

// DYNAMIC TRIGGERS
def handleTriggerOnEvent(evt) {
    logDebug("handleTriggerTriggered() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
    if (isRecirculatorOff() || (isRecirculatorOn() && settings["maxDurationExtendable"] == true)) {
        // trigger on with device either if recirculator is off or if recirculator is on but the on duration period should be extended by this device trigger|
        logDebug("${evt.device?.label} (${evt.device?.id}) triggered on period. Cancelled any pending delayed turn off of recirculator.", "Debug")
        unschedule("delayedOffHandler")
        state.pendingDelayedOffTriggers = []
        triggerOn()
    }
    else logDebug("${evt.device?.label} triggered on period but recirculator is already on and maximum duration of the on period is not configured in app settings to extend the maximum on period duration", "Debug")
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

def handleCustomDevices1(evt) {
    if (evt.value == customDevice1AttributeValue) {
        state.customDevice1Triggered = true
        handleTriggerOnEvent(evt)
    }
    else if (evt.value != customDevice1AttributeValue) {
        if (state.customDevice1Triggered && turnOffWithCustomDevice1) {
            handleTriggerOffEvent(evt, settings["turnOffWithCustomDevice1Delay"] ?: 0)
        }
        state.customDevice1Triggered = false
    }
}

def handleCustomDevices2(evt) {
    if (evt.value == customDevice2AttributeValue) {
        state.customDevice2Triggered = true
        handleTriggerOnEvent(evt)
    }
    else if (evt.value != customDevice2AttributeValue) {
        if (state.customDevice2Triggered && turnOffWithCustomDevice2) {
            handleTriggerOffEvent(evt, settings["turnOffWithCustomDevice2Delay"] ?: 0)
        }
        state.customDevice2Triggered = false
    }
}

def handleAccelerationOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenStopsMovingDelay"] ?: 0)
}

def handleFlumeOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenFlowStopsDelay"] ?: 0)
}

def handleTriggerOffEvent(evt, turnOffDelay) {
    logDebug("handleTriggerOffEvent() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
        if (isRecirculatorOn()) {
            if (turnOffDelay > 0) {
                runIn(turnOffDelay * 60, "delayedOffHandler", [overwrite: false, data: [deviceLabel: "${evt.device?.label}", deviceId: evt.device?.id]])
                if (state.pendingDelayedOffTriggers == null) state.pendingDelayedOffTriggers = []
                if (state.pendingDelayedOffTriggers.indexOf(evt.device?.id) == -1) state.pendingDelayedOffTriggers.add(evt.device?.id)
                logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period after delay. Scheduled recirculator to turn off in " + turnOffDelay + " minutes. state.pendingDelayedOffTriggers = " + state.pendingDelayedOffTriggers, "Debug")
            }
            else {
                logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period. Calling triggerOff()", "Debug")
                triggerOff()
            } 
        }
        else logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period but recirculator is already off. Nothing to do.", "Debug")
}

def delayedOffHandler(data) {
    logDebug("Executing delayeOffHandler() triggered by ${data.deviceLabel}...", "Trace")
    if (state.pendingDelayedOffTriggers != null && state.pendingDelayedOffTriggers.indexOf(data.deviceId) != -1) {
        state.pendingDelayedOffTriggers.removeElement(data.deviceId)
        logDebug("Removed ${data.deviceId} from state.pendingDelayedOffTriggers", "Debug")
    }
    if (state.pendingDelayedOffTriggers == null || (state.pendingDelayedOffTriggers != null && state.pendingDelayedOffTriggers.size() == 0)) triggerOff() // only turn off if no pending trigger off events still
    else logDebug("delayeOffHandler() triggered by ${data.deviceLabel} but there are other delayed trigger off events still pending. Waiting to turn off. state.pendingDelayedOffTriggers = " + state.pendingDelayedOffTriggers + " with size = " + state.pendingDelayedOffTriggers.size(), "Debug")
}

def triggerOn() {   
    logDebug("Executing triggerOn()...", "Trace")
    if (isRecirculatorOff() && state.onPeriodLastEndedAt) {
        def coolDownPeriodSecs = triggerOnCoolDownPeriod ? (triggerOnCoolDownPeriod.toInteger() * 60) : 60
        def hasCoolDownPeriodPast = haveSecondsPast(state.onPeriodLastEndedAt, coolDownPeriodSecs)
        if (hasCoolDownPeriodPast == false) {
            def secsLeft = coolDownPeriodSecs - howManySecsPastSince(state.onPeriodLastEndedAt)
            logDebug("Minimum duration between dynamically triggered on periods not met (${secsLeft} seconds left in cooldown period). Not triggering recirculator, but will check again in ${secsLeft} seconds.", "Warning")
            if (secsLeft > 0) runIn(secsLeft, "updateFromTriggerState")
            return
        }
    }
    
    turnRecirculatorOn() // will only turn on if already off

    if (settings["triggerOnMaxDuration"] && settings["triggerOnMaxDuration"]) runIn((settings["triggerOnMaxDuration"] * 60), "handleTriggerOnMaxDurationReached", [overwrite: true])
    // because triggerOn is called if the recirculator is already on and maxDurationExtendable == true, then this call to overwrite any existing triggerOnTimeout method will extend the max duration on period, to be the max duration on period from the start of the latest trigger
}

def handleTriggerOnMaxDurationReached() {
    logDebug("Executing handleTriggerOnMaxDurationReached() ...", "Trace")
    triggerOff(true)
}

def triggerOff(maxDurationReached = false) {
    logDebug("Executing triggerOff()...", "Trace")
    if (maxDurationReached) {
        turnRecirculatorOff()
        logDebug("Triggerd off based on max duration being reached, even if some triggers are still on.", "Debug")
    }
    else if (areAllTriggersOff()) {
        turnRecirculatorOff()
        logDebug("Triggerd off based on all triggers being off.", "Debug")
    }
    else logDebug("Triggerd off, but not all triggers are off and max on-duration has not yet been reached. Keeping on.", "Debug")
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
    logDebug("Updating based on trigger state.", "Debug")
    if (isRecirculatorOff() && !areAllTriggersOff() && doesModeAllowRecirculatorOn() && doSchedulesAllowRecirculatorState("on")) {
        // check current state of trigger devices and turn recirculator on if state of trigger devices indicates it should be on and mode allows it to be on
        triggerOn()
        logDebug("Turned Recirculator on based on state of dynamic triggers", "Debug")
    }
    else if (isRecirculatorOn() && areAllTriggersOff() && doesModeAllowRecirculatorOff() && doSchedulesAllowRecirculatorState("off")) {
        triggerOff()
        logDebug("Turned Recirculator off based on state of dynamic triggers", "Debug")
    }
    else logDebug("State of Recirculator is already consistent with state of dynamic triggers. Nothing to do.", "Debug")
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

private def howManySecsPastSince(timestamp) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.getTime()
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().getTime() - timestamp) / 1000
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

