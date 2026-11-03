package com.spendmart.bank_auth_service.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
@Profile("local")
public class LocalTokenStore extends TokenStore {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    public LocalTokenStore() {
        super(null);
    }

    @Override
    public void put(String userId, String provider, String token) {
        store.put(userId + ":" + provider, token);
    }

    @Override
    public Optional<String> get(String userId, String provider) {
        return Optional.ofNullable(store.get(userId + ":" + provider));
    }
}
