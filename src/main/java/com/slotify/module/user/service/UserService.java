package com.slotify.module.user.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.user.dto.ChangePasswordRequest;
import com.slotify.module.user.dto.DeviceTokenRequest;
import com.slotify.module.user.dto.UpdateProfileRequest;
import com.slotify.module.user.dto.UserResponse;
import com.slotify.module.user.entity.DeviceToken;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.mapper.UserMapper;
import com.slotify.module.user.repository.DeviceTokenRepository;
import com.slotify.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile management for the signed-in user ({@code /me} endpoints). */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final DeviceTokenRepository deviceTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  /** Loads a user or throws {@link ErrorCode#USER_NOT_FOUND}. */
  @Transactional(readOnly = true)
  public User getById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile(Long userId) {
    return userMapper.toResponse(getById(userId));
  }

  public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
    User user = getById(userId);
    user.setFullName(request.fullName().trim());
    user.setPhone(request.phone());
    user.setAvatarUrl(request.avatarUrl());
    if (request.locale() != null) {
      user.setLocale(request.locale());
    }
    return userMapper.toResponse(user);
  }

  /** Changes the password of a local account after checking the current one. */
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = getById(userId);
    if (!user.hasPassword()
        || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
  }

  /** Registers (or re-assigns) an FCM token for push notifications. */
  public void registerDeviceToken(Long userId, DeviceTokenRequest request) {
    User user = getById(userId);
    DeviceToken token =
        deviceTokenRepository
            .findByFcmToken(request.token())
            .orElseGet(
                () -> DeviceToken.of(user, request.token(), request.platform(), request.appType()));
    // A token can move to another account when a different user signs in on the same device.
    token.setUser(user);
    token.setPlatform(request.platform());
    token.setAppType(request.appType());
    deviceTokenRepository.save(token);
  }

  /** Removes an FCM token, e.g. on sign-out. */
  public void removeDeviceToken(String fcmToken) {
    deviceTokenRepository.deleteByFcmToken(fcmToken);
  }
}
