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
public class VoteStart {
    private List<AvatarState> options; // who can be voted out (NPCs and possibly players)
    private String message;
    private int durationSeconds;
}
