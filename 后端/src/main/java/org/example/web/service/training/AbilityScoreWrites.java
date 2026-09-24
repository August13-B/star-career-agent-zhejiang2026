package org.example.web.service.training;

import java.util.List;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.service.StudentAbilityScoreService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serializes quiz/AI replacement with training; remote inference stays outside this transaction. */
@Service
public class AbilityScoreWrites {
    private final JdbcTemplate jdbc;
    private final StudentAbilityScoreService scores;
    public AbilityScoreWrites(JdbcTemplate jdbc, StudentAbilityScoreService scores) { this.jdbc=jdbc; this.scores=scores; }
    public void lock(Long user) { jdbc.queryForObject("SELECT id FROM user WHERE id=? FOR UPDATE",Long.class,user); }
    @Transactional
    public int replace(StudentAbilityScore score) {
        lock(score.getUserId());
        List<Long> ids=jdbc.queryForList("SELECT id FROM student_ability_score WHERE user_id=? AND score_type=1 AND is_deleted=0 FOR UPDATE",Long.class,score.getUserId());
        for(Long id:ids) scores.deleteById(id);
        return scores.insert(score);
    }
}
