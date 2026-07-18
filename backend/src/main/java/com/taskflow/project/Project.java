package com.taskflow.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity mirroring the {@code projects} table (SCHEMA.md §2.2). */
@Entity
@Table(name = "projects")
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "color_tag", nullable = false, length = 20)
  private String colorTag;

  @Column(nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA-only constructor. */
  protected Project() {}

  /**
   * Creates a new unarchived project.
   *
   * @param name the project name
   * @param description optional free-text description, may be null
   * @param colorTag one of the eight preset color tag keys
   */
  public Project(String name, String description, String colorTag) {
    this.name = name;
    this.description = description;
    this.colorTag = colorTag;
    this.archived = false;
    this.createdAt = Instant.now();
  }

  /**
   * Applies a full update from {@code PUT /projects/{id}} — all fields are replaced.
   *
   * @param name the new project name
   * @param description the new description, may be null
   * @param colorTag the new color tag key
   * @param archived the new archived state
   */
  public void updateDetails(String name, String description, String colorTag, boolean archived) {
    this.name = name;
    this.description = description;
    this.colorTag = colorTag;
    this.archived = archived;
  }

  /** Returns the database id, or null if not yet persisted. */
  public Long getId() {
    return id;
  }

  /** Returns the project name. */
  public String getName() {
    return name;
  }

  /** Returns the description, or null if none was provided. */
  public String getDescription() {
    return description;
  }

  /** Returns the preset color tag key. */
  public String getColorTag() {
    return colorTag;
  }

  /** Returns whether the project is archived. */
  public boolean isArchived() {
    return archived;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
