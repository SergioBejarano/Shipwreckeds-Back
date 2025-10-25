package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private String code;
    private long timestamp;
    private int timerSeconds;
    private Island island;
    private List<AvatarState> avatars;
    private double fuelPercentage;
    private String status;
    private Boat boat;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Island {
        private double cx;
        private double cy;
        private double radius;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Boat {
        private double x;
        private double y;
        private double interactionRadius;
    }
}
