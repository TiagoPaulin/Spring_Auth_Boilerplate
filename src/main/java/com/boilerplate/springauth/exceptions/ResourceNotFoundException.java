package com.boilerplate.springauth.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException (Object obj, Object id) {
        super(obj + " não encontrado. ID: " + id);
    }
    
}
