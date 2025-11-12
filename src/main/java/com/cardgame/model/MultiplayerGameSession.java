package com.cardgame.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiplayerGameSession {
    private String sessionId;
    private List<Player> players = new ArrayList<>();
    private int currentCardIndex;
    private List<Card> cards = new ArrayList<>();
    private List<CardVoteStatus> voteStatuses = new ArrayList<>();
    private boolean gameStarted;
    private boolean gameEnded;
    private boolean cardAnswered;
}