package com.slotify.module.user.mapper;

import com.slotify.module.user.dto.UserResponse;
import com.slotify.module.user.entity.User;
import org.mapstruct.Mapper;

/** MapStruct mapper between {@link User} and its DTOs. */
@Mapper
public interface UserMapper {

  UserResponse toResponse(User user);
}
