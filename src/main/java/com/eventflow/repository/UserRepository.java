package com.eventflow.repository;

import com.eventflow.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    long countByRole(String role);
    List<User> findByRole(String role);
    List<User> findAllByOrderByFullNameAsc();
    List<User> findTop5ByOrderByFullNameAsc();
}
