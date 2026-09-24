package org.example.web.security;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Bind platform jobs to their local owner durably before returning their identifiers. */
@Service
public class ReportJobRegistry {
    private final JdbcTemplate db;
    public ReportJobRegistry(JdbcTemplate db) { this.db = db; }
    public void register(String job, long user) {
        valid(job);
        db.update("INSERT INTO career_report_job(job_id,user_id) VALUES(?,?)", job, user);
    }
    public void owned(String job, long user) {
        valid(job);
        Integer count = db.queryForObject("SELECT COUNT(*) FROM career_report_job WHERE job_id=? AND user_id=?", Integer.class, job, user);
        if (count == null || count != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
    }
    private void valid(String job) {
        if (job == null || !job.matches("[A-Za-z0-9_-]{1,128}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务 ID 格式错误");
    }
}
