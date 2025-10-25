package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EliminationEvent {
    private long targetId;
    private String targetUsername;
    private String message;
}
