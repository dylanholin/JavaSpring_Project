package com.squaregames.api;

import org.springframework.stereotype.Service;

@Service
public class RandomHeartbeat implements HeartbeatSensor {
    
    @Override
    public int get() {
        // Retourne une valeur aléatoire entre 40 et 230
        return 40 + (int)(Math.random() * ((230 - 40) + 1));
    }
}
