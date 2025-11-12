package com.cardgame.service;

import com.cardgame.model.*;
import com.cardgame.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {
    private final CardRepository cardRepository;
    private final Map<String, MultiplayerGameSession> sessions = new ConcurrentHashMap<>();

    public MultiplayerGameSession startSession(int numberOfCards) {
        List<Card> allCards = cardRepository.findAll();

        if (allCards.size() < numberOfCards) {
            throw new IllegalStateException("Недостаточно карточек в базе данных. Требуется: " + numberOfCards + ", доступно: " + allCards.size());
        }

        Collections.shuffle(allCards);
        List<Card> selectedCards = allCards.stream()
                .limit(numberOfCards)
                .collect(Collectors.toList());

        MultiplayerGameSession session = new MultiplayerGameSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCards(selectedCards);
        session.setCurrentCardIndex(0);
        session.setVoteStatuses(new ArrayList<>());
        session.setGameStarted(false);
        session.setGameEnded(false);
        session.setCardAnswered(false);

        sessions.put(session.getSessionId(), session);
        return session;
    }

    public Player joinSession(String sessionId, String playerName) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Сессия не найдена");
        }

        if (session.getPlayers().size() >= 5) {
            throw new IllegalStateException("В сессии уже максимальное количество игроков (5)");
        }

        PlayerRole role = session.getPlayers().isEmpty() ? PlayerRole.ANSWERER : PlayerRole.REVIEWER;
        Player player = new Player(UUID.randomUUID().toString(), playerName, role);
        session.getPlayers().add(player);

        if (session.getPlayers().size() > 1) {
            session.setGameStarted(true);
        }

        return player;
    }

    public MultiplayerGameSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Card getCurrentCard(String sessionId) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null || session.getCurrentCardIndex() >= session.getCards().size()) {
            return null;
        }
        return session.getCards().get(session.getCurrentCardIndex());
    }

    public void markCardAnswered(String sessionId) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Сессия не найдена");
        }

        session.setCardAnswered(true);

        if (session.getVoteStatuses().size() <= session.getCurrentCardIndex()) {
            Card currentCard = getCurrentCard(sessionId);
            if (currentCard != null) {
                session.getVoteStatuses().add(new CardVoteStatus(currentCard.getId(), new ArrayList<>()));
            }
        }
    }

    public void vote(String sessionId, String playerId, boolean accepted) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Сессия не найдена");
        }

        int idx = session.getCurrentCardIndex();
        if (idx >= session.getVoteStatuses().size()) {
            throw new IllegalStateException("Статус голосования не инициализирован");
        }

        List<Vote> votes = session.getVoteStatuses().get(idx).getVotes();

        if (votes.stream().anyMatch(v -> v.getPlayerId().equals(playerId))) {
            throw new IllegalArgumentException("Этот игрок уже проголосовал");
        }

        votes.add(new Vote(playerId, accepted));
    }

    public VotingStatus getVotingStatus(String sessionId) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Сессия не найдена");
        }

        int reviewers = (int) session.getPlayers().stream()
                .filter(p -> p.getRole() == PlayerRole.REVIEWER)
                .count();

        int idx = session.getCurrentCardIndex();
        int votes = 0;

        if (idx < session.getVoteStatuses().size()) {
            votes = session.getVoteStatuses().get(idx).getVotes().size();
        }

        return new VotingStatus(votes, reviewers);
    }

    public boolean allReviewersVoted(String sessionId) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }

        int reviewers = (int) session.getPlayers().stream()
                .filter(p -> p.getRole() == PlayerRole.REVIEWER)
                .count();

        int idx = session.getCurrentCardIndex();
        if (idx >= session.getVoteStatuses().size()) {
            return false;
        }

        int votes = session.getVoteStatuses().get(idx).getVotes().size();
        return votes == reviewers && reviewers > 0;
    }

    public void nextCard(String sessionId) {
        MultiplayerGameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Сессия не найдена");
        }

        if (!allReviewersVoted(sessionId)) {
            throw new IllegalStateException("Голосование не завершено");
        }

        session.setCardAnswered(false);
        session.setCurrentCardIndex(session.getCurrentCardIndex() + 1);

        if (session.getCurrentCardIndex() >= session.getCards().size()) {
            session.setGameEnded(true);
        }
    }

    public void endSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public void initializeCards() {
        if (cardRepository.count() == 0) {
            List<Card> defaultCards = Arrays.asList(
                    new Card(null, "Что такое Java?", "Java — объектно-ориентированный язык программирования, разработанный компанией Sun Microsystems."),
                    new Card(null, "Что такое Spring?", "Spring — фреймворк для разработки приложений на Java, упрощающий создание корпоративных приложений."),
                    new Card(null, "Что такое REST API?", "REST API — архитектурный стиль для веб-сервисов, использующий HTTP методы (GET, POST, PUT, DELETE)."),
                    new Card(null, "Что такое микросервисы?", "Микросервисы — архитектурный подход, где большое приложение разделено на малые независимые сервисы."),
                    new Card(null, "Что такое базы данных?", "Базы данных — организованные наборы данных, хранящиеся на компьютере для быстрого доступа и управления."),
                    new Card(null, "Что такое HTTP?", "HTTP — протокол передачи гипертекста для веб-коммуникации между клиентом и сервером."),
                    new Card(null, "Что такое JSON?", "JSON — формат обмена данными, основанный на текстовом представлении структурированной информации."),
                    new Card(null, "Что такое Git?", "Git — система контроля версий для отслеживания изменений в исходном коде и совместной разработки."),
                    new Card(null, "Что такое Docker?", "Docker — платформа для контейнеризации приложений, позволяющая упаковывать приложения с их зависимостями."),
                    new Card(null, "Что такое SQL?", "SQL — язык для работы с реляционными базами данных, позволяющий создавать, читать и обновлять данные."),
                    new Card(null, "Что такое API?", "API — интерфейс программирования приложений для взаимодействия между различными программами."),
                    new Card(null, "Что такое фронтенд?", "Фронтенд — клиентская часть приложения, с которой взаимодействует пользователь (HTML, CSS, JavaScript)."),
                    new Card(null, "Что такое бэкенд?", "Бэкенд — серверная часть приложения, которая обрабатывает запросы от клиента и управляет данными."),
                    new Card(null, "Что такое кэширование?", "Кэширование — сохранение данных в быстро доступном хранилище для уменьшения времени доступа."),
                    new Card(null, "Что такое аутентификация?", "Аутентификация — процесс проверки личности пользователя путем проверки учетных данных (пароль, токен).")
            );
            cardRepository.saveAll(defaultCards);
        }
    }
}