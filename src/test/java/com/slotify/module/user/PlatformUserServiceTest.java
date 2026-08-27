package com.slotify.module.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.auth.service.RefreshTokenService;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.entity.UserStatus;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.module.user.service.PlatformUserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The "keep at least one super admin" rule and session revocation on suspension. */
@ExtendWith(MockitoExtension.class)
class PlatformUserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenService refreshTokenService;
  @InjectMocks private PlatformUserService service;

  @Test
  void demotingTheLastSuperAdminIsRefused() {
    User admin = User.local("root@example.com", "hash", "Root", Role.SUPER_ADMIN);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(1L);

    assertThatThrownBy(() -> service.updateRole(1L, Role.SALON_OWNER))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(ErrorCode.LAST_SUPER_ADMIN);
    assertThat(admin.getRole()).isEqualTo(Role.SUPER_ADMIN);
  }

  @Test
  void demotingASuperAdminSucceedsWhenAnotherExists() {
    User admin = User.local("second@example.com", "hash", "Second", Role.SUPER_ADMIN);
    when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(2L);

    assertThat(service.updateRole(2L, Role.CUSTOMER).role()).isEqualTo(Role.CUSTOMER);
  }

  @Test
  void suspendingRevokesSessionsAndReactivatingDoesNot() {
    User customer = User.local("c@example.com", "hash", "Cus", Role.CUSTOMER);
    when(userRepository.findById(3L)).thenReturn(Optional.of(customer));

    assertThat(service.updateStatus(3L, UserStatus.SUSPENDED).status())
        .isEqualTo(UserStatus.SUSPENDED);
    verify(refreshTokenService).revokeAll(customer.getId());

    assertThat(service.updateStatus(3L, UserStatus.ACTIVE).status()).isEqualTo(UserStatus.ACTIVE);
    verify(refreshTokenService, never()).revokeAll(anyLong());
  }

  @Test
  void deletedStatusIsReservedForTheGdprFlow() {
    assertThatThrownBy(() -> service.updateStatus(4L, UserStatus.DELETED))
        .isInstanceOf(AppException.class);
  }
}
