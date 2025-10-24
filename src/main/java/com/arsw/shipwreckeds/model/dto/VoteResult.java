package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    private Map<Long, Integer> counts;
    private Long expelledId;
    private String expelledType; // "npc" or "human"
    private String message;
}
