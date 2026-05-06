package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.request.ApplicationRequest;
import com.sefirr.hiretrack.dto.response.ApplicationResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.enums.Role;
import com.sefirr.hiretrack.exception.ResourceNotFoundException;
import com.sefirr.hiretrack.exception.UnauthorizedException;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import com.sefirr.hiretrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ApplicationService applicationService;

    private User owner;
    private User other;
    private Application application;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").role(Role.USER).build();
        other = User.builder().id(UUID.randomUUID()).email("other@test.com").role(Role.USER).build();
        application = Application.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .company("Acme Corp")
                .role("Software Engineer")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getById_returnsApplication_whenOwnedByUser() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getById(application.getId(), owner.getId());

        assertThat(response.getCompany()).isEqualTo("Acme Corp");
        assertThat(response.getRole()).isEqualTo("Software Engineer");
    }

    @Test
    void getById_throws403_whenNotOwnedByUser() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.getById(application.getId(), other.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_throws404_whenNotFound() {
        UUID randomId = UUID.randomUUID();
        when(applicationRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(randomId, owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(userRepository.getReferenceById(owner.getId())).thenReturn(owner);
        when(applicationRepository.save(any())).thenReturn(application);

        ApplicationRequest request = new ApplicationRequest();
        request.setCompany("Acme Corp");
        request.setRole("Software Engineer");
        request.setAppliedAt(LocalDate.now());

        ApplicationResponse response = applicationService.create(owner.getId(), request);

        assertThat(response.getCompany()).isEqualTo("Acme Corp");
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void delete_removesApplication_whenOwned() {
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        applicationService.delete(application.getId(), owner.getId());

        verify(applicationRepository).delete(application);
    }
}
