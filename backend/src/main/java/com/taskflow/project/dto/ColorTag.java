package com.taskflow.project.dto;

/**
 * The fixed palette of eight project color tag keys (DESIGN.md §2 — no free color picker). Shared
 * by the create and update request validations.
 */
public final class ColorTag {

  /** Validation regex accepting exactly the eight preset keys. */
  public static final String PATTERN = "red|orange|amber|green|teal|blue|indigo|purple";

  /** Validation message listing the allowed keys. */
  public static final String MESSAGE =
      "must be one of: red, orange, amber, green, teal, blue, indigo, purple";

  private ColorTag() {}
}
