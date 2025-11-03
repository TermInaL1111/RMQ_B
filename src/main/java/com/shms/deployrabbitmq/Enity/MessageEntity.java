package com.shms.deployrabbitmq.Enity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "messages")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageId;

    private String sender;
    private String receiver;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private String content;
    private String fileUrl;
    private Long timestamp;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    public enum MessageType { text, file, status }
    public enum Status { PENDING, SENT, DELIVERED, READ }

}
