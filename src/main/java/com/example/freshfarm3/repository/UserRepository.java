package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Optional: for login with active check
    Optional<User> findByEmailAndActiveTrue(String email);
}