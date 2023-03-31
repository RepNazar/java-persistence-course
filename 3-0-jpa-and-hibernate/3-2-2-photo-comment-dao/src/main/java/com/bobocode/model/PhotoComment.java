package com.bobocode.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * todo:
 * - implement equals and hashCode based on identifier field
 *
 * - configure JPA entity
 * - specify table name: "photo_comment"
 * - configure auto generated identifier
 * - configure not nullable column: text
 *
 * - map relation between Photo and PhotoComment using foreign_key column: "photo_id"
 * - configure relation as mandatory (not optional)
 */
@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"id"})
@Table(name = "photo_comment")
public class PhotoComment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String text;

    private LocalDateTime createdOn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "photo_id")
    private Photo photo;
}
