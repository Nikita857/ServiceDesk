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
import com.bm.wschat.feature.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportLineService {

    private final SupportLineRepository supportLineRepository;
    private final UserRepository userRepository;
    private final SupportLineMapper mapper;

    @Transactional
    public SupportLineResponse createLine(CreateSupportLineRequest request) {
        if (supportLineRepository.findByName(request.name()).isPresent()) {
            throw new EntityExistsException("Support line with name '" + request.name() + "' already exists");
        }

        SupportLine line = mapper.toEntity(request);
        SupportLine saved = supportLineRepository.save(line);

        return mapper.toResponse(saved);
    }

    public SupportLineResponse getLineById(Long id) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(id)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + id));
        return mapper.toResponse(line);
    }

    public List<SupportLineListResponse> getAllLines() {
        List<SupportLine> lines = supportLineRepository.findAllByOrderByDisplayOrderAsc();
        return mapper.toListResponses(lines);
    }

    public List<SupportLineListResponse> getLinesBySpecialist(Long userId) {
        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<SupportLine> lines = supportLineRepository.findBySpecialist(specialist);
        return mapper.toListResponses(lines);
    }

    @Transactional
    public SupportLineResponse updateLine(Long id, UpdateSupportLineRequest request) {
        SupportLine line = supportLineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + id));

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
        return mapper.toResponse(updated);
    }

    @Transactional
    public void deleteLine(Long id) {
        SupportLine line = supportLineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + id));

        line.setDeletedAt(Instant.now());
        supportLineRepository.save(line);
    }

    @Transactional
    public SupportLineResponse addSpecialist(Long lineId, Long userId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + lineId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("User is not a specialist");
        }

        line.getSpecialists().add(specialist);
        SupportLine updated = supportLineRepository.save(line);

        return mapper.toResponse(updated);
    }

    @Transactional
    public SupportLineResponse removeSpecialist(Long lineId, Long userId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + lineId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        line.getSpecialists().remove(specialist);
        SupportLine updated = supportLineRepository.save(line);

        return mapper.toResponse(updated);
    }

    public List<SpecialistResponse> getLineSpecialists(Long lineId) {
        SupportLine line = supportLineRepository.findByIdWithSpecialists(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + lineId));

        return mapper.toSpecialistResponses(line.getSpecialists());
    }
}
