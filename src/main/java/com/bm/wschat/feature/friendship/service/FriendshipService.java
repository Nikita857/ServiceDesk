package com.bm.wschat.feature.friendship.service;

import com.bm.wschat.feature.friendship.dto.response.FriendResponse;
import com.bm.wschat.feature.friendship.dto.response.FriendshipResponse;
import com.bm.wschat.feature.friendship.model.Friendship;
import com.bm.wschat.feature.friendship.model.FriendshipStatus;
import com.bm.wschat.feature.friendship.repository.FriendshipRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.dto.UserShortResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    /**
     * Отправить запрос в друзья
     */
    @Transactional
    public FriendshipResponse sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Нельзя отправить запрос самому себе");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + requesterId));

        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + addresseeId));

        // Проверяем, нет ли уже связи между пользователями
        friendshipRepository.findBetweenUsers(requesterId, addresseeId)
                .ifPresent(existing -> {
                    switch (existing.getStatus()) {
                        case ACCEPTED -> throw new IllegalStateException("Вы уже друзья");
                        case PENDING -> throw new IllegalStateException("Запрос уже отправлен");
                        case BLOCKED -> throw new AccessDeniedException("Невозможно отправить запрос");
                        case REJECTED -> {
                        } // Можно повторно отправить
                    }
                });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        log.info("Friend request sent from {} to {}", requesterId, addresseeId);
        return toResponse(saved);
    }

    /**
     * Принять запрос в друзья
     */
    @Transactional
    public FriendshipResponse acceptRequest(Long friendshipId, Long userId) {
        Friendship friendship = getFriendshipById(friendshipId);

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new AccessDeniedException("Только получатель может принять запрос");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Запрос уже обработан");
        }

        friendship.accept();
        Friendship saved = friendshipRepository.save(friendship);
        log.info("Friend request {} accepted by user {}", friendshipId, userId);
        return toResponse(saved);
    }

    /**
     * Отклонить запрос в друзья
     */
    @Transactional
    public FriendshipResponse rejectRequest(Long friendshipId, Long userId) {
        Friendship friendship = getFriendshipById(friendshipId);

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new AccessDeniedException("Только получатель может отклонить запрос");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Запрос уже обработан");
        }

        friendship.reject();
        Friendship saved = friendshipRepository.save(friendship);
        log.info("Friend request {} rejected by user {}", friendshipId, userId);
        return toResponse(saved);
    }

    /**
     * Удалить из друзей
     */
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(userId, friendId)
                .orElseThrow(() -> new EntityNotFoundException("Дружба не найдена"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Пользователи не являются друзьями");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed between {} and {}", userId, friendId);
    }

    /**
     * Заблокировать пользователя
     */
    @Transactional
    public FriendshipResponse blockUser(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + blockerId));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + targetId));

        Friendship friendship = friendshipRepository.findBetweenUsers(blockerId, targetId)
                .orElse(Friendship.builder()
                        .requester(blocker)
                        .addressee(target)
                        .build());

        friendship.block();
        Friendship saved = friendshipRepository.save(friendship);
        log.info("User {} blocked user {}", blockerId, targetId);
        return toResponse(saved);
    }

    /**
     * Получить список друзей
     */
    public List<FriendResponse> getFriends(Long userId) {
        return friendshipRepository.findFriendsOfUser(userId).stream()
                .map(f -> {
                    User friend = f.getRequester().getId().equals(userId)
                            ? f.getAddressee()
                            : f.getRequester();
                    return new FriendResponse(
                            f.getId(),
                            new UserShortResponse(friend.getId(), friend.getUsername(), friend.getFio()));
                })
                .toList();
    }

    /**
     * Получить входящие запросы в друзья
     */
    public List<FriendshipResponse> getPendingRequests(Long userId) {
        return friendshipRepository.findPendingRequestsForUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Получить исходящие запросы в друзья
     */
    public List<FriendshipResponse> getOutgoingRequests(Long userId) {
        return friendshipRepository.findOutgoingRequestsForUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Проверить, являются ли пользователи друзьями
     */
    public boolean areFriends(Long userId1, Long userId2) {
        return friendshipRepository.areFriends(userId1, userId2);
    }

    /**
     * Количество входящих запросов
     */
    public long getPendingRequestsCount(Long userId) {
        return friendshipRepository.countPendingRequests(userId);
    }

    // --- Helper methods ---

    private Friendship getFriendshipById(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запрос дружбы не найден: " + id));
    }

    private FriendshipResponse toResponse(Friendship f) {
        return new FriendshipResponse(
                f.getId(),
                new UserShortResponse(f.getRequester().getId(), f.getRequester().getUsername(),
                        f.getRequester().getFio()),
                new UserShortResponse(f.getAddressee().getId(), f.getAddressee().getUsername(),
                        f.getAddressee().getFio()),
                f.getStatus(),
                f.getRequestedAt(),
                f.getRespondedAt());
    }
}
