package com.neuroguard.forumsservice.config;


import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
