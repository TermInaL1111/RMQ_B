package com.shms.deployrabbitmq.Repository;

import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Enity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    List<UserEntity> findByStatus(UserEntity.Status status);
}