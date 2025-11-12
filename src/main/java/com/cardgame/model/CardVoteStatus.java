package com.cardgame.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardVoteStatus {
    private Long cardId;
    private List<Vote> votes;
}