package org.example.web.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentAbility;
import org.example.web.mapper.StudentAbilityMapper;
import org.example.web.service.StudentAbilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.web.tool.RSA_256;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAbilityServiceImpl implements StudentAbilityService {

    private static final Logger logger = LoggerFactory.getLogger(StudentAbilityServiceImpl.class);
    private final StudentAbilityMapper studentAbilityMapper;
    private final RSA_256 rsa256;

    @Override
    public List<StudentAbility> selectAll() {
        try {
            List<StudentAbility> abilities = studentAbilityMapper.selectAll();
            // 解密敏感字段
            decryptSensitiveFields(abilities);
            return abilities;
        } catch (Exception e) {
            logger.error("【能力服务】查询所有能力失败", e);
            throw new RuntimeException("查询所有能力失败", e);
        }
    }

    @Override
    public List<StudentAbility> selectByCondition(StudentAbility ability) {
        try {
            List<StudentAbility> abilities = studentAbilityMapper.selectByCondition(ability);
            // 解密敏感字段
            decryptSensitiveFields(abilities);
            return abilities;
        } catch (Exception e) {
            logger.error("【能力服务】条件查询能力失败，查询条件：{}", ability, e);
            throw new RuntimeException("条件查询能力失败", e);
        }
    }

    /**
     * 解密能力信息中的敏感字段
     */
    private void decryptSensitiveFields(List<StudentAbility> abilities) {
        for (StudentAbility ability : abilities) {
            if (ability.getEducationRequirement() != null) {
                try {
                    ability.setEducationRequirement(rsa256.rsaDecrypt(ability.getEducationRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密学历背景失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getInternshipAbility() != null) {
                try {
                    ability.setInternshipAbility(rsa256.rsaDecrypt(ability.getInternshipAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密实习经历失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getProfessionalSkill() != null) {
                try {
                    ability.setProfessionalSkill(rsa256.rsaDecrypt(ability.getProfessionalSkill()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密专业技能失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getCertificateRequirement() != null) {
                try {
                    ability.setCertificateRequirement(rsa256.rsaDecrypt(ability.getCertificateRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密证书资质失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getInnovationAbility() != null) {
                try {
                    ability.setInnovationAbility(rsa256.rsaDecrypt(ability.getInnovationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密创新能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getLearningAbility() != null) {
                try {
                    ability.setLearningAbility(rsa256.rsaDecrypt(ability.getLearningAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密学习能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getPressureResistance() != null) {
                try {
                    ability.setPressureResistance(rsa256.rsaDecrypt(ability.getPressureResistance()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密抗压能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getCommunicationAbility() != null) {
                try {
                    ability.setCommunicationAbility(rsa256.rsaDecrypt(ability.getCommunicationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密沟通能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getProblemSolving() != null) {
                try {
                    ability.setProblemSolving(rsa256.rsaDecrypt(ability.getProblemSolving()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密问题解决能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (ability.getTeamworkAbility() != null) {
                try {
                    ability.setTeamworkAbility(rsa256.rsaDecrypt(ability.getTeamworkAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】解密团队协作能力失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentAbility> insert(StudentAbility ability) {
        try {
            // 使用雪花算法生成分布式ID
            ability.setId(org.example.web.tool.SnowIdCreater.generateId(5)); // 类别5=student_ability

            // 使用RSA加密所有敏感字段
            if (ability.getEducationRequirement() != null) {
                try {
                    ability.setEducationRequirement(rsa256.rsaEncrypt(ability.getEducationRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密学历背景失败", e);
                    throw new RuntimeException("加密学历背景失败", e);
                }
            }
            if (ability.getInternshipAbility() != null) {
                try {
                    ability.setInternshipAbility(rsa256.rsaEncrypt(ability.getInternshipAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密实习经历失败", e);
                    throw new RuntimeException("加密实习经历失败", e);
                }
            }
            if (ability.getProfessionalSkill() != null) {
                try {
                    ability.setProfessionalSkill(rsa256.rsaEncrypt(ability.getProfessionalSkill()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密专业技能失败", e);
                    throw new RuntimeException("加密专业技能失败", e);
                }
            }
            if (ability.getCertificateRequirement() != null) {
                try {
                    ability.setCertificateRequirement(rsa256.rsaEncrypt(ability.getCertificateRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密证书资质失败", e);
                    throw new RuntimeException("加密证书资质失败", e);
                }
            }
            if (ability.getInnovationAbility() != null) {
                try {
                    ability.setInnovationAbility(rsa256.rsaEncrypt(ability.getInnovationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密创新能力失败", e);
                    throw new RuntimeException("加密创新能力失败", e);
                }
            }
            if (ability.getLearningAbility() != null) {
                try {
                    ability.setLearningAbility(rsa256.rsaEncrypt(ability.getLearningAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密学习能力失败", e);
                    throw new RuntimeException("加密学习能力失败", e);
                }
            }
            if (ability.getPressureResistance() != null) {
                try {
                    ability.setPressureResistance(rsa256.rsaEncrypt(ability.getPressureResistance()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密抗压能力失败", e);
                    throw new RuntimeException("加密抗压能力失败", e);
                }
            }
            if (ability.getCommunicationAbility() != null) {
                try {
                    ability.setCommunicationAbility(rsa256.rsaEncrypt(ability.getCommunicationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密沟通能力失败", e);
                    throw new RuntimeException("加密沟通能力失败", e);
                }
            }
            if (ability.getProblemSolving() != null) {
                try {
                    ability.setProblemSolving(rsa256.rsaEncrypt(ability.getProblemSolving()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密问题解决能力失败", e);
                    throw new RuntimeException("加密问题解决能力失败", e);
                }
            }
            if (ability.getTeamworkAbility() != null) {
                try {
                    ability.setTeamworkAbility(rsa256.rsaEncrypt(ability.getTeamworkAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密团队协作能力失败", e);
                    throw new RuntimeException("加密团队协作能力失败", e);
                }
            }

            return studentAbilityMapper.insert(ability);
        } catch (Exception e) {
            logger.error("【能力服务】新增能力失败，能力数据：{}", ability, e);
            throw new RuntimeException("新增能力失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentAbility> update(StudentAbility ability) {
        try {
            if (ability.getId() == null || ability.getId() <= 0) {
                throw new IllegalArgumentException("能力ID不能为空且必须大于0");
            }

            // 敏感字段更新时重新加密
            if (ability.getEducationRequirement() != null) {
                try {
                    ability.setEducationRequirement(rsa256.rsaEncrypt(ability.getEducationRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密学历背景失败", e);
                    throw new RuntimeException("加密学历背景失败", e);
                }
            }
            if (ability.getInternshipAbility() != null) {
                try {
                    ability.setInternshipAbility(rsa256.rsaEncrypt(ability.getInternshipAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密实习经历失败", e);
                    throw new RuntimeException("加密实习经历失败", e);
                }
            }
            if (ability.getProfessionalSkill() != null) {
                try {
                    ability.setProfessionalSkill(rsa256.rsaEncrypt(ability.getProfessionalSkill()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密专业技能失败", e);
                    throw new RuntimeException("加密专业技能失败", e);
                }
            }
            if (ability.getCertificateRequirement() != null) {
                try {
                    ability.setCertificateRequirement(rsa256.rsaEncrypt(ability.getCertificateRequirement()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密证书资质失败", e);
                    throw new RuntimeException("加密证书资质失败", e);
                }
            }
            if (ability.getInnovationAbility() != null) {
                try {
                    ability.setInnovationAbility(rsa256.rsaEncrypt(ability.getInnovationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密创新能力失败", e);
                    throw new RuntimeException("加密创新能力失败", e);
                }
            }
            if (ability.getLearningAbility() != null) {
                try {
                    ability.setLearningAbility(rsa256.rsaEncrypt(ability.getLearningAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密学习能力失败", e);
                    throw new RuntimeException("加密学习能力失败", e);
                }
            }
            if (ability.getPressureResistance() != null) {
                try {
                    ability.setPressureResistance(rsa256.rsaEncrypt(ability.getPressureResistance()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密抗压能力失败", e);
                    throw new RuntimeException("加密抗压能力失败", e);
                }
            }
            if (ability.getCommunicationAbility() != null) {
                try {
                    ability.setCommunicationAbility(rsa256.rsaEncrypt(ability.getCommunicationAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密沟通能力失败", e);
                    throw new RuntimeException("加密沟通能力失败", e);
                }
            }
            if (ability.getProblemSolving() != null) {
                try {
                    ability.setProblemSolving(rsa256.rsaEncrypt(ability.getProblemSolving()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密问题解决能力失败", e);
                    throw new RuntimeException("加密问题解决能力失败", e);
                }
            }
            if (ability.getTeamworkAbility() != null) {
                try {
                    ability.setTeamworkAbility(rsa256.rsaEncrypt(ability.getTeamworkAbility()));
                } catch (Exception e) {
                    logger.error("【能力服务】加密团队协作能力失败", e);
                    throw new RuntimeException("加密团队协作能力失败", e);
                }
            }

            // 查询原数据
            List<StudentAbility> originalAbilities = studentAbilityMapper.selectById(ability.getId());
            if (!originalAbilities.isEmpty()) {
                StudentAbility original = originalAbilities.get(0);
                // 比较主要字段是否相同
                if (isAbilityEqual(original, ability)) {
                    throw new RuntimeException("更新后与原先相同");
                }
            }

            return studentAbilityMapper.update(ability);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                throw e;
            }
            if (e instanceof IllegalArgumentException) {
                logger.warn("【能力服务】更新能力参数错误：{}", e.getMessage());
                throw e;
            }
            logger.error("【能力服务】更新能力失败，能力数据：{}", ability, e);
            throw new RuntimeException("更新能力失败", e);
        } catch (Exception e) {
            logger.error("【能力服务】更新能力失败，能力数据：{}", ability, e);
            throw new RuntimeException("更新能力失败", e);
        }
    }

    // 比较两个能力信息是否相同
    private boolean isAbilityEqual(StudentAbility original, StudentAbility updated) {
        return (updated.getUserId() == null || updated.getUserId().equals(original.getUserId())) &&
               (updated.getProfileId() == null || updated.getProfileId().equals(original.getProfileId())) &&
               (updated.getEducationRequirement() == null || updated.getEducationRequirement().equals(original.getEducationRequirement())) &&
               (updated.getInternshipAbility() == null || updated.getInternshipAbility().equals(original.getInternshipAbility())) &&
               (updated.getProfessionalSkill() == null || updated.getProfessionalSkill().equals(original.getProfessionalSkill())) &&
               (updated.getCertificateRequirement() == null || updated.getCertificateRequirement().equals(original.getCertificateRequirement())) &&
               (updated.getInnovationAbility() == null || updated.getInnovationAbility().equals(original.getInnovationAbility())) &&
               (updated.getLearningAbility() == null || updated.getLearningAbility().equals(original.getLearningAbility())) &&
               (updated.getPressureResistance() == null || updated.getPressureResistance().equals(original.getPressureResistance())) &&
               (updated.getCommunicationAbility() == null || updated.getCommunicationAbility().equals(original.getCommunicationAbility())) &&
               (updated.getProblemSolving() == null || updated.getProblemSolving().equals(original.getProblemSolving())) &&
               (updated.getTeamworkAbility() == null || updated.getTeamworkAbility().equals(original.getTeamworkAbility()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentAbility> deleteById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("能力ID不能为空且必须大于0");
            }
            return studentAbilityMapper.deleteById(id);
        } catch (IllegalArgumentException e) {
            logger.warn("【能力服务】删除能力参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【能力服务】删除能力失败，能力ID：{}", id, e);
            throw new RuntimeException("删除能力失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentAbility> batchDelete(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量删除的ID列表不能为空");
            }
            return studentAbilityMapper.batchDelete(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【能力服务】批量删除参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【能力服务】批量删除能力失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量删除能力失败", e);
        }
    }

    @Override
    public List<StudentAbility> batchSelect(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量查询的ID列表不能为空");
            }
            return studentAbilityMapper.batchSelect(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【能力服务】批量查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【能力服务】批量查询能力失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量查询能力失败", e);
        }
    }

    @Override
    public List<StudentAbility> selectByUserId(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("用户ID不能为空且必须大于0");
            }
            List<StudentAbility> abilities = studentAbilityMapper.selectByUserId(userId);
            // 解密敏感字段
            decryptSensitiveFields(abilities);
            return abilities;
        } catch (IllegalArgumentException e) {
            logger.warn("【能力服务】根据用户ID查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【能力服务】根据用户ID查询能力失败，用户ID：{}", userId, e);
            throw new RuntimeException("根据用户ID查询能力失败", e);
        }
    }

    @Override
    public List<StudentAbility> selectById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("能力ID不能为空且必须大于0");
            }
            List<StudentAbility> abilities = studentAbilityMapper.selectById(id);
            // 解密敏感字段
            decryptSensitiveFields(abilities);
            return abilities;
        } catch (IllegalArgumentException e) {
            logger.warn("【能力服务】根据ID查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【能力服务】根据ID查询能力失败，能力ID：{}", id, e);
            throw new RuntimeException("根据ID查询能力失败", e);
        }
    }

//    @Override
//    public List<StudentAbility> batchSelect(List<Long> ids) {
//        try {
//            if (ids == null || ids.isEmpty()) {
//                throw new IllegalArgumentException("批量查询的ID列表不能为空");
//            }
//            List<StudentAbility> abilities = studentAbilityMapper.batchSelect(ids);
//            // 解密敏感字段
//            decryptSensitiveFields(abilities);
//            return abilities;
//        } catch (IllegalArgumentException e) {
//            logger.warn("【能力服务】批量查询参数错误：{}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            logger.error("【能力服务】批量查询能力失败，ID列表：{}", ids, e);
//            throw new RuntimeException("批量查询能力失败", e);
//        }
//    }
}