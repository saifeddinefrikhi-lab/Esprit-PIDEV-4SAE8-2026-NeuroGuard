package com.neuroguard.forumsservice.feign;


import com.neuroguard.forumsservice.dto.UserDto;
import com.neuroguard.forumsservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class,
        fallbackFactory = UserServiceClientFallbackFactory.class
)
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);
}
