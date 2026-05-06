package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.request.InterviewRequest;
import com.sefirr.hiretrack.dto.response.InterviewResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.entity.Interview;
import com.sefirr.hiretrack.exception.ResourceNotFoundException;
import com.sefirr.hiretrack.exception.UnauthorizedException;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import com.sefirr.hiretrack.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    public List<InterviewResponse> getByApplication(UUID applicationId, UUID userId) {
        verifyApplicationOwnership(applicationId, userId);
        return interviewRepository.findByApplicationId(applicationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public InterviewResponse create(UUID applicationId, UUID userId, InterviewRequest request) {
        Application app = verifyApplicationOwnership(applicationId, userId);
        Interview interview = Interview.builder()
                .application(app)
                .type(request.getType())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .platform(request.getPlatform())
                .interviewerName(request.getInterviewerName())
                .notes(request.getNotes())
                .build();
        return toResponse(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse update(UUID id, UUID userId, InterviewRequest request) {
        Interview interview = findOwned(id, userId);
        interview.setType(request.getType());
        interview.setScheduledAt(request.getScheduledAt());
        interview.setDurationMinutes(request.getDurationMinutes());
        interview.setPlatform(request.getPlatform());
        interview.setInterviewerName(request.getInterviewerName());
        interview.setNotes(request.getNotes());
        return toResponse(interviewRepository.save(interview));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        Interview interview = findOwned(id, userId);
        interviewRepository.delete(interview);
    }

    private Interview findOwned(UUID id, UUID userId) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
        verifyApplicationOwnership(interview.getApplication().getId(), userId);
        return interview;
    }

    private Application verifyApplicationOwnership(UUID applicationId, UUID userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
        if (!app.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return app;
    }

    private InterviewResponse toResponse(Interview i) {
        return InterviewResponse.builder()
                .id(i.getId())
                .applicationId(i.getApplication().getId())
                .type(i.getType())
                .scheduledAt(i.getScheduledAt())
                .durationMinutes(i.getDurationMinutes())
                .platform(i.getPlatform())
                .interviewerName(i.getInterviewerName())
                .notes(i.getNotes())
                .feedback(i.getFeedback())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
