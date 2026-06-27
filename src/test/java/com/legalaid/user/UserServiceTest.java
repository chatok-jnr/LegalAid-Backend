package com.legalaid.user;

import com.legalaid.user.dto.UpdateUserRequest;
import com.legalaid.user.dto.UserPrivateResponse;
import com.legalaid.user.dto.UserPublicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final UUID   USER_ID = UUID.randomUUID();
    private static final String NAME    = "Test User";
    private static final String EMAIL   = "test@legalaid.com";
    private static final String PHONE   = "01700000000";
    private static final String AVATAR  = "https://avatar.url/pic.jpg";

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(USER_ID)
                .name(NAME)
                .email(EMAIL)
                .phone(PHONE)
                .avatarUrl(AVATAR)
                .role(UserRole.CLIENT)
                .build();
    }

    // ── getPublicProfile ─────────────────────────────────────

    @Test
    @DisplayName("Should return public profile with limited fields only")
    void shouldReturnPublicProfileWithLimitedFields() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));

        UserPublicResponse result = userService.getPublicProfile(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getAvatarUrl()).isEqualTo(AVATAR);
        assertThat(result.getRole()).isEqualTo("CLIENT");
    }

    @Test
    @DisplayName("Public profile should throw if user not found")
    void publicProfileShouldThrowIfUserNotFound() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPublicProfile(USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── getMyProfile ─────────────────────────────────────────

    @Test
    @DisplayName("Should return full private profile for own user")
    void shouldReturnFullPrivateProfile() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));

        UserPrivateResponse result = userService.getMyProfile(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getPhone()).isEqualTo(PHONE);
        assertThat(result.getRole()).isEqualTo("CLIENT");
    }

    // ── updateMyProfile ──────────────────────────────────────

    @Test
    @DisplayName("Should update only fields that are provided")
    void shouldUpdateOnlyProvidedFields() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");
        // phone, address, avatarUrl intentionally left null

        UserPrivateResponse result = userService.updateMyProfile(USER_ID, request);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo(PHONE); // unchanged
        verify(userRepository, times(1)).save(activeUser);
    }

    @Test
    @DisplayName("Should update phone when provided")
    void shouldUpdatePhoneWhenProvided() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("01900000000");

        userService.updateMyProfile(USER_ID, request);

        assertThat(activeUser.getPhone()).isEqualTo("01900000000");
    }

    @Test
    @DisplayName("Should not update name if null is passed")
    void shouldNotUpdateNameIfNullPassed() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        UpdateUserRequest request = new UpdateUserRequest();
        // nothing set — all null

        userService.updateMyProfile(USER_ID, request);

        assertThat(activeUser.getName()).isEqualTo(NAME); // unchanged
    }

    // ── deleteMyAccount ──────────────────────────────────────

    @Test
    @DisplayName("Delete should soft delete — set deletedAt, not remove row")
    void deleteShouldSoftDelete() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        userService.deleteMyAccount(USER_ID);

        assertThat(activeUser.getDeletedAt()).isNotNull(); // deletedAt was set
        verify(userRepository, times(1)).save(activeUser); // row was saved not deleted
        verify(userRepository, never()).delete(any());     // hard delete never called
    }

    @Test
    @DisplayName("Delete should throw if user not found")
    void deleteShouldThrowIfUserNotFound() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteMyAccount(USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── requestLawyerRole ────────────────────────────────────

//    @Test
//    @DisplayName("Admin should not be able to request lawyer role")
//    void adminShouldNotRequestLawyerRole() {
//        User adminUser = User.builder()
//                .id(USER_ID).name(NAME).email(EMAIL)
//                .role(UserRole.ADMIN).build();
//
//        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(adminUser));
//
//        assertThatThrownBy(() -> userService.requestLawyerRole(USER_ID))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Admins cannot");
//    }
//
//    @Test
//    @DisplayName("Existing lawyer should not request lawyer role again")
//    void existingLawyerShouldNotRequestLawyerRoleAgain() {
//        User lawyerUser = User.builder()
//                .id(USER_ID).name(NAME).email(EMAIL)
//                .role(UserRole.LAWYER).build();
//
//        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(lawyerUser));
//
//        assertThatThrownBy(() -> userService.requestLawyerRole(USER_ID))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("already a lawyer");
//    }
}