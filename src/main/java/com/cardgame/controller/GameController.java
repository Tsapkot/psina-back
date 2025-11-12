package com.cardgame.controller;

import com.cardgame.model.*;
import com.cardgame.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @PostMapping("/start-session")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Integer> request) {
        try {
            int numberOfCards = request.getOrDefault("numberOfCards", 5);
            MultiplayerGameSession session = gameService.startSession(numberOfCards);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/join-session")
    public ResponseEntity<?> joinSession(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            String playerName = request.get("playerName");

            Player player = gameService.joinSession(sessionId, playerName);
            MultiplayerGameSession session = gameService.getSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("playerId", player.getPlayerId());
            response.put("role", player.getRole());
            response.put("players", session.getPlayers());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/card/{sessionId}")
    public ResponseEntity<?> getCard(@PathVariable String sessionId) {
        try {
            MultiplayerGameSession session = gameService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            if (session.isGameEnded()) {
                return ResponseEntity.ok(Map.of("gameEnded", true));
            }

            Card card = gameService.getCurrentCard(sessionId);
            if (card == null) {
                return ResponseEntity.ok(Map.of("gameEnded", true));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("question", card.getQuestion());
            response.put("answer", card.getAnswer());
            response.put("cardIndex", session.getCurrentCardIndex() + 1);
            response.put("totalCards", session.getCards().size());
            response.put("cardAnswered", session.isCardAnswered());
            response.put("gameEnded", false);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/answer/{sessionId}")
    public ResponseEntity<?> answer(@PathVariable String sessionId) {
        try {
            gameService.markCardAnswered(sessionId);
            return ResponseEntity.ok(Map.of("status", "answer recorded"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/vote/{sessionId}")
    public ResponseEntity<?> vote(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        try {
            String playerId = (String) request.get("playerId");
            boolean accepted = (Boolean) request.get("accepted");
            gameService.vote(sessionId, playerId, accepted);
            return ResponseEntity.ok(Map.of("status", "vote recorded"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/votes/{sessionId}")
    public ResponseEntity<?> votes(@PathVariable String sessionId) {
        try {
            VotingStatus votingStatus = gameService.getVotingStatus(sessionId);
            return ResponseEntity.ok(votingStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/next-card/{sessionId}")
    public ResponseEntity<?> nextCard(@PathVariable String sessionId) {
        try {
            if (!gameService.allReviewersVoted(sessionId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Не все проверяющие проголосовали"));
            }

            gameService.nextCard(sessionId);
            MultiplayerGameSession session = gameService.getSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            if (session.isGameEnded()) {
                response.put("gameEnded", true);
                response.put("totalCards", session.getCards().size());
            } else {
                Card nextCard = gameService.getCurrentCard(sessionId);
                response.put("gameEnded", false);
                response.put("question", nextCard.getQuestion());
                response.put("answer", nextCard.getAnswer());
                response.put("cardIndex", session.getCurrentCardIndex() + 1);
                response.put("totalCards", session.getCards().size());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/end-session/{sessionId}")
    public ResponseEntity<?> endSession(@PathVariable String sessionId) {
        try {
            gameService.endSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session ended"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}