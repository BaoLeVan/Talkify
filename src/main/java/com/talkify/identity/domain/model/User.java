package com.talkify.identity.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class User {
    private UserId id;
    private String name;
    private String displayName;
    private Email email;
    private String phoneNumber;
    private Password password;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private final List<Object> domainEvents = new ArrayList<>();
    private final List<Device> devices = new ArrayList<>();

    public static User register(UserId id, Username username, Email email, Password password, String displayName) {
        User user = new User();
        user.id = id;
        user.name = username.value();
        user.displayName = displayName;
        user.email = email;
        user.password = password;
        user.role = UserRole.USER;
        user.status = UserStatus.INACTIVE;
        user.createdAt = Instant.now();
        return user;
    }

    public static User reconstruct(
            UserId id,
            String name,
            String displayName,
            Email email,
            String phoneNumber,
            Password password,
            UserRole role,
            UserStatus status,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        User user = new User();
        user.id = id;
        user.name = name;
        user.displayName = displayName;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.password = password;
        user.role = role;
        user.status = status;
        user.deletedAt = deletedAt;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    public void registerDevice(Device device) {
        boolean exists = devices.stream().anyMatch(d -> d.equals(device));
        if (!exists) {
            devices.add(device);
        }
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
}