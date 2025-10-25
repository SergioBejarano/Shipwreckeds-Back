package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FuelActionRequest {
    public enum Action {
        FILL,
        SABOTAGE
    }

    private String username;
    private Action action;
    private Double amount;
}
