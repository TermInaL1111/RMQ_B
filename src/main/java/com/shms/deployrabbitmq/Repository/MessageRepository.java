package com.shms.deployrabbitmq.Repository;

import com.shms.deployrabbitmq.Enity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Optional<MessageEntity> findByMessageId(String messageId);
    List<MessageEntity> findByReceiverAndStatus(String receiver, MessageEntity.Status status);

}