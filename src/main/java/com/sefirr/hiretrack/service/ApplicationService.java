package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.request.ApplicationRequest;
import com.sefirr.hiretrack.dto.request.StatusUpdateRequest;
import com.sefirr.hiretrack.dto.response.ApplicationResponse;
import com.sefirr.hiretrack.dto.response.PageResponse;
import com.sefirr.hiretrack.config.CacheNames;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.entity.Document;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.exception.ResourceNotFoundException;
import com.sefirr.hiretrack.exception.UnauthorizedException;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import com.sefirr.hiretrack.repository.UserRepository;
import com.sefirr.hiretrack.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    // The key must include every argument that changes the result (page size, sort and date range included)
    @Cacheable(cacheNames = CacheNames.APPLICATIONS,
            key = "#userId + ':' + #pageable + ':' + #status + ':' + #company + ':' + #remote + ':' + #from + ':' + #to")
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getAll(
            UUID userId, Pageable pageable,
            ApplicationStatus status, String company,
            Boolean remote, LocalDate from, LocalDate to
    ) {
        Page<ApplicationResponse> page = applicationRepository
                .findAllByUserIdWithFilters(userId, status, company, remote, from, to, pageable)
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id, UUID userId) {
        return toResponse(findOwned(id, userId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.APPLICATIONS, allEntries = true),
            @CacheEvict(cacheNames = {CacheNames.STATS_SUMMARY, CacheNames.STATS_TIMELINE, CacheNames.STATS_FUNNEL}, key = "#userId")
    })
    @Transactional
    public ApplicationResponse create(UUID userId, ApplicationRequest request) {
        User user = userRepository.getReferenceById(userId);
        Application app = Application.builder()
                .user(user)
                .company(request.getCompany())
                .role(request.getRole())
                .status(ApplicationStatus.APPLIED)
                .jobUrl(request.getJobUrl())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .location(request.getLocation())
                .remote(Boolean.TRUE.equals(request.getRemote()))
                .notes(request.getNotes())
                .appliedAt(request.getAppliedAt())
                .build();
        return toResponse(applicationRepository.save(app));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.APPLICATIONS, allEntries = true),
            @CacheEvict(cacheNames = {CacheNames.STATS_SUMMARY, CacheNames.STATS_TIMELINE, CacheNames.STATS_FUNNEL}, key = "#userId")
    })
    @Transactional
    public ApplicationResponse update(UUID id, UUID userId, ApplicationRequest request) {
        Application app = findOwned(id, userId);
        app.setCompany(request.getCompany());
        app.setRole(request.getRole());
        app.setJobUrl(request.getJobUrl());
        app.setSalaryMin(request.getSalaryMin());
        app.setSalaryMax(request.getSalaryMax());
        app.setLocation(request.getLocation());
        app.setRemote(Boolean.TRUE.equals(request.getRemote()));
        app.setNotes(request.getNotes());
        app.setAppliedAt(request.getAppliedAt());
        return toResponse(applicationRepository.save(app));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.APPLICATIONS, allEntries = true),
            @CacheEvict(cacheNames = {CacheNames.STATS_SUMMARY, CacheNames.STATS_TIMELINE, CacheNames.STATS_FUNNEL}, key = "#userId")
    })
    @Transactional
    public ApplicationResponse updateStatus(UUID id, UUID userId, StatusUpdateRequest request) {
        Application app = findOwned(id, userId);
        app.setStatus(request.getStatus());
        return toResponse(applicationRepository.save(app));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.APPLICATIONS, allEntries = true),
            @CacheEvict(cacheNames = {CacheNames.STATS_SUMMARY, CacheNames.STATS_TIMELINE, CacheNames.STATS_FUNNEL}, key = "#userId")
    })
    @Transactional
    public void delete(UUID id, UUID userId) {
        Application app = findOwned(id, userId);
        List<String> storedFiles = app.getDocuments().stream().map(Document::getStoragePath).toList();
        applicationRepository.delete(app);
        applicationRepository.flush();
        // Document rows are removed by cascade; the files on disk must be removed explicitly
        storedFiles.forEach(storageService::delete);
    }

    private Application findOwned(UUID id, UUID userId) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        if (!app.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return app;
    }

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .company(app.getCompany())
                .role(app.getRole())
                .status(app.getStatus())
                .jobUrl(app.getJobUrl())
                .salaryMin(app.getSalaryMin())
                .salaryMax(app.getSalaryMax())
                .location(app.getLocation())
                .remote(app.getRemote())
                .notes(app.getNotes())
                .appliedAt(app.getAppliedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
