
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
<<<<<<< HEAD
 * v0.3.1 - bug fixes
 * v0.3.0 - substantial recode to allow prioritizing any selected trigger over select mode(s) and/or schedule(s)
 * v0.2.6 - reverted to stateless recirc on/off control while maintaining water temp substate
 * v0.2.5 - added ability to prioritize a schedule over select mode(s)
 * v0.2.4 - fix shorter delayed off from triggering off when still have longer delayed off pending
 * v0.2.3 - bug fixes
 * v0.2.2 - make sensed state switch optional
 * v0.2.1 - bug fixes; early version of water temp sensor handling
 * v0.2.0 - bug fixes with modes and schedules; optimized to unsubscribe from trigger events if mode or schedule active; added support for schedules that wrap around to new year
 * v0.1.2 - bug fix with schedules
 * v0.1.1 - update momentary relay settings and make delay configurable
 * v0.1.0 - Beta release
=======
 * v.0.3.0 - substantial recode to allow prioritizing any selected trigger over select mode(s) and/or schedule(s)
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
>>>>>>> c7864cab24c0a9061fe74fbd7f33e22a9dae563c
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

@Field triggerTypes = ["MotionSensor", "ArriveSensor", "DepartSensor", "OpenSensor", "CloseSensor", "Switch", "CustomDevice1", "CustomDevice2", "TempSensor", "MoveSensor", "FlumeSensor"]

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
            paragraph getInterface("note", "Demanding the recirculator to turn on using this switch will unconditionally turn the recirculator on for at least the specified duration, after which the app will resume normal control based on mode, schedule, or dynamic triggers.")
            if (manualOnSwitch) {
                input name: "manualOnDuration", type: "number", title: "On Duration (minutes)", required: true, defaultValue: 20, multiple: false, width: 6
                input name: "manualOnCoolDownPeriod", type: "number", title: "Minimum off duration, if any, before can manually turn on", required: false, defaultValue: 0, multiple: false, width: 6
            }
            input name: "manualOffSwitch", type: "capability.switch", title: "OFF Switch: Momentary Switch to manually turn off the recirculator", multiple: false, required: false
            paragraph getInterface("note", "Demanding the recirculator to turn off using this switch will unconditionally turn the recirculator off for at least the specified duration, after which the app will resume normal control based on mode, schedule, or dynamic triggers.")
            if (manualOffSwitch) input name: "manualOffDuration", type: "number", title: "Off Duration (minutes)", required: true, defaultValue: 20, multiple: false, width: 6
        }

        section() {
            paragraph getInterface("header", " Recirculator Mode Controls")
	        input name: "offModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator off", multiple: true, required: false, width: 6              
	        input name: "onModes",  type: "mode", title: "Hubitat Mode(s) in which to turn the recirculator on", multiple: true, required: false, width: 6                    
        }

        section() {
            paragraph getInterface("header", " Recirculator Schedules")
            href(name: "SchedulePage", title: getInterface("boldText", "Configure Time-Based Schedule(s)"), description: getSchedulesDescription() ?: "", required: false, page: "schedulePage", image:  (getSchedulesEnumList() ? checkMark : xMark))
        }

		section() {
            paragraph getInterface("header", " Recirculator Dynamic Trigger events")
            paragraph "Specify events to trigger the recirculator to turn on."

            paragraph ""
			input name: "motionSensors", type: "capability.motionSensor", title: "On when motion is detected in any of these places", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.motionSensors) {
                input name:"motionOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes, multiple: true, required: false, width: 6
                input name:"motionOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "turnOffWhenMotionStops", type: "bool", title: "Off when motion stops everywhere?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenMotionStops) input name: "turnOffWhenMotionStopsDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            }

            paragraph ""
			input name: "arrivePresenceSensors", type: "capability.presenceSensor", title: "On when any of these presence sensors arrives", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.arrivePresenceSensors) {
                input name:"arrivePresenceOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"arrivePresenceOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "turnOffWhenAllNotPresent", type: "bool", title: "Off when all presence sensors are not present?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllNotPresentDelay", type: "number", title: "After how long of a delay, if any  (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            }

            paragraph ""
			input name: "departPresenceSensors", type: "capability.presenceSensor", title: "On when any of these presence sensors departs", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.departPresenceSensors) {
                input name:"departPresenceOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"departPresenceOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "turnOffWhenAllPresent", type: "bool", title: "Off when all presence sensors are present?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenPresenceDeparts) input name: "turnOffWhenAllPresentDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
            }

			paragraph ""
			input name: "openContactSensors", type: "capability.contactSensor", title: "On when any of these things open", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.openContactSensors) {
                input name:"openContactOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"openContactOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
				input name: "turnOffWhenReclose", type: "bool", title: "Off when all of these things re-close?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenReclose) input name: "turnOffWhenRecloseDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}
			
			paragraph ""
			input name: "closeContactSensors", type: "capability.contactSensor", title: "On when any of these things close", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.closeContactSensors) {
                input name:"closeContactOverModes",type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"closeContactOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
				input name: "turnOffWhenReopen", type: "bool", title: "Off when all of these things re-open?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenReopen) input name: "turnOffWhenReopenDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}

			paragraph ""
			input name: "onSwitches", type: "capability.switch", title: "On with any of these switches", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.onSwitches) {
                input name:"onSwitchesOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"onSwitchesOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
				input name: "turnOffWithSwitches", type: "bool", title: "Off when all switches turn off?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWithSwitches) input name: "turnOffWithSwitchesDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}

			paragraph ""
			input name: "customDevices1", type: "capability.*", title: "On when a specified attribute of any of these devices changes to a specified value (Custom Device 1)", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
            paragraph getInterface("note", "Example use: Dishwasher smart plug device using 'Zooz Power Switch with States' driver to specify when dishwasher is idle or running. If select multiple devices, all devices must have the same attribute.")
			if (settings.customDevices1) {
                def enumOptions = getEnumOptions(settings["customDevices1"])
                input name: "customDevice1Attribute", type: "enum", title: "Select attribute...", options: enumOptions, multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice1AttributeValue", type: "string", title: "On when attribute value change to:", multiple: false, required: true, submitOnChange: true, width: 6
                input name:"customDevices1OverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes, multiple: true, required: false, width: 6
                input name:"customDevices1OverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "turnOffWithCustomDevice1", type: "bool", title: "Off when the specified attribute of all of the devices changes away from the specific value?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWithCustomDevice1) input name: "turnOffWithCustomDevice1Delay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}

			paragraph ""
			input name: "customDevices2", type: "capability.*", title: "On when a specified attribute of any of these devices changes to a specified value (Custom Device 2)", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
            paragraph getInterface("note", "Example use: Washing Machine smart plug device using 'Zooz Power Switch with States' driver to specify when washing machine is idle or running. If select multiple devices, all devices must have the same attribute.")
			if (settings.customDevices2) {
                def enumOptions = getEnumOptions(settings["customDevices2"])
                input name: "customDevice2Attribute", type: "enum", title: "Select attribute...", options: enumOptions, multiple: false, required: true, submitOnChange: true, width: 6
                input name: "customDevice2AttributeValue", type: "string", title: "On when attribute value change to:", multiple: false, required: true, submitOnChange: true, width: 6
                input name:"customDevices2OverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes, multiple: true, required: false, width: 6
                input name:"customDevices2OverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "turnOffWithCustomDevice2", type: "bool", title: "Off when the specified attribute of all of the devices changes away from the specific value?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWithCustomDevice2) input name: "turnOffWithCustomDevice2Delay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}

			paragraph ""
			input name: "tempTriggerSensors", type: "capability.temperatureMeasurement", title: "On when outside temperature drops", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.tempTriggerSensors) {
                input name:"tempOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"tempOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
                input name: "onWhenBelowTemp", type: "number", title: "On when at least one sensor falls below this temp", required: true, width: 6
                input name: "offWhenAboveTemp", type: "number", title: "Off when all sensors rise above this temp", required: true, width: 6
			}

            paragraph ""
			input name: "accelerationSensors", type: "capability.accelerationSensor", title: "On when any of these things move", multiple: true, required: false, refreshAfterSelection: true, submitOnChange: true
			if (accelerationSensors) {
                input name:"accelerationOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes,multiple: true, required: false, width: 6
                input name:"accelerationOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
				input name: "turnOffWhenStopsMoving", type: "bool", title: "Off when all stop moving?",  defaultValue: false
                if (turnOffWhenStopsMoving) input name: "turnOffWhenStopsMovingDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0
            }

			paragraph ""
			input name: "flumeDevice", type: "device.FlumeDevice", title: "On when Flume Device Detects Flow", multiple: false, required: false, refreshAfterSelection: true, submitOnChange: true
			if (settings.flumeDevice) {
                input name:"flumeOverModes", type: "enum", title: "Trigger on even in these OFF Modes...", options: offModes, multiple: true, required: false, width: 6
                input name:"flumeOverSchedules", type: "enum", title: "Trigger on even if scheduled to be off by these schedules...", options: getSchedulesEnumMap(), multiple: true, required: false, width: 6
				input name: "turnOffWhenFlowStops", type: "bool", title: "Off when flow stops?", defaultValue: false, submitOnChange: true, width: 6
                if (turnOffWhenFlowStops) input name: "turnOffWhenFlowStopsDelay", type: "number", title: "After how long of a delay, if any (minutes)?", required: true, defaultValue: 0, multiple: false, width: 6
			}

            paragraph getInterface("subHeader", " Dynamic Trigger Limits")
            input name: "triggerOnMaxDuration", type: "number", title: "Maximum Duration of triggered on period (minutes)?", required: false, defaultValue: 30, multiple: false, width: 4
            input name: "maxDurationExtendable", type: "bool", title: "Maximum Duration Extendable with Continued Triggers?", defaultValue: false, required: true, width: 4
            input name: "triggerOnCoolDownPeriod", type: "number", title: "Minimum Duration between end of one triggered on period and start of next triggered on period (minutes)?", required: false, defaultValue: 0, multiple: false, width: 4
            input name: "triggerLimitsExceptions", type: "enum", title: "Exceptions: Select Types of Triggers", required: false, multiple: true, options: getTriggerTypeOptions(), width: 12
            paragraph getInterface("note", " Dynamic trigger limits will not be enforced when any of the selected trigger types are active.")
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
<<<<<<< HEAD

                    def modeOptions = getModeOptions()
                    if (modeOptions.size() > 0) input name:"schedule${j}DeprioritizedModes", type: "enum", title: "Prioritize schedule over these Hubitat modes that are defined as ON or OFF modes...", options: modeOptions, multiple: true, required: false, width: 12
=======
                    input name:"schedule${j}DeprioritizedModes", type: "mode", title: "Prioritize Schedule Over These Modes...", options: location?.getModes(), multiple: true, required: false, width: 12
>>>>>>> c7864cab24c0a9061fe74fbd7f33e22a9dae563c
                    
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

def getSchedulesEnumMap() {
    def map = [:]
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            map[j] = settings["schedule${j}Name"]
        }
    }
    return map
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
     //   logDebug("Attributes: " + attributes, "Debug")
        attributes.each { att ->
         //   logDebug("Attribute Name: " + att.name + " indexOf = " + options.indexOf(att.name), "Debug")
            if (options.indexOf(att.name) == -1) options.add(att.name)
        }
    }
 //   logDebug("Enum Options: " + options, "Debug")    
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
    state.maxDurationHandlingScheduled = false
    state.manualOn = false
    state.manualOff = false

    update(true)
    schedule("0 1 0 ? * *", update) // at beginning of each day, update recirculator state to handle potential issues with the start of the day

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

def getTriggerTypeOptions() {
    def triggers = []
    if (motionSensors) triggers.add("MotionSensor")
    if (arrivePresenceSensors) triggers.add("ArriveSensor")
    if (departPresenceSensors) triggers.add("DepartSensor")
    if (openContactSensors) triggers.add("OpenSensor")
    if (closeContactSensors) triggers.add("CloseSensor")
    if (onSwitches) triggers.add("Switch")
    if (customDevices1 && customDevice1Attribute) triggers.add("CustomDevice1")
    if (customDevices2 && customDevice2Attribute) triggers.add("CustomDevice2")
    if (tempTriggerSensors) triggers.add("TempSensor")
    if (accelerationSensors) triggers.add("MoveSensor")
    if (flumeDevice) triggers.add("FlumeSensor")
    return triggers
}

def updateTriggerSubscriptionsAndDelayedEvents(triggers = triggerTypes) {
    
    if (motionSensors) {        
        if ("MotionSensor" in triggers) { 
            subscribe(motionSensors, "motion.active", handleTriggerOnEvent)
            if (turnOffWhenMotionStops) subscribe(motionSensors, "motion.inactive", handleMotionOff)
        }
        else {
            unsubscribe(motionSensors, "motion.active")
            if (turnOffWhenMotionStops) unsubscribe(motionSensors, "motion.inactive")
            cancelDelayedOffTrigger("MotionSensor")
        }
    }

    if (arrivePresenceSensors) {
        if ("ArriveSensor" in triggers) { 
            subscribe(arrivePresenceSensors, "presence.present", handleTriggerOnEvent)
            if (turnOffWhenAllNotPresent) subscribe(arrivePresenceSensors, "presence.not present", handleArrivePresenceOff)
        }
        else {
            unsubscribe(arrivePresenceSensors, "presence.present")
            if (turnOffWhenAllNotPresent) unsubscribe(arrivePresenceSensors, "presence.not present")
            cancelDelayedOffTrigger("ArriveSensor")
        }
    }

    if (departPresenceSensors) {
        if ("DepartSensor" in triggers) { 
            subscribe(departPresenceSensors, "presence.not present", handleTriggerOnEvent)
            if (turnOffWhenAllPresent) subscribe(departPresenceSensors, "presence.present", handleDepartPresenceOff)
        }
        else {
            unsubscribe(departPresenceSensors, "presence.not present")
            if (turnOffWhenAllPresent) unsubscribe(departPresenceSensors, "presence.present")  
            cancelDelayedOffTrigger("DepartSensor")          
        }
    }

    if (openContactSensors) {
        if ("OpenSensor" in triggers) { 
            subscribe(openContactSensors, "contact.open", handleTriggerOnEvent)
            if (turnOffWhenReclose) subscribe(openContactSensors, "contact.closed", handleOpenContacteOff)
        }
        else {
            unsubscribe(openContactSensors, "contact.open")
            if (turnOffWhenReclose) unsubscribe(openContactSensors, "contact.closed")   
            cancelDelayedOffTrigger("OpenSensor")         
        }
    }

    if (closeContactSensors) {
        if ("CloseSensor" in triggers) { 
            subscribe(closeContactSensors, "contact.closed", handleTriggerOnEvent)
            if (turnOffWhenReopen) subscribe(closeContactSensors, "contact.open", handleCloseContactOff)
        }
        else {
            unsubscribe(closeContactSensors, "contact.closed")
            if (turnOffWhenReopen) unsubscribe(closeContactSensors, "contact.open")    
            cancelDelayedOffTrigger("CloseSensor")        
        }
    }

    if (onSwitches) {
        if ("Switch" in triggers) { 
            subscribe(onSwitches, "switch.on", handleTriggerOnEvent)
            if (turnOffWithSwitches) subscribe(onSwitches, "switch.off", handleOnSwitchOff)
        }
        else {
            unsubscribe(onSwitches, "switch.on")
            if (turnOffWithSwitches) unsubscribe(onSwitches, "switch.off")
            cancelDelayedOffTrigger("Switch")
        }
    }

    if (customDevices1 && customDevice1Attribute) {
        if ("CustomDevice1" in triggers) { 
            subscribe(customDevices1, customDevice1Attribute, handleCustomDevices1)
        }
        else {
            unsubscribe(customDevices1, customDevice1Attribute)
            cancelDelayedOffTrigger("CustomDevice1")
        }
    }

    if (customDevices2 && customDevice2Attribute) {
        if ("CustomDevice2" in triggers) { 
            subscribe(customDevices2, customDevice2Attribute, handleCustomDevices2)
        }
        else {
            unsubscribe(customDevices2, customDevice2Attribute)
            cancelDelayedOffTrigger("CustomDevice2")
        }
    }

    if (tempTriggerSensors) {
        if ("TempSensor" in triggers) { 
            subscribe(tempTriggerSensors, "temperature", triggerTempHandler)
        }
        else {
            unsubscribe(tempTriggerSensors, "temperature")
            cancelDelayedOffTrigger("TempSensor")
        }
    }

    if (accelerationSensors) {
        if ("MoveSensor" in triggers) { 
            subscribe(accelerationSensors, "acceleration.active", handleTriggerOnEvent)
            if (turnOffWhenStopsMoving) subscribe(accelerationSensors, "acceleration.inactive", handleAccelerationOff)
        }
        else {
            unsubscribe(accelerationSensors, "acceleration.active")
            if (turnOffWhenStopsMoving) unsubscribe(accelerationSensors, "acceleration.inactive")     
            cancelDelayedOffTrigger("MoveSensor")      
        }
    }

    if (flumeDevice) {
        if ("FlumeSensor" in triggers) { 
            subscribe(flumeDevice, "flowStatus.running", handleTriggerOnEvent)
            if (turnOffWhenFlowStops) subscribe(flumeDevice, "flowStatus.stopped", handleFlumeOff)
        }
        else {
            unsubscribe(flumeDevice, "flowStatus.running")
            if (turnOffWhenFlowStops) unsubscribe(flumeDevice, "flowStatus.stopped")
            cancelDelayedOffTrigger("FlumeSensor")
        }
    }
}

def cancelDelayedOffTrigger(deviceType = "All") {
    logDebug("Cancelling any delayed off triggers for: " + deviceType, "Debug")
    if (deviceType == "All") {
        for (type in triggerTypes) {
            unschedule("delayedOffHandler" + type)
            if (state.pendingDelayedOffTriggers == null) state.pendingDelayedOffTriggers = [:]
            state.pendingDelayedOffTriggers[type] = []
        }
    }
    else if (deviceType != null) {
        unschedule("delayedOffHandler" + deviceType)
        if (state.pendingDelayedOffTriggers == null) state.pendingDelayedOffTriggers = [:]
        state.pendingDelayedOffTriggers[deviceType] = []
    }
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
                    schedule(startChron, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
<<<<<<< HEAD
                    schedule(startChron, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
=======
                    runOnce(startChron, handlePeriodStart, [data: [scheduleId: j, periodId: index], overwrite: false])
>>>>>>> c7864cab24c0a9061fe74fbd7f33e22a9dae563c
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
    def coolDownPeriodSecs = manualOnCoolDownPeriod != null ? (manualOnCoolDownPeriod.toInteger() * 60) : 60
    if (state.onPeriodLastEndedAt) {
        hasCoolDownPeriodPast = haveSecondsPast(state.onPeriodLastEndedAt, coolDownPeriodSecs)
        if (hasCoolDownPeriodPast == false) {
            def secsLeft = coolDownPeriodSecs - howManySecsPastSince(state.onPeriodLastEndedAt)
            logDebug("Minimum duration between manual on periods not met. Not triggering recirculator. Try again in ${secsLeft} seconds.", "Warning")
            return
        }
    }
    logDebug("Manual override: Recirculator unconditionally on for " + settings["manualOnDuration"] + " minutes.", "Debug")
    turnRecirculatorOn()
    updateTriggerSubscriptionsAndDelayedEvents([])
    unscheduleMaxDurationHandling()
    state.manualOn = true
    state.manualOff = false
    runIn((settings["manualOnDuration"] * 60), "manualOnTimeout", [overwrite: true])
}

def manualOnTimeout() {
    logDebug("Max duration reached for manually turned on recirculator. Resuming normal control.", "Debug")
    state.lastManualOnTime = null
    state.manualOn = false
    state.manualOff = false
    update()
}

def manualOffTimeout() {
    logDebug("Max duration reached for manually turned off recirculator. Resuming normal control.", "Debug")
    state.manualOn = false
    state.manualOff = false
    update()
}

def manualOffHandler(evt) {
    logDebug("Manual Override: Recirculator unconditionally off for " + settings["manualOffDuration"] + " minutes.", "Debug")
    turnRecirculatorOff()
    updateTriggerSubscriptionsAndDelayedEvents([])
    unscheduleMaxDurationHandling()
    state.manualOn = false
    state.manualOff = true
    runIn((settings["manualOffDuration"] * 60), "manualOffTimeout", [overwrite: true])
}

def isRecirculatorOn() {
    if (recircSensedState != null) return recircSensedState.currentSwitch == "on"
    else return recircRelay.currentSwitch == "on"
}

def isRecirculatorOff() {
    if (recircSensedState != null) return recircSensedState.currentSwitch == "off"
    else return recircRelay.currentSwitch == "off"
}

def getTriggersPrioritizedOverSchedule(scheduleId) {
    def triggers = []
    triggerTypes.each { type ->
        if (type == "MotionSensor" && motionSensors && motionOverSchedules && scheduleId in motionOverSchedules) triggers.add(type)
        else if (type == "ArriveSensor" && arrivePresenceSensors && arrivePresenceOverSchedules && scheduleId in arrivePresenceOverSchedules) triggers.add(type)
        else if (type == "DepartSensor" && departPresenceSensors && departPresenceOverSchedules && scheduleId in departPresenceOverSchedules) triggers.add(type)
        else if (type == "OpenSensor" && openContactSensors && openContactOverSchedules && scheduleId in openContactOverSchedules) triggers.add(type)
        else if (type == "CloseSensor" && closeContactSensors && closeContactOverSchedules && scheduleId in closeContactOverSchedules) triggers.add(type)
        else if (type == "Switch" && onSwitches && onSwitchesOverSchedules && scheduleId in onSwitchesOverSchedules) triggers.add(type)
        else if (type == "CustomDevice1" && customDevices1 && customDevices1OverSchedules && scheduleId in customDevices1OverSchedules) triggers.add(type)
        else if (type == "CustomDevice2" && customDevices2 && customDevices2OverSchedules && scheduleId in customDevices2OverSchedules) triggers.add(type)
        else if (type == "TempSensor" && tempTriggerSensors && tempOverSchedules && scheduleId in tempOverSchedules) triggers.add(type)
        else if (type == "MoveSensor" && accelerationSensors && accelerationOverSchedules && scheduleId in accelerationOverSchedules) triggers.add(type)
        else if (type == "FlumeSensor" && flumeDevice && flumeOverSchedules && scheduleId in flumeOverSchedules) triggers.add(type)
    }
    return triggers
}

def getTriggersPrioritizedOverCurrentMode() {
    def triggers = []
    def currentMode = location?.getMode()
    triggerTypes.each { type ->
        if (type == "MotionSensor" && motionSensors && motionOverModes && currentMode in motionOverModes) triggers.add(type)
        else if (type == "ArriveSensor" && arrivePresenceSensors && arrivePresenceOverModes && currentMode in arrivePresenceOverModes) triggers.add(type)
        else if (type == "DepartSensor" && departPresenceSensors && departPresenceOverModes && currentMode in departPresenceOverModes) triggers.add(type)
        else if (type == "OpenSensor" && openContactSensors && openContactOverModes && currentMode in openContactOverModes) triggers.add(type)
        else if (type == "CloseSensor" && closeContactSensors && closeContactOverModes && currentMode in closeContactOverModes) triggers.add(type)
        else if (type == "Switch" && onSwitches && onSwitchesOverModes && currentMode in onSwitchesOverModes) triggers.add(type)
        else if (type == "CustomDevice1" && customDevices1 && customDevices1OverModes && currentMode in customDevices1OverModes) triggers.add(type)
        else if (type == "CustomDevice2" && customDevices2 && customDevices2OverModes && currentMode in customDevices2OverModes) triggers.add(type)
        else if (type == "TempSensor" && tempTriggerSensors && tempOverModes && currentMode in tempOverModes) triggers.add(type)
        else if (type == "MoveSensor" && accelerationSensors && accelerationOverModes && currentMode in accelerationOverModes) triggers.add(type)
        else if (type == "FlumeSensor" && flumeDevice && flumeOverModes && currentMode in flumeOverModes) triggers.add(type)
    }
    return triggers
}

def anyTriggersActive(triggers = triggerTypes, exceptedFromLimits = false) {
    answer = false
    def activeTriggers = []

    if ("MotionSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "MotionSensor" in triggerLimitsExceptions))) {
        def list = motionSensors?.findAll { it?.latestValue("motion") == "active" }
        if (list && list.size() > 0) activeTriggers += list
    }
    
    if ("ArriveSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "ArriveSensor" in triggerLimitsExceptions))) {
        list = arrivePresenceSensors?.findAll { it?.latestValue("presence") == "present" }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("DepartSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "DepartSensor" in triggerLimitsExceptions))) {
        list = departPresenceSensors?.findAll { it?.latestValue("presence") == "not present" }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("OpenSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "OpenSensor" in triggerLimitsExceptions))) {
        list = openContactSensors?.findAll { it?.latestValue("contact") == "open" }
        if (list && list.size() > 0) activeTriggers += list
    }
    
    if ("CloseSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "CloseSensor" in triggerLimitsExceptions))) {
        list = closeContactSensors?.findAll { it?.latestValue("contact") == "closed" }
        if (list && list.size() > 0) activeTriggers += list
    }
    
    if ("Switch" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "Switch" in triggerLimitsExceptions))) {
        list = onSwitches?.findAll { it?.latestValue("switch") == "on" }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("CustomDevice1" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "CustomDevice1" in triggerLimitsExceptions))) {
        list = customDevices1?.findAll { it?.latestValue(customDevice1Attribute) == customDevice1AttributeValue }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("CustomDevice2" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "CustomDevice2" in triggerLimitsExceptions))) {
        list = customDevices2?.findAll { it?.latestValue(customDevice2Attribute) == customDevice2AttributeValue }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("TempSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "TempSensor" in triggerLimitsExceptions))) {
        list = tempTriggerSensors?.findAll { it?.latestValue("temperature") < onWhenBelowTemp }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("MoveSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "MoveSensor" in triggerLimitsExceptions))) {
        list = accelerationSensors?.findAll { it?.latestValue("acceleration") == "active" }
        if (list && list.size() > 0) activeTriggers += list
    }

    if ("FlumeSensor" in triggers && (!exceptedFromLimits || (exceptedFromLimits && "FlumeSensor" in triggerLimitsExceptions))) {
        if (flumeDevice?.latestValue("flowStatus") == "running") activeTriggers += flumeDevice
    }

    if (activeTriggers.size() > 0) {
        answer = true
        logDebug("Active Triggers: " + activeTriggers, "Debug")
    }

    return answer  
}

def anyTriggersExceptedFromLimits(triggers = triggerTypes) {
    answer = false

    if ("MotionSensor" in triggers && "MotionSensor" in triggerLimitsExceptions) answer = true
    if ("ArriveSensor" in triggers && "ArriveSensor" in triggerLimitsExceptions) answer = true
    if ("DepartSensor" in triggers && "DepartSensor" in triggerLimitsExceptions) answer = true
    if ("OpenSensor" in triggers && "OpenSensor" in triggerLimitsExceptions) answer = true
    if ("CloseSensor" in triggers && "CloseSensor" in triggerLimitsExceptions) answer = true
    if ("Switch" in triggers && "Switch" in triggerLimitsExceptions) answer = true
    if ("CustomDevice1" in triggers && "CustomDevice1" in triggerLimitsExceptions) answer = true
    if ("CustomDevice2" in triggers && "CustomDevice2" in triggerLimitsExceptions) answer = true
    if ("TempSensor" in triggers && "TempSensor" in triggerLimitsExceptions) answer = true
    if ("MoveSensor" in triggers && "MoveSensor" in triggerLimitsExceptions) answer = true
    if ("FlumeSensor" in triggers && "FlumeSensor" in triggerLimitsExceptions) answer = true

    return answer  
}

def update(onInitialize = false) {
    logDebug("update()...", "Debug")
    def prioritizedSchedule = getAnyActiveSchedule(true)
    def normalSchedule = getAnyActiveSchedule(false)
    if (state.manualOn) {
        logDebug("Manual Override: Recirculator On; Unsubscribing from triggers and unscheduling max on duration handling.", "Debug")
        turnRecirculatorOn()
        updateTriggerSubscriptionsAndDelayedEvents([])
        unscheduleMaxDurationHandling()
    }
    else if (state.manualOff) {
        logDebug("Manual Override: Recirculator Off; Unsubscribing from triggers and unscheduling max on duration handling.", "Debug")
        turnRecirculatorOff()
        updateTriggerSubscriptionsAndDelayedEvents([])
        unscheduleMaxDurationHandling()
    }
    else if (prioritizedSchedule != null) {  // prioritized schedule active
        if (prioritizedSchedule.state == "on") { // keep on according to prioritized schedule and unsubscribe from all triggers
            logDebug("Prioritized schedule: " + prioritizedSchedule + " Recirculator scheduled to be on, so turning on and unsubscribing from triggers", "Debug")
            turnRecirculatorOn()
            updateTriggerSubscriptionsAndDelayedEvents([])
            unscheduleMaxDurationHandling()
        }
        else if (prioritizedSchedule.state == "off") { 
            def prioritizedTriggers = getTriggersPrioritizedOverSchedule(prioritizedSchedule.scheduleId)
            if (prioritizedTriggers?.size > 0) { // trigger prioritized over prioritized schedule; 
                logDebug("Prioritized schedule: " + prioritizedSchedule + " Recirculator scheduled to be off, but the following triggers are prioritized over this schedule: " + prioritizedTriggers, "Debug")
                if (anyTriggersActive(prioritizedTriggers)) { // turn on if prioritized triggers active
                    logDebug("Prioritized triggers active. Turning recirculator on.", "Debug")
                    turnRecirculatorOn()
                    if (!anyTriggersActive(prioritizedTriggers, true)) {
                        if (settings["triggerOnMaxDuration"] && !state.maxDurationHandlingScheduled) {
                            scheduleMaxDurationHandling()
                            logDebug("No applicable triggers are excepted from limitations. Scheduling max duration handling.", "Debug")
                        }
                    }
                    else if ( anyTriggersActive(prioritizedTriggers, true))  {
                        unscheduleMaxDurationHandling()
                        logDebug("At least one applicable trigger is excepted from limitations. Unscheduling any max duration handling.", "Debug")
                    }
                }
                else if (anyDelayedOffTriggersPending(prioritizedTriggers) && !onInitialize) {
                    logDebug("No prioritized triggers are active, but they have recently been de-activated and turn off from them is pending.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        def pendingPrioritizedTriggers = getPendingDelayedOffTriggers(prioritizedTriggers)
                        if (anyTriggersExceptedFromLimits(pendingPrioritizedTriggers)) {
                            logDebug("Within prioritized scheduled off period: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) are excepted from limits, so unscheduling max duration handling.", "Debug")
                            unscheduleMaxDurationHandling()
                        }
                        else {
                            // keep on but don't unschedule delayed off triggers pending or maxOnDuration; nothing to do
                            // Scenario: triggers dictate on, then triggers dictate off with delay, then schedule changes without changing outcome, then this update() is called --> even if on but no triggers active, needs to keep trigger off with delay and maxOnDuration pending
                            logDebug("Within prioritized scheduled off period: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) that are active are not excepted from limits. Nothing to do.", "Debug")
                        }
                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        unscheduleMaxDurationHandling()
                        logDebug("Unexpected: Recirculator is already off even though prioritized triggers pending off. Keeping off according to prioritized schedule.", "Debug")
                    }                    
                }
                else {
                    logDebug("No prioritized triggers active or pending off, and/or app just initialized.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        // presume recirculator on because mode or schedule just changed; turn off according to schedule
                        logDebug("Recirculator is on, without any prioritized triggers being active or pending off. Turning off according to prioritized schedule.", "Debug")
                        turnRecirculatorOff()
                        unscheduleMaxDurationHandling()
                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        logDebug("Recirculator is already off. Keeping off according to prioritized schedule.", "Debug")
                        unscheduleMaxDurationHandling()
                    }
                }
                updateTriggerSubscriptionsAndDelayedEvents(prioritizedTriggers)
            }
            else { // no trigger prioritized over prioritized schedule
                // keep off and unsubscribe from all triggers
                logDebug("Prioritized schedule  " + prioritizedSchedule + " with no triggers prioritized. Turning off according to prioritized schedule.", "Debug")
                turnRecirculatorOff()
                updateTriggerSubscriptionsAndDelayedEvents([])
                unscheduleMaxDurationHandling()
            }
        }
    }
    else if (inAnySpecifiedMode()) {
        if (inOnMode()) {
            logDebug("Hubitat mode dictates that recirculator be on. Turning on according to mode.", "Debug")
            turnRecirculatorOn()
            updateTriggerSubscriptionsAndDelayedEvents([])
            unscheduleMaxDurationHandling()
        }
        else if (inOffMode()) {
            def prioritizedTriggers = getTriggersPrioritizedOverCurrentMode()
            if (prioritizedTriggers?.size > 0) { // trigger prioritized over current mode; turn on if prioritized triggers active, otherwise turn off according to mode
                logDebug("Hubitat mode ${location?.getMode()} dictates that recirculator be off, except that the following triggers are prioritized over the mode: " + prioritizedTriggers, "Debug")
                if (anyTriggersActive(prioritizedTriggers)) {
                    logDebug("At least some applicable trigger(s) are prioritized and active. Turning recirculator on", "Debug")
                    turnRecirculatorOn()
                    if (!anyTriggersActive(prioritizedTriggers, true)) {
                        if (settings["triggerOnMaxDuration"] && !state.maxDurationHandlingScheduled) {
                            scheduleMaxDurationHandling()
                            logDebug("No applicable triggers are excepted from limitations. Because max duration handling was not already scheduled, scheduling max duration handling now.", "Debug")
                        }
                    }
                    else if ( anyTriggersActive(prioritizedTriggers, true))  {
                        unscheduleMaxDurationHandling()
                        logDebug("At least one applicable trigger is excepted from limitations. Unscheduling any max duration handling.", "Debug")
                    }
                }
                else if (anyDelayedOffTriggersPending(prioritizedTriggers) && !onInitialize) {
                    logDebug("No prioritized triggers are active, but they have recently been de-activated and turn off from them is pending.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        def pendingPrioritizedTriggers = getPendingDelayedOffTriggers(prioritizedTriggers)
                        if (anyTriggersExceptedFromLimits(pendingPrioritizedTriggers)) {
                            logDebug("Within off mode: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) are excepted from limits, so unscheduling max duration handling.", "Debug")
                            unscheduleMaxDurationHandling()
                        }
                        else {
                            // keep on but don't unschedule delayed off triggers pending or maxOnDuration; nothing to do
                            // Scenario: triggers dictate on, then triggers dictate off with delay, then mode changes without changing outcome, then this update() is called --> even if on but no triggers active, needs to keep trigger off with delay and maxOnDuration pending
                            logDebug("Within off mode: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) that are active are not excepted from limits. Nothing to do.", "Debug")
                        }
                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        unscheduleMaxDurationHandling()
                        logDebug("Unexpected: Recirculator is already off even though prioritized triggers pending off. Keeping off according to mode.", "Debug")
                    }                    
                }
                else {
                    logDebug("No prioritized triggers active or pending off, and/or app just initialized.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        // presume recirculator on because mode or schedule just changed; turn off according to mode
                        logDebug("Recirculator is on, without any prioritized triggers being active or pending off. Turning off according to mode.", "Debug")
                        turnRecirculatorOff()
                        unscheduleMaxDurationHandling()
                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        logDebug("Recirculator is already off. Keeping off according to mode.", "Debug")
                        unscheduleMaxDurationHandling()
                    }
                }
                updateTriggerSubscriptionsAndDelayedEvents(prioritizedTriggers)
            }
            else { // no trigger prioritized over current mode; keep off and unsubscribe from all triggers
                logDebug("In off mode with no triggers prioritized. Turning off according to mode.", "Debug")
                turnRecirculatorOff()
                updateTriggerSubscriptionsAndDelayedEvents([])
                unscheduleMaxDurationHandling()
            }
        }
    }
    else if (normalSchedule != null) {  // normal (non-prioritized) schedule active
        if (normalSchedule.state == "on") { // keep on according to schedule and unsubscribe from all triggers
            turnRecirculatorOn()
            updateTriggerSubscriptionsAndDelayedEvents([])
            unscheduleMaxDurationHandling()
            logDebug("Normal schedule: " + normalSchedule + " Recirculator scheduled to be on, so turning on and unsubscribing from triggers", "Debug")
        }
        else if (normalSchedule.state == "off") { 
            def prioritizedTriggers = getTriggersPrioritizedOverSchedule(normalSchedule.scheduleId)
            if (prioritizedTriggers?.size > 0) { // trigger prioritized over schedule; turn on if prioritized triggers active, otherwise turn off according to schedule
                if (anyTriggersActive(prioritizedTriggers)) {
                    if (!anyTriggersActive(prioritizedTriggers, true)) {
                        if (settings["triggerOnMaxDuration"] && !state.maxDurationHandlingScheduled) {
                            scheduleMaxDurationHandling()
                            logDebug("No applicable triggers are excepted from limitations. Scheduling max duration handling.", "Debug")
                        }
                    }
                    else if ( anyTriggersActive(prioritizedTriggers, true))  {
                        unscheduleMaxDurationHandling()
                        logDebug("At least one applicable trigger is excepted from limitations. Unscheduling any max duration handling.", "Debug")
                    }
                    logDebug("Prioritized triggers active. Turning recirculator on.", "Debug")
                    turnRecirculatorOn()
                }
                else if (anyDelayedOffTriggersPending(prioritizedTriggers) && !onInitialize) {
                    logDebug("No prioritized triggers are active, but they have recently been de-activated and turn off from them is pending.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        def pendingPrioritizedTriggers = getPendingDelayedOffTriggers(prioritizedTriggers)
                        if (anyTriggersExceptedFromLimits(pendingPrioritizedTriggers)) {
                            logDebug("Within scheduled off period: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) are excepted from limits, so unscheduling max duration handling.", "Debug")
                            unscheduleMaxDurationHandling()
                        }
                        else {
                            // keep on but don't unschedule delayed off triggers pending or maxOnDuration; nothing to do
                            // Scenario: triggers dictate on, then triggers dictate off with delay, then schedule changes without changing outcome, then this update() is called --> even if on but no triggers active, needs to keep trigger off with delay and maxOnDuration pending
                            logDebug("Within scheduled off period: Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off. Prioritized trigger(s) that are active are not excepted from limits. Nothing to do.", "Debug")
                        }                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        unscheduleMaxDurationHandling()
                        logDebug("Unexpected: Recirculator is already off even though prioritized triggers pending off. Keeping off according to schedule.", "Debug")
                    }                    
                }
                else {
                    logDebug("No prioritized triggers active or pending off, and/or app just initialized.", "Debug")
                    if (isRecirculatorOn()) { // on even though no triggers active
                        // presume recirculator on because mode or schedule just changed; turn off according to mode
                        logDebug("Recirculator is on, without any prioritized triggers being active or pending off. Turning off according to schedule.", "Debug")
                        turnRecirculatorOff()
                        unscheduleMaxDurationHandling()
                    }
                    else { // keep off and unsubscribe from maxOnDuration
                        logDebug("Recirculator is already off. Keeping off according to schedule.", "Debug")
                        unscheduleMaxDurationHandling()
                    }
                }
                updateTriggerSubscriptionsAndDelayedEvents(prioritizedTriggers)
            }
            else { // no trigger prioritized over schedule; keep off and unsubscribe from all triggers
                turnRecirculatorOff()
                updateTriggerSubscriptionsAndDelayedEvents([])
                unscheduleMaxDurationHandling()
                logDebug("Normal schedule active with no triggers prioritized. Turning off according to normal schedule.", "Debug")
            }
        }
    }
    else {
        if (anyTriggersActive()) {
            logDebug("Triggers active. Turning recirculator on.", "Debug")
            turnRecirculatorOn()
            def anyExceptedTriggers = anyTriggersActive(triggerTypes, true)
            if (!anyExceptedTriggers) {
                if (settings["triggerOnMaxDuration"] && !state.maxDurationHandlingScheduled) {
                    logDebug("No applicable triggers are excepted from limitations. Scheduling max duration handling.", "Debug")
                    scheduleMaxDurationHandling()
                }
            }
            else if (anyExceptedTriggers)  {
                logDebug("At least one applicable trigger is excepted from limitations. Unscheduling any max duration handling.", "Debug")
                unscheduleMaxDurationHandling()
            }
        }
        else if (anyDelayedOffTriggersPending() && !onInitialize) {
            logDebug("No triggers are active, but they have recently been de-activated and turn off from them is pending.", "Debug")
            if (isRecirculatorOn()) { // on even though no triggers active
                // keep on but don't unschedule delayed off triggers pending or maxOnDuration; nothing to do
                // Scenario: triggers dictate on, then triggers dictate off with delay, then mode or schedule changes without changing outcome, then this update() is called --> even if on but no triggers active, needs to keep trigger off with delay and maxOnDuration pending
                logDebug("Recirculator on even though no prioritized triggers active, because prioritized triggers have recently become de-activated and are delaying off; Nothing to do.", "Debug")
            }
            else { // keep off and unsubscribe from maxOnDuration
                unscheduleMaxDurationHandling()
                logDebug("Unexpected: Recirculator is already off even though triggers pending off. Keeping off since no triggers active.", "Debug")
            }                    
        }
        else {
            logDebug("No triggers active or pending, and/or app just initialized.", "Debug")
            if (isRecirculatorOn()) { // on even though no triggers active
                // presume recirculator on because mode or schedule just changed; turn off since no triggers active and no off triggers pending
                logDebug("Recirculator is on, without any triggers being active or pending off. Turning off because no triggers active or pending.", "Debug")
                turnRecirculatorOff()
                unscheduleMaxDurationHandling()
            }
            else { // keep off and unsubscribe from all triggers
                logDebug("Recirculator is already off. Keeping off according to lack of triggers being active or pending.", "Debug")
                turnRecirculatorOff()
                unscheduleMaxDurationHandling()
            }
        }
        updateTriggerSubscriptionsAndDelayedEvents()
    }
}

def getApplicableTriggers() {
    def triggers = []

    def prioritizedSchedule = getAnyActiveSchedule(true)
    def normalSchedule = getAnyActiveSchedule(false)

    if (prioritizedSchedule != null && prioritizedSchedule.state == "off") triggers = getTriggersPrioritizedOverSchedule(prioritizedSchedule.scheduleId)
    else if (inOffMode()) triggers = getTriggersPrioritizedOverCurrentMode()     
    else if (normalSchedule != null && normalSchedule.state == "off") triggers = getTriggersPrioritizedOverSchedule(normalSchedule.scheduleId)
    else  triggers = triggerTypes
          
    return triggers  
}

def locationModeHandler(evt) {
    logDebug("Handling location mode event ${evt.value}", "Debug")
    if (!state.manualOn && !state.manualOff) update()
}

Boolean inAnySpecifiedMode() {
    def answer = false
    if (settings["onModes"] && location?.getMode() in settings["onModes"]) answer = true
    if (settings["offModes"] && location?.getMode() in settings["offModes"]) answer = true
    return answer
}

Boolean inOnMode() {
    def answer = false
    if (settings["onModes"] && location?.getMode() in settings["onModes"]) answer = true
    return answer    
}

Boolean inOffMode() {
    def answer = false
    if (settings["offModes"] && location?.getMode() in settings["offModes"]) answer = true
    return answer    
}

def getModeOptions() {
    def modeOptions = []
    if (settings["onModes"]) modeOptions += settings["onModes"]
    if (settings["offModes"]) modeOptions += settings["offModes"]
    return modeOptions
}

<<<<<<< HEAD
def getModeOptions() {
    def modeOptions = []
    if (settings["onModes"]) modeOptions += settings["onModes"]
    if (settings["offModes"]) modeOptions += settings["offModes"]
    return modeOptions
}

=======
>>>>>>> c7864cab24c0a9061fe74fbd7f33e22a9dae563c
// SCHEDULES
def handlePeriodStart(data) {
    
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    logDebug("Handling Start of Period " + periodId + " for schedule " + settings["schedule${scheduleId}Name"], "Debug")

    if (!state.manualOn && !state.manualOff) update()
}

def handlePeriodStop(data) {
    def scheduleId = data.scheduleId
    def periodId = data.periodId as Integer

    logDebug("Handling End of Period " + periodId + " for schedule " + settings["schedule${scheduleId}Name"], "Debug")

    if (!state.manualOn && !state.manualOff) update()
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

Boolean isSchedulePrioritizedOverCurrentMode(scheduleId) {
    def answer = false
    if (settings["schedule${scheduleId}DeprioritizedModes"] && location?.getMode() in settings["schedule${scheduleId}DeprioritizedModes"]) answer = true
  //  logDebug("schedule${scheduleId}DeprioritizedModes = " + settings["schedule${scheduleId}DeprioritizedModes"] + "location?.getMode() = " + location?.getMode() + " answer = " + location?.getMode() in settings["schedule${scheduleId}DeprioritizedModes"], "Debug")
    return answer
}

def getAnyActiveSchedule(prioritizedOverCurrentMode = false) {
    def activeSchedule = null
    if (state.scheduleMap) {
        for (j in state.scheduleMap.keySet()) {
            if (isTodayWithinScheduleDates(j) && isScheduledDayofWeek(j) && (prioritizedOverCurrentMode == false || (prioritizedOverCurrentMode == true && isSchedulePrioritizedOverCurrentMode(j)))) {
                // schedule applicable to today and if prioritizedOverCurrentMode == true is prioritized over current mode
                state.scheduleMap[(j)].eachWithIndex { item, index ->
                    if (isTimeOfDayWithinPeriod(j, index)) {
                        logDebug("Schedule " + settings["schedule${j}Name"] + " has an active period.", "Debug")
                        if (activeSchedule == null) {
                            activeSchedule = [:] 
                            activeSchedule.state = state.scheduleMap[(j)][index].state
                            activeSchedule.scheduleId = j
                            activeSchedule.periodId = index
                        }
                        else logDebug("Schedule " + settings["schedule${j}Name"] + " has an active period, but there are multiple chedules/periods that are active. Ignoring this one.", "Debug")
                    }
                }
            }
        }
    }    
    if (activeSchedule == null) logDebug("No " + (prioritizedOverCurrentMode == true ? "Prioritized" : "Normal") + " Schedules with an active period found.", "Debug")
    return activeSchedule
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


def handleMotionOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenMotionStopsDelay"] ?: 0, "MotionSensor")
}

def handleArrivePresenceOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenAllNotPresentDelay"] ?: 0, "ArriveSensor")
}

def handleDepartPresenceOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenAllPresentDelay"] ?: 0, "DepartSensor")
}

def handleOpenContacteOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenRecloseDelay"] ?: 0, "OpenSensor")
}

def handleCloseContactOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenReopenDelay"] ?: 0, "CloseSensor")
}

def handleOnSwitchOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWithSwitchesDelay"] ?: 0, "Switch")
}

def handleCustomDevices1(evt) {
    if (evt.value == customDevice1AttributeValue) {
        state.customDevice1Triggered = true
        handleTriggerOnEvent(evt)
    }
    else if (evt.value != customDevice1AttributeValue) {
        if (state.customDevice1Triggered && turnOffWithCustomDevice1) {
            handleTriggerOffEvent(evt, settings["turnOffWithCustomDevice1Delay"] ?: 0, "CustomDevice1")
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
            handleTriggerOffEvent(evt, settings["turnOffWithCustomDevice2Delay"] ?: 0, "CustomDevice2")
        }
        state.customDevice2Triggered = false
    }
}

def handleAccelerationOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenStopsMovingDelay"] ?: 0, "MoveSensor")
}

def handleFlumeOff(evt) {
    handleTriggerOffEvent(evt, settings["turnOffWhenFlowStopsDelay"] ?: 0, "FlumeSensor")
}

def handleTriggerOffEvent(evt, turnOffDelay, deviceType) {
    logDebug("handleTriggerOffEvent() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
        if (isRecirculatorOn()) {
            if (turnOffDelay > 0) {
                runIn(turnOffDelay * 60, ("delayedOffHandler" + deviceType), [overwrite: false, data: [deviceLabel: "${evt.device?.label}", deviceId: evt.device?.id]])
                if (state.pendingDelayedOffTriggers == null) state.pendingDelayedOffTriggers = [:]
                if (state.pendingDelayedOffTriggers[deviceType] == null) state.pendingDelayedOffTriggers[deviceType] = []
                if (state.pendingDelayedOffTriggers[deviceType].indexOf(evt.device?.id) == -1) state.pendingDelayedOffTriggers[deviceType].add(evt.device?.id)
                logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period after delay. Scheduled recirculator to turn off in " + turnOffDelay + " minutes. state.pendingDelayedOffTriggers = " + state.pendingDelayedOffTriggers, "Debug")
            }
            else {
                triggerOff()
                logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period.", "Debug")
            } 
        }
        else logDebug("${evt.device?.label} (${evt.device?.id}) triggered off period but recirculator is already off. Nothing to do.", "Debug")
}

def delayedOffHandlerMotionSensor(data) {
    delayedOffHandler(data, "MotionSensor")
}

def delayedOffHandlerArriveSensor(data) {
    delayedOffHandler(data, "ArriveSensor")
}

def delayedOffHandlerDepartSensor(data) {
    delayedOffHandler(data, "DepartSensor")
}

def delayedOffHandlerOpenSensor(data) {
    delayedOffHandler(data, "OpenSensor")
}

def delayedOffHandlerCloseSensor(data) {
    delayedOffHandler(data, "CloseSensor")
}

def delayedOffHandlerSwitch(data) {
    delayedOffHandler(data, "Switch")
}

def delayedOffHandlerCustomDevice1(data) {
    delayedOffHandler(data, "CustomDevice1")
}

def delayedOffHandlerCustomDevice2(data) {
    delayedOffHandler(data, "CustomDevice2")
}

def delayedOffHandlerTempSensor(data) {
    delayedOffHandler(data, "TempSensor")
}

def delayedOffHandlerMoveSensor(data) {
    delayedOffHandler(data, "MoveSensor")
}

def delayedOffHandlerFlumeSensor(data) {
    delayedOffHandler(data, "FlumeSensor")
}

def delayedOffHandler(data, deviceType) {
    logDebug("Executing delayeOffHandler() triggered by ${data.deviceLabel}...", "Trace")
    if (state.pendingDelayedOffTriggers != null && state.pendingDelayedOffTriggers[deviceType] != null && state.pendingDelayedOffTriggers[deviceType].indexOf(data.deviceId) != -1) {
        state.pendingDelayedOffTriggers[deviceType].removeElement(data.deviceId)
        logDebug("Removed ${data.deviceId} from state.pendingDelayedOffTriggers", "Debug")
    }
    triggerOff()
}

def anyDelayedOffTriggersPending(triggers = triggerTypes) {
    def anyPending = false
    state.pendingDelayedOffTriggers.each { deviceType, pendingTriggers ->
        if (deviceType in triggers && pendingTriggers?.size() > 0) anyPending = true
    }
    return anyPending
}

def getPendingDelayedOffTriggers(triggers = triggerTypes) {
    def pending = []
    state.pendingDelayedOffTriggers.each { deviceType, pendingTriggers ->
        if (deviceType in triggers && pendingTriggers?.size() > 0) pending.add(deviceType)
    }
    return pending
}

def isTriggerExcepted(device) {
    def answer = false
    if (device in motionSensors && exceptedFromLimits && "MotionSensor" in triggerLimitsExceptions) answer = true
    else if (device in arrivePresenceSensors && exceptedFromLimits && "ArriveSensor" in triggerLimitsExceptions) answer = true
    else if (device in departPresenceSensors && exceptedFromLimits && "DepartSensor" in triggerLimitsExceptions) answer = true
    else if (device in openContactSensors && exceptedFromLimits && "OpenSensor" in triggerLimitsExceptions) answer = true
    else if (device in closeContactSensors && exceptedFromLimits && "CloseSensor" in triggerLimitsExceptions) answer = true
    else if (device in onSwitches && exceptedFromLimits && "Switch" in triggerLimitsExceptions) answer = true
    else if (device in customDevices1 && exceptedFromLimits && "CustomDevice1" in triggerLimitsExceptions) answer = true
    else if (device in customDevices2 && exceptedFromLimits && "CustomDevice2" in triggerLimitsExceptions) answer = true
    else if (device in tempTriggerSensors && exceptedFromLimits && "TempSensor" in triggerLimitsExceptions) answer = true
    else if (device in accelerationSensors && exceptedFromLimits && "MoveSensor" in triggerLimitsExceptions) answer = true
    else if (device in flumeDevice && exceptedFromLimits && "FlumeSensor" in triggerLimitsExceptions) answer = true
    return answer
}

def handleTriggerOnEvent(evt) {
    logDebug("handleTriggerTriggered() ${evt.device?.label} (${evt.device?.id}) ${evt.name}: ${evt.value}", "Trace")
    logDebug("${evt.device?.label} (${evt.device?.id}) Triggered Recirculator to Turn On", "Debug")
    def isExcepted = isTriggerExcepted(evt.getDevice())
    if (isRecirculatorOff() && state.onPeriodLastEndedAt && !isExcepted && triggerOnCoolDownPeriod != null && triggerOnCoolDownPeriod != 0) {
        def coolDownPeriodSecs = triggerOnCoolDownPeriod.toInteger() * 60
        def hasCoolDownPeriodPast = haveSecondsPast(state.onPeriodLastEndedAt, coolDownPeriodSecs)
        if (hasCoolDownPeriodPast == false) {
            Integer secsLeft = coolDownPeriodSecs - howManySecsPastSince(state.onPeriodLastEndedAt)
            logDebug("Minimum duration between dynamically triggered on periods not met (${secsLeft} seconds left in cooldown period). Not triggering recirculator, but will check again in ${secsLeft} seconds.", "Warning")
            if (secsLeft > 0) runIn(secsLeft, "update")
            return
        }
    }

    cancelDelayedOffTrigger("All")

    turnRecirculatorOn() // will only turn on if already off   

    if (settings["triggerOnMaxDuration"]) {
        def triggers = getApplicableTriggers()
        if (anyTriggersActive(triggers, true))  { 
            // if any active triggers are excepted from the limitations for dynamic triggers, unschedule any pending max duration limitation
            unscheduleMaxDurationHandling()
            logDebug("At least one applicable trigger is active and excepted from limitations. Unscheduling any max duration handling.", "Debug")
        }
        else if (!state.maxDurationHandlingScheduled || (state.maxDurationHandlingScheduled && settings["maxDurationExtendable"])) {
            scheduleMaxDurationHandling()
            logDebug("No applicable triggers are excepted from limitations, and max duration handling is either not already scheduled or needs to be extended. Scheduling max duration handling.", "Debug")
        }
    }
}

def scheduleMaxDurationHandling() {
    runIn((settings["triggerOnMaxDuration"] * 60), "handleTriggerOnMaxDurationReached", [overwrite: true])
    state.maxDurationHandlingScheduled = true
}

def unscheduleMaxDurationHandling() {
    unschedule("handleTriggerOnMaxDurationReached")
    state.maxDurationHandlingScheduled = false
}

def handleTriggerOnMaxDurationReached() {
    logDebug("Executing handleTriggerOnMaxDurationReached() ...", "Trace")
    state.maxDurationHandlingScheduled = false
    triggerOff(true)
}

def triggerOff(maxDurationReached = false) {
    logDebug("Executing triggerOff()...", "Trace")
    if (maxDurationReached) {
        turnRecirculatorOff()
        unscheduleMaxDurationHandling()
        logDebug("Triggered off based on max duration being reached, even if some triggers are still on.", "Debug")
    }
    else {
        def triggers = getApplicableTriggers()
        if (state.pendingDelayedOffTriggers == null || (state.pendingDelayedOffTriggers != null && !anyDelayedOffTriggersPending(triggers))) { // only turn off if no applicable trigger off events still pending
            if (!anyTriggersActive(triggers)) { // turn off if no applicable triggers active
                turnRecirculatorOff()
                unscheduleMaxDurationHandling()
                logDebug("Triggered off based on all applicable triggers being off. Unscheduling max duration handling.", "Debug")
            }
            else logDebug("Triggered off, but not all applicable triggers are off and max on-duration has not yet been reached. Keeping on.", "Debug")
        }
        else logDebug("There are delayed trigger off events still applicable and pending. Waiting to turn off. state.pendingDelayedOffTriggers = " + state.pendingDelayedOffTriggers, "Debug")

        if (!state.maxDurationHandlingScheduled && anyTriggersActive(triggers) && !anyTriggersActive(triggers, true)) scheduleMaxDurationHandling()  // if some triggers are still active, but the active triggers are not excepted from limitations, schedule max duration handling if not already scheduled (handles the case where the trigger just turned off was excepted from limitations and preventing max duration handling)
    }
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
            handleTriggerOffEvent(evt, 0, "TempSensor")
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

private def howManySecsPastSince(timestamp) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.getTime()
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
            logDebug("howManySecsPastSince called with unexpected data type.", "Error")
			return 0
		}
	}
	BigDecimal secsPast = (new Date().getTime() - timestamp) / 1000
    secsPast = secsPast.setScale(0, java.math.RoundingMode.UP)
    return secsPast.intValueExact()
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

