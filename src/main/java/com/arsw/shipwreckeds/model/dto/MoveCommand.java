package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveCommand {
    private String username;
    private Long avatarId;
    private Direction direction;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Direction {
        private double dx;
        private double dy;
    }
}
