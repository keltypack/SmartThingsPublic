/**
 *  LockDoors
 *
 *  Copyright 2020 Kevin Smart
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
definition(
    name: "LockDoors",
    namespace: "keltypack",
    author: "Kevin Smart",
    description: "Lock Doors with a button",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Button to Push") {
        input "button", "capability.switch", title: "Which button?", multiple: false
    }
    
    section("Doors to Lock") {
		input "locks", "capability.lock", title: "Which doors?", multiple: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(button, "switch.off", switchOff, [filterEvents: false])
}

// TODO: implement event handlers
def buttonPushed(evt) {
	log.debug "switch on"
}

def switchOff(evt) {
	log.debug "switch off"
    locks?.each { lock ->
        def lockName = lock.displayName
     	def lockValue = lock.currentState("lock").value
        
    	log.debug "Lock: ${lockName}, Value: ${lockValue}"
    
    	if (lockValue != "locked") {
	    	log.debug "Locking: ${lockName}, Current state: ${lockValue}"
   			lock.lock()
        }
        button.off();
    }
}

// TODO: implement event handlers