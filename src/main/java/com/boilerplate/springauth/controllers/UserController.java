package com.boilerplate.springauth.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserResponse;
import com.boilerplate.springauth.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    private final UserService service;

    public UserController (UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create (@Valid @RequestBody UserCreateRequest request) {

        UserResponse obj = service.create(request);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(obj.id()).toUri();
        
        return ResponseEntity.created(uri).body(obj);

    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserResponse> find (@PathVariable Long id) {

        UserResponse user = service.findUser(id);

        return ResponseEntity.ok().body(user);

    }
   
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete (@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();

    }

}
