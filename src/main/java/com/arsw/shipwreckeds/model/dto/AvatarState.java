package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvatarState {
    private Long id;
    private String type; // "human" | "npc"
    private String ownerUsername; // null for NPC
    private double x;
    private double y;
    private boolean isInfiltrator;
    private boolean isAlive;
    private String displayName; // visible label to show in UI (NPC-xxxx or username)
}
