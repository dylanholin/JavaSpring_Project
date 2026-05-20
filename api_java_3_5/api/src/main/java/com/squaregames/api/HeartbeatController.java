package com.squaregames.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartbeatController {

    private final HeartbeatSensor heartbeatSensor;

    // L'injection par constructeur est recommandée (comme noté dans tes instructions)
    @Autowired
    public HeartbeatController(HeartbeatSensor heartbeatSensor) {
        this.heartbeatSensor = heartbeatSensor;
    }

    @GetMapping("/heartbeat")
    public int getHeartbeat() {
        return heartbeatSensor.get();
    }
}
