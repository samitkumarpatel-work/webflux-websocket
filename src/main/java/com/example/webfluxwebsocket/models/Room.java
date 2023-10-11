package com.example.webfluxwebsocket.models;

import java.util.List;
import java.util.UUID;

public record Room(UUID id, String name, List<User> users) {}
