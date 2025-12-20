package com.bm.wschat.feature.supportline.service;

import com.bm.wschat.feature.supportline.dto.request.CreateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.request.UpdateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.response.SpecialistResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineResponse;
import com.bm.wschat.feature.supportline.mapper.SupportLineMapper;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис управления линиями поддержки и их специалистами.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportLineService {

    private final SupportLineRepository supportLineRepository;
    private final UserRepository userRepository;
    private final SupportLineMapper mapper;
    private final UserActivityStatusService userActivityStatusService;

    @Transactional
    public SupportLineResponse createLine(CreateSupportLineRequest request) {
        if (supportLineRepository.findByName(request.name()).isPresent()) {
            throw new EntityExistsException("Линия поддержки с названием '" + request.name() + "' уже существует");
        }

        SupportLine line = mapper.toEntity(request);
        SupportLine saved = supportLineRepository.save(line);

        return toResponseWithSpecialists(saved);
    }

    public SupportLineResponse getLineById(Long id) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(id)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + id));
        return toResponseWithSpecialists(line);
    }

    public List<SupportLineListResponse> getAllLines() {
        List<SupportLine> lines = supportLineRepository.findAllByOrderByDisplayOrderAsc();
        return mapper.toListResponses(lines);
    }

    public List<SupportLineListResponse> getLinesBySpecialist(Long userId) {
        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        List<SupportLine> lines = supportLineRepository.findBySpecialist(specialist);
        return mapper.toListResponses(lines);
    }

    @Transactional
    public SupportLineResponse updateLine(Long id, UpdateSupportLineRequest request) {
        SupportLine line = supportLineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Линия поодержки не найдена: " + id));

        if (request.description() != null) {
            line.setDescription(request.description());
        }
        if (request.slaMinutes() != null) {
            line.setSlaMinutes(request.slaMinutes());
        }
        if (request.assignmentMode() != null) {
            line.setAssignmentMode(request.assignmentMode());
        }
        if (request.displayOrder() != null) {
            line.setDisplayOrder(request.displayOrder());
        }

        SupportLine updated = supportLineRepository.save(line);
        return toResponseWithSpecialists(updated);
    }

    @Transactional
    public void deleteLine(Long id) {
        SupportLine line = supportLineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + id));

        supportLineRepository.delete(line); // Soft delete via @SQLDelete
    }

    @Transactional
    public SupportLineResponse addSpecialist(Long lineId, Long userId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + lineId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("Пользователь не является специалистом");
        }

        line.getSpecialists().add(specialist);
        SupportLine updated = supportLineRepository.save(line);

        return toResponseWithSpecialists(updated);
    }

    @Transactional
    public SupportLineResponse removeSpecialist(Long lineId, Long userId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + lineId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        line.getSpecialists().remove(specialist);
        SupportLine updated = supportLineRepository.save(line);

        return toResponseWithSpecialists(updated);
    }

    /**
     * Получить список специалистов линии с их статусами активности.
     */
    public List<SpecialistResponse> getLineSpecialists(Long lineId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + lineId));

        return toSpecialistResponsesWithStatus(line.getSpecialists());
    }

    // === Private helpers ===

    /**
     * Создаёт SupportLineResponse с заполненным списком специалистов и их
     * статусами.
     */
    private SupportLineResponse toResponseWithSpecialists(SupportLine line) {
        List<SpecialistResponse> specialists = toSpecialistResponsesWithStatus(line.getSpecialists());

        return new SupportLineResponse(
                line.getId(),
                line.getName(),
                line.getDescription(),
                line.getSlaMinutes(),
                line.getAssignmentMode(),
                line.getDisplayOrder(),
                specialists.size(),
                specialists,
                line.getCreatedAt(),
                line.getUpdatedAt());
    }

    /**
     * Конвертирует специалистов в DTO с добавлением их статусов активности.
     */
    private List<SpecialistResponse> toSpecialistResponsesWithStatus(Set<User> specialists) {
        if (specialists == null || specialists.isEmpty()) {
            return List.of();
        }

        return specialists.stream()
                .map(user -> {
                    UserActivityStatus status = userActivityStatusService.getStatus(user.getId());
                    return new SpecialistResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getFio(),
                            user.isActive(),
                            status,
                            status.isAvailableForAssignment());
                })
                .collect(Collectors.toList());
    }
}
