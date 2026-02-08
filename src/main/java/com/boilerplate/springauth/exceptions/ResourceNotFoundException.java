package com.boilerplate.springauth.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException (String message) {
        super(message);
    }
    
}
