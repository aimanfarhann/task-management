package com.taskflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies that all four V1–V4 Flyway migrations apply cleanly against real PostgreSQL and produce
 * the schema SCHEMA.md specifies — including the M2 task tables that ship in schema v1.
 */
class FlywayMigrationTest extends IntegrationTestBase {

  @Test
  void migrations_applyCleanly_creatingAllSixTables() {
    List<String> tables =
        jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);

    assertThat(tables)
        .contains(
            "users", "projects", "project_members", "tasks", "task_comments", "refresh_tokens");
  }

  @Test
  void flywayHistory_containsFourSuccessfulVersions() {
    List<Map<String, Object>> history =
        jdbcTemplate.queryForList(
            "select version, success from flyway_schema_history where version is not null"
                + " order by installed_rank");

    assertThat(history).hasSize(4);
    assertThat(history).extracting(row -> row.get("version")).containsExactly("1", "2", "3", "4");
    assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
  }

  @Test
  void schema_containsAllSpecifiedIndexes() {
    List<String> indexes =
        jdbcTemplate.queryForList(
            "select indexname from pg_indexes where schemaname = 'public'", String.class);

    assertThat(indexes)
        .contains(
            "idx_users_email_lower",
            "idx_members_user",
            "idx_tasks_project_status",
            "idx_tasks_assignee",
            "idx_tasks_due_date",
            "idx_comments_task",
            "idx_refresh_user");
  }
}
