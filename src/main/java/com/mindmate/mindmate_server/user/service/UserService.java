package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingRequest;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User findUserById(Long userId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    List<User> findByCurrentRoleAndSuspensionEndTimeBefore(RoleType roleType, LocalDateTime time);

    List<Long> findAllUserIds();

    Optional<User> findByEmailOptional(String email);

    PushNotificationSettingResponse getPushNotificationSetting(Long userId);

    PushNotificationSettingResponse updatePushNotificationSetting(Long userId, PushNotificationSettingRequest request);

    boolean isPushNotificationEnabled(Long userId);
}
