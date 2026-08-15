package org.example.web.service.impl;

import java.util.List;

import org.example.web.entity.StudentAbilityScore;
import org.example.web.mapper.StudentAbilityScoreMapper;
import org.example.web.service.StudentAbilityScoreService;
import org.example.web.tool.RSA_256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentAbilityScoreServiceImpl implements StudentAbilityScoreService {

    private static final Logger logger = LoggerFactory.getLogger(StudentAbilityScoreServiceImpl.class);
    private final StudentAbilityScoreMapper studentAbilityScoreMapper;
    private final RSA_256 rsa256;

    @Override
    public List<StudentAbilityScore> selectAll() {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreMapper.selectAll();
            // 解密敏感字段
            decryptSensitiveFields(scores);
            return scores;
        } catch (Exception e) {
            logger.error("【评分服务】查询所有评分失败", e);
            throw new RuntimeException("查询所有评分失败", e);
        }
    }

    @Override
    public List<StudentAbilityScore> selectByCondition(StudentAbilityScore score) {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreMapper.selectByCondition(score);
            // 解密敏感字段
            decryptSensitiveFields(scores);
            return scores;
        } catch (Exception e) {
            logger.error("【评分服务】条件查询评分失败，查询条件：{}", score, e);
            throw new RuntimeException("条件查询评分失败", e);
        }
    }

    /**
     * 解密评分信息中的敏感字段
     */
    private void decryptSensitiveFields(List<StudentAbilityScore> scores) {
        for (StudentAbilityScore score : scores) {
            if (score.getScoreComment() != null) {
                try {
                    score.setScoreComment(rsa256.rsaDecrypt(score.getScoreComment()));
                } catch (Exception e) {
                    logger.error("【评分服务】解密评分评语失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(StudentAbilityScore score) {
        try {
            // 使用雪花算法生成分布式ID
            score.setId(org.example.web.tool.SnowIdCreater.generateId(6)); // 类别6=student_ability_score

            // 使用RSA加密敏感字段
            if (score.getScoreComment() != null) {
                try {
                    score.setScoreComment(rsa256.rsaEncrypt(score.getScoreComment()));
                } catch (Exception e) {
                    logger.error("【评分服务】加密评分评语失败", e);
                    throw new RuntimeException("加密评分评语失败", e);
                }
            }

            return studentAbilityScoreMapper.insert(score);
        } catch (Exception e) {
            logger.error("【评分服务】新增评分失败，评分数据：{}", score, e);
            throw new RuntimeException("新增评分失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(StudentAbilityScore score) {
        try {
            if (score.getId() == null || score.getId() <= 0) {
                throw new IllegalArgumentException("评分ID不能为空且必须大于0");
            }

            // 敏感字段更新时重新加密
            if (score.getScoreComment() != null) {
                try {
                    score.setScoreComment(rsa256.rsaEncrypt(score.getScoreComment()));
                } catch (Exception e) {
                    logger.error("【评分服务】加密评分评语失败", e);
                    throw new RuntimeException("加密评分评语失败", e);
                }
            }

            // 查询原数据
            List<StudentAbilityScore> originalScores = studentAbilityScoreMapper.selectById(score.getId());
            if (!originalScores.isEmpty()) {
                StudentAbilityScore original = originalScores.get(0);
                // 比较主要字段是否相同
                if (isScoreEqual(original, score)) {
                    throw new RuntimeException("更新后与原先相同");
                }
            }

            return studentAbilityScoreMapper.update(score);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                throw e;
            }
            if (e instanceof IllegalArgumentException) {
                logger.warn("【评分服务】更新评分参数错误：{}", e.getMessage());
                throw e;
            }
            logger.error("【评分服务】更新评分失败，评分数据：{}", score, e);
            throw new RuntimeException("更新评分失败", e);
        } catch (Exception e) {
            logger.error("【评分服务】更新评分失败，评分数据：{}", score, e);
            throw new RuntimeException("更新评分失败", e);
        }
    }

    // 比较两个评分信息是否相同
    private boolean isScoreEqual(StudentAbilityScore original, StudentAbilityScore updated) {
        return (updated.getUserId() == null || updated.getUserId().equals(original.getUserId())) &&
               (updated.getAbilityId() == null || updated.getAbilityId().equals(original.getAbilityId())) &&
               (updated.getEducationScore() == null || updated.getEducationScore().equals(original.getEducationScore())) &&
               (updated.getInternshipScore() == null || updated.getInternshipScore().equals(original.getInternshipScore())) &&
               (updated.getProfessionalScore() == null || updated.getProfessionalScore().equals(original.getProfessionalScore())) &&
               (updated.getCertificateScore() == null || updated.getCertificateScore().equals(original.getCertificateScore())) &&
               (updated.getInnovationScore() == null || updated.getInnovationScore().equals(original.getInnovationScore())) &&
               (updated.getLearningScore() == null || updated.getLearningScore().equals(original.getLearningScore())) &&
               (updated.getPressureScore() == null || updated.getPressureScore().equals(original.getPressureScore())) &&
               (updated.getCommunicationScore() == null || updated.getCommunicationScore().equals(original.getCommunicationScore())) &&
               (updated.getProblemSolvingScore() == null || updated.getProblemSolvingScore().equals(original.getProblemSolvingScore())) &&
               (updated.getTeamworkScore() == null || updated.getTeamworkScore().equals(original.getTeamworkScore())) &&
               (updated.getTotalScore() == null || updated.getTotalScore().equals(original.getTotalScore())) &&
               (updated.getIndustryRank() == null || updated.getIndustryRank().equals(original.getIndustryRank())) &&
               (updated.getPeerRank() == null || updated.getPeerRank().equals(original.getPeerRank())) &&
               (updated.getScoreType() == null || updated.getScoreType().equals(original.getScoreType())) &&
               (updated.getScoreComment() == null || updated.getScoreComment().equals(original.getScoreComment()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("评分ID不能为空且必须大于0");
            }
            return studentAbilityScoreMapper.deleteById(id);
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】删除评分参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】删除评分失败，评分ID：{}", id, e);
            throw new RuntimeException("删除评分失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量删除的ID列表不能为空");
            }
            return studentAbilityScoreMapper.batchDelete(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】批量删除参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】批量删除评分失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量删除评分失败", e);
        }
    }

    @Override
    public List<StudentAbilityScore> batchSelect(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量查询的ID列表不能为空");
            }
            return studentAbilityScoreMapper.batchSelect(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】批量查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】批量查询评分失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量查询评分失败", e);
        }
    }

    @Override
    public List<StudentAbilityScore> selectByUserId(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("用户ID不能为空且必须大于0");
            }
            List<StudentAbilityScore> scores = studentAbilityScoreMapper.selectByUserId(userId);
            // 解密敏感字段
            decryptSensitiveFields(scores);
            return scores;
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】根据用户ID查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】根据用户ID查询评分失败，用户ID：{}", userId, e);
            throw new RuntimeException("根据用户ID查询评分失败", e);
        }
    }

    @Override
    public List<StudentAbilityScore> selectByAbilityId(Long abilityId) {
        try {
            if (abilityId == null || abilityId <= 0) {
                throw new IllegalArgumentException("能力ID不能为空且必须大于0");
            }
            List<StudentAbilityScore> scores = studentAbilityScoreMapper.selectByAbilityId(abilityId);
            // 解密敏感字段
            decryptSensitiveFields(scores);
            return scores;
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】根据能力ID查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】根据能力ID查询评分失败，能力ID：{}", abilityId, e);
            throw new RuntimeException("根据能力ID查询评分失败", e);
        }
    }

    @Override
    public List<StudentAbilityScore> selectById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("评分ID不能为空且必须大于0");
            }
            List<StudentAbilityScore> scores = studentAbilityScoreMapper.selectById(id);
            // 解密敏感字段
            decryptSensitiveFields(scores);
            return scores;
        } catch (IllegalArgumentException e) {
            logger.warn("【评分服务】根据ID查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【评分服务】根据ID查询评分失败，评分ID：{}", id, e);
            throw new RuntimeException("根据ID查询评分失败", e);
        }
    }
}
