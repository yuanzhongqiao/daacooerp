package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByTel(String tel);
    
    boolean existsByUsername(String username);
    
    boolean existsByTel(String tel);
}