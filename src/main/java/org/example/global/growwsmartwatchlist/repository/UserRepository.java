package org.example.global.growwsmartwatchlist.repository;

import org.example.global.growwsmartwatchlist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
