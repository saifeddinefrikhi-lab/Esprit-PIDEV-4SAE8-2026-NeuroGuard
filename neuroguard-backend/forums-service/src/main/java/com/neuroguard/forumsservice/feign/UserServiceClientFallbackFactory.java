package com.neuroguard.forumsservice.feign;

import com.neuroguard.forumsservice.dto.UserDto;
import org.springframework.stereotype.Component;
import org.springframework.cloud.openfeign.FallbackFactory;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public UserDto getUserById(Long id) {
                UserDto userDto = new UserDto();
                userDto.setId(id);
                userDto.setUsername("Unknown");
                userDto.setRole("UNKNOWN");
                return userDto;
            }

            @Override
            public UserDto getUserByUsername(String username) {
                UserDto userDto = new UserDto();
                userDto.setUsername(username);
                userDto.setRole("UNKNOWN");
                return userDto;
            }
        };
    }
}
