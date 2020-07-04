/**
 *  Its too humid!
 *
 *  Copyright 2014 Brian Critchlow
 *  Based on Its too cold code by SmartThings
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
definition(
    name: "Humidity Alert!",
    namespace: "keltypack",
    author: "Kevin Smart",
    description: "Automatically turn on a fan based on the ambient humidity level.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances2-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances2-icn@2x.png",
    pausable: true
)


preferences {
	section("Monitor the humidity of:") {
		input "humiditySensor1", "capability.relativeHumidityMeasurement"
	}
	section("When the humidity rises above:") {
		input "on_humidity", "number", title: "Percentage ?"
	}
    section("When the humidity falls below:") {
		input "off_humidity", "number", title: "Percentage ?"
	}
    section( "Notifications" ) {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
        input "phone1", "phone", title: "Send a Text Message?", required: false
    }
	section("Control this switch:") {
		input "switch1", "capability.switch", required: false
	}
    section("Time out - min :") {
		input "timeout", "number", title: "Timeout for no humiditity updates when running?", required: false, defaultValue: 30
	}
  	section("Only when this switch is OFF") {
		input "switch2", "capability.switch", title: "this switch when on will stop the app", required: false
	}
}

def installed() {
	installer()
}
def updated() {
	unsubscribe()
    installer()
}
def installer(){
log.info "installer"
	subscribe(humiditySensor1, "humidity", humidityHandler)
    subscribe(switch1, "switch", switchstate)
}

def switchstate(evt){
	log.info "HumSwitchEvt '${evt.device}' is '${evt.value}', App is '${state.appstate}'last humidity was '${state?.humstate}'"
	state.sstate = evt.value
    if (state.appstate == "on"){
    	if (switch2 != null && switch2?.currentSwitch == "on" ) {
    		state.appstate = "off"
    		log.warn "Switch event - override switch is on app is now '${state.appstate}'"
        }
    	else if (evt.value == "off"){
    		log.trace "${switch1.label} switch is '${evt.value}' , but App is still '${state.appstate}' turning on - Should be on event and app on"
    		switch1?.on()
    	sendEvent (name:"Switch Event",  value:"${switch1.label} is ${evt.value}")
        }
	}
}

def humidityHandler(evt) {
	def currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
    def traceaction = ""
    state.humstate = currentHumidity

	if (switch2 != null && switch2?.currentSwitch == "on" ){
    	Log.info "${switch2} & ${switch2?.currentSwitch}"
    	if (state.appstate == "on" ){
        	state.appstate = "off"
             log.warn "Humidity event - override switch is turning app off"
        }
    }
    else {
//High ------------------------
	if (currentHumidity >= settings.on_humidity) {
		if (state.sstate == "on" && state.appstate == "on"){ // everything on - Don't send a continuous stream of text messages
        	traceaction = "High - everything on already"
        }
		else if (state.sstate == "on" && state.appstate == "off"){ //fan on for some other reason
        	traceaction = "High - switch on but app not send message anyway turning app on"
            send("${humiditySensor1.label} sensed high humidity level of ${evt.value}")
            state.appstate = "on"
        }
        else { //app off and/or switch off
        	state.appstate = "on"
			traceaction = "High - turning on"
			send("${humiditySensor1.label} sensed high humidity level of ${evt.value}")
            switch1?.on()
		}
		timelag() //if no humidity update in period of time turn app and fan off
	}
//Low -------------------
    else if (currentHumidity <= settings.off_humidity) {
		if (state.sstate == "off" && state.appstate == "off"){
			traceaction = "Low - All off - no action"
        }
        else if (state.appstate == "on"){
        	if (state.sstate == "on") {
        		traceaction = "Low turning off ${settings?.switch1}"
            	switch1?.off()
        	}
            else if (state.sstate == "off"){
        		traceaction = "Low - turning off APP"
        	}
            send("${humiditySensor1.label} sensed LOW humidity level of ${evt.value} and de-activating ${settings?.switch1}")
            state.appstate = "off"
            unschedule (timeoff)
        }
	}
// middle --------------------------
    else {
    	if (state.sstate == "off" && state.appstate == "on"){
        	traceaction = "MID - Switch off but app still on, turning on again, reset timer"
        	switch1?.on()
            timelag()
     	}
        else if (state.appstate == "on"){
        	traceaction = "MID - No action but reset timer"
        timelag()
        }
	}
    log.trace "Humidity Event - ${traceaction}, Reported humidity: '${evt.value}%', Switch is-'${state.sstate}', App State is-'${state.appstate}'"
    sendEvent (name:"Humidity Event",  value:"${humiditySensor1.label} is ${evt.value}% - ${traceaction}")
  }
}

private send(msg) {
    if ( sendPushMessage != "No" ) {
        log.debug( "sending push message" )
        sendPush( msg )
    }

    if ( phone1 ) {
        log.debug( "sending text message" )
        sendSms( phone1, msg )
    }
    log.debug msg
}

def timelag(){
	runIn (timeout*60 ?: 30*60, timeoff)
}

def timeoff (){ // don't get a humidity report for 30 min or defined time
	def timelag = timeout*60 ?: 30*60
	sendPush( "time off initated no events for '$timelag' min" )
	log.warn "timeoff initated no events for '$timelag' min" 
    if (state.appstate == "on"){
    	state.appstate = "off"
        if (state.sstate == "on"){
        	switch1?.off()
        }
	}
}