/*
 *  DSC Alarm Panel integration via REST API callbacks
 *
 *  Author: Kent Holloway <drizit@gmail.com>
 *  Modified by: Matt Martz <matt.martz@gmail.com>
 */


// Automatically generated. Make future change here.
definition(
    name: "DSC Integration",
    namespace: "",
    author: "Andy Ray",
    description: "Provides conduit to Juggie Alarmserver",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true
)

preferences {

  section("Alarm Panel:") {
    input "panel", "capability.polling", title: "Alarm Panel (required)", multiple: false, required: true
  }
  section("Zone Devices:") {
    input "zonedevices", "capability.polling", title: "DSC Zone Devices", multiple: true, required: false
  }
  section("Control Switches:") {
    input "lights", "capability.switch", title: "Which lights/switches?", multiple: true, required: false
  }
  section("Activate Alarm Strobe:") {
    input "alarms", "capability.alarm", title: "Which Alarm(s)?", multiple: true, required: false
  }
  section("Notifications (optional):") {
    input "sendPush", "enum", title: "Push Notifiation", required: false,
      metadata: [
       values: ["Yes","No"]
      ]
    input "phone1", "phone", title: "Phone Number", required: false
  }
  section("Notification events (optional):") {
    input "notifyEvents", "enum", title: "Which Events?", description: "default (none)", required: false, multiple: true,
     options:
      ['all','alarm','closed','open','closed','partitionready',
       'partitionnotready','partitionarmed','partitionalarm',
       'partitionexitdelay','partitionentrydelay'
      ]
  }
}

mappings {
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "updateZoneOrPartition"
    ]
  }
}

def installed() {
  log.debug "Installed!"
  subscribe(panel)
}

def updated() {
  log.debug "Updated!"
  unsubscribe()
  subscribe(panel)
}

void updateZoneOrPartition() {
  update(panel)
}

private update(panel) {
    // log.debug "update, request: params: ${params} panel: ${panel.name}"
    def zoneorpartition = params.zoneorpart

    // Add more events here as needed
    // Each event maps to a command in your "DSC Panel" device type
    def eventMap = [
      '601':"zone alarm",
      '602':"zone closed",
      '609':"zone open",
      '610':"zone closed",
      '650':"partition ready",
      '651':"partition notready",
      '652':"partition armed",
      '654':"partition alarm",
      '656':"partition exitdelay",
      '657':"partition entrydelay"
    ]

    // get our passed in eventcode
    def eventCode = params.eventcode
    if (eventCode)
    {
      // Lookup our eventCode in our eventMap
      def opts = eventMap."${eventCode}"?.tokenize()
//      log.debug "Options after lookup: ${opts}"
//      log.debug "Zone or partition: $zoneorpartition"
      if (opts[0])
      {
        // We have some stuff to send to the device now
        // this looks something like panel.zone("open", "1")
//        log.debug "Test: ${opts[0]} and: ${opts[1]} for $zoneorpartition"
        panel."${opts[0]}"("${opts[1]}", "$zoneorpartition")
        if ("${opts[0]}" == 'zone') {
          //log.debug "It was a zone...  ${opts[1]}"
            updateZoneDevices(zonedevices,"$zoneorpartition","${opts[1]}")
        }
      }
    }
}

private updateZoneDevices(zonedevices,zonenum,zonestatus) {
    log.debug "zonedevices: $zonedevices - ${zonenum} is ${zonestatus}"
//    log.debug "zonedevices.id are $zonedevices.id"
//    log.debug "zonedevices.displayName are $zonedevices.displayName"
//    log.debug "zonedevices.deviceNetworkId are $zonedevices.deviceNetworkId"
  def zonedevice = zonedevices.find { it.deviceNetworkId == "${zonenum}" }
    if (!zonedevice) {
    
    } else {
      log.debug "Was True... Zone Device: $zonedevice.displayName at $zonedevice.deviceNetworkId is ${zonestatus}"
        //Was True... Zone Device: Front Door Sensor at zone1 is closed
        zonedevice.zone("${zonestatus}")
    }
}

private sendMessage(msg) {
    def newMsg = "Alarm Notification: $msg"
    if (phone1) {
        sendSms(phone1, newMsg)
    }
    if (sendPush == "Yes") {
        sendPush(newMsg)
    }
}