package org.example.web.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.example.web.entity.StudentProfile;
import org.example.web.mapper.StudentProfileMapper;
import org.example.web.service.StudentProfileService;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SnowIdCreater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

   private static final Logger logger = LoggerFactory.getLogger(StudentProfileServiceImpl.class);
   private final StudentProfileMapper studentProfileMapper;
    private final RSA_256 rsa256;


    @Transactional(rollbackFor = Exception.class)
    public List<StudentProfile> encryptAndSave(StudentProfile profile) {
        // 对敏感字符串字段逐一进行 RSA 加密
        if (profile.getUserName() != null) profile.setUserName(rsa256.rsaEncrypt(profile.getUserName()));
        if (profile.getPhone() != null) profile.setPhone(rsa256.rsaEncrypt(profile.getPhone()));
        if (profile.getEmail() != null) profile.setEmail(rsa256.rsaEncrypt(profile.getEmail()));
        if (profile.getCollege() != null) profile.setCollege(rsa256.rsaEncrypt(profile.getCollege()));
        if (profile.getMajor() != null) profile.setMajor(rsa256.rsaEncrypt(profile.getMajor()));
        if (profile.getGrade() != null) profile.setGrade(rsa256.rsaEncrypt(profile.getGrade()));
        if (profile.getCareerIntentions() != null)
            profile.setCareerIntentions(rsa256.rsaEncrypt(profile.getCareerIntentions()));
        if (profile.getJobIntentionDetail() != null)
            profile.setJobIntentionDetail(rsa256.rsaEncrypt(profile.getJobIntentionDetail()));
        if (profile.getTargetCity() != null) profile.setTargetCity(rsa256.rsaEncrypt(profile.getTargetCity()));
        if (profile.getExpectedSalary() != null)
            profile.setExpectedSalary(rsa256.rsaEncrypt(profile.getExpectedSalary()));
        if (profile.getIndustryPreference() != null)
            profile.setIndustryPreference(rsa256.rsaEncrypt(profile.getIndustryPreference()));
        if (profile.getEducation() != null) profile.setEducation(rsa256.rsaEncrypt(profile.getEducation()));
        if (profile.getWorkExperience() != null)
            profile.setWorkExperience(rsa256.rsaEncrypt(profile.getWorkExperience()));
        if (profile.getProjectExperience() != null)
            profile.setProjectExperience(rsa256.rsaEncrypt(profile.getProjectExperience()));
        if (profile.getSkill() != null) profile.setSkill(rsa256.rsaEncrypt(profile.getSkill()));
        if (profile.getCertificate() != null) profile.setCertificate(rsa256.rsaEncrypt(profile.getCertificate()));

        // 入库
        studentProfileMapper.insert(profile);
        return List.of(profile);
    }


    @Override
    public List<StudentProfile> decryptAndGetById(Long id) {
        // 1. 从数据库查询密文列表
        List<StudentProfile> profiles = studentProfileMapper.selectById(id);
        if (profiles == null || profiles.isEmpty()) {
            throw new RuntimeException("未找到该ID对应的数据");
        }

        // 2. 对列表中的每个对象进行完整解密
        return profiles.stream()
                .map(this::decryptProfile)
                .collect(Collectors.toList());
    }



    @Override
    public List<StudentProfile> decryptAndGetList() {
        // 1. 从数据库查询所有密文列表
        List<StudentProfile> list = studentProfileMapper.selectAll();
        if (list == null) {
            throw new RuntimeException("未找到数据");
        }

        // 2. 使用 Stream 遍历并解密每一条数据
        return list.stream()
                .map(this::decryptProfile)
                .collect(Collectors.toList());
    }

    /**
     * 抽取公共解密方法，供单条和列表查询共用
     */
    private StudentProfile decryptProfile(StudentProfile profile) {
        if (profile.getUserName() != null) {
            try {
                profile.setUserName(rsa256.rsaDecrypt(profile.getUserName()));
            } catch (Exception e) {
                logger.error("【学生服务】解密姓名失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getPhone() != null) {
            try {
                profile.setPhone(rsa256.rsaDecrypt(profile.getPhone()));
            } catch (Exception e) {
                logger.error("【学生服务】解密手机号失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getEmail() != null) {
            try {
                profile.setEmail(rsa256.rsaDecrypt(profile.getEmail()));
            } catch (Exception e) {
                logger.error("【学生服务】解密邮箱失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getCollege() != null) {
            try {
                profile.setCollege(rsa256.rsaDecrypt(profile.getCollege()));
            } catch (Exception e) {
                logger.error("【学生服务】解密学校失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getMajor() != null) {
            try {
                profile.setMajor(rsa256.rsaDecrypt(profile.getMajor()));
            } catch (Exception e) {
                logger.error("【学生服务】解密专业失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getGrade() != null) {
            try {
                profile.setGrade(rsa256.rsaDecrypt(profile.getGrade()));
            } catch (Exception e) {
                logger.error("【学生服务】解密年级失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getCareerIntentions() != null) {
            try {
                profile.setCareerIntentions(rsa256.rsaDecrypt(profile.getCareerIntentions()));
            } catch (Exception e) {
                logger.error("【学生服务】解密职业意向失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getJobIntentionDetail() != null) {
            try {
                profile.setJobIntentionDetail(rsa256.rsaDecrypt(profile.getJobIntentionDetail()));
            } catch (Exception e) {
                logger.error("【学生服务】解密求职意向详细描述失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getTargetCity() != null) {
            try {
                profile.setTargetCity(rsa256.rsaDecrypt(profile.getTargetCity()));
            } catch (Exception e) {
                logger.error("【学生服务】解密目标城市失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getExpectedSalary() != null) {
            try {
                profile.setExpectedSalary(rsa256.rsaDecrypt(profile.getExpectedSalary()));
            } catch (Exception e) {
                logger.error("【学生服务】解密期望薪资失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getIndustryPreference() != null) {
            try {
                profile.setIndustryPreference(rsa256.rsaDecrypt(profile.getIndustryPreference()));
            } catch (Exception e) {
                logger.error("【学生服务】解密行业偏好失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getEducation() != null) {
            try {
                profile.setEducation(rsa256.rsaDecrypt(profile.getEducation()));
            } catch (Exception e) {
                logger.error("【学生服务】解密学历失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getWorkExperience() != null) {
            try {
                profile.setWorkExperience(rsa256.rsaDecrypt(profile.getWorkExperience()));
            } catch (Exception e) {
                logger.error("【学生服务】解密工作经历失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getProjectExperience() != null) {
            try {
                profile.setProjectExperience(rsa256.rsaDecrypt(profile.getProjectExperience()));
            } catch (Exception e) {
                logger.error("【学生服务】解密项目经历失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getSkill() != null) {
            try {
                profile.setSkill(rsa256.rsaDecrypt(profile.getSkill()));
            } catch (Exception e) {
                logger.error("【学生服务】解密技能特长失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        if (profile.getCertificate() != null) {
            try {
                profile.setCertificate(rsa256.rsaDecrypt(profile.getCertificate()));
            } catch (Exception e) {
                logger.error("【学生服务】解密证书失败", e);
                // 解密失败时保持原加密值，避免影响其他字段的使用
            }
        }
        return profile;
    }
    @Override
    public List<StudentProfile> selectAll() {
        try {
            List<StudentProfile> profiles = studentProfileMapper.selectAll();
            // 解密敏感字段
            decryptSensitiveFields(profiles);
            return profiles;
        } catch (Exception e) {
            logger.error("【学生服务】查询所有学生信息失败", e);
            throw new RuntimeException("查询所有学生信息失败", e);
        }
    }

    @Override
    public List<StudentProfile> selectByCondition(StudentProfile student) {
        try {
            List<StudentProfile> profiles = studentProfileMapper.selectByCondition(student);
            // 解密敏感字段
            decryptSensitiveFields(profiles);
            return profiles;
        } catch (Exception e) {
            logger.error("【学生服务】条件查询学生信息失败，查询条件：{}", student, e);
            throw new RuntimeException("条件查询学生信息失败", e);
        }
    }

    @Override
    public List<StudentProfile> selectByUserId(Long userId) {
        try {
            logger.info("【学生服务】开始查询用户ID：{} 的学生信息", userId);
            List<StudentProfile> profiles = studentProfileMapper.selectByUserId(userId);
            logger.info("【学生服务】查询到用户ID：{} 的学生信息数量：{}", userId, profiles == null ? "null" : profiles.size());
            if (profiles != null && !profiles.isEmpty()) {
                logger.info("【学生服务】第一条记录ID：{}，is_deleted：{}", profiles.get(0).getId(), profiles.get(0).getIsDeleted());
            }
            // 解密敏感字段
            decryptSensitiveFields(profiles);
            logger.info("【学生服务】解密完成，返回数据数量：{}", profiles == null ? "null" : profiles.size());
            return profiles;
        } catch (Exception e) {
            logger.error("【学生服务】根据用户ID查询学生信息失败，用户ID：{}", userId, e);
            throw new RuntimeException("根据用户ID查询学生信息失败", e);
        }
    }

    @Override
    public List<StudentProfile> selectById(Long id) {
        try {
            List<StudentProfile> profiles = studentProfileMapper.selectById(id);
            // 解密敏感字段
            decryptSensitiveFields(profiles);
            return profiles;
        } catch (Exception e) {
            logger.error("【学生服务】根据ID查询学生信息失败，ID：{}", id, e);
            throw new RuntimeException("根据ID查询学生信息失败", e);
        }
    }

    @Override
    public List<StudentProfile> chooseSelect(StudentProfile student) {
        try {
            List<StudentProfile> profiles = studentProfileMapper.chooseSelect(student);
            // 解密敏感字段
            decryptSensitiveFields(profiles);
            return profiles;
        } catch (Exception e) {
            logger.error("【学生服务】优先级条件查询失败，查询条件：{}", student, e);
            throw new RuntimeException("优先级条件查询失败", e);
        }
    }

    /**
     * 解密学生信息中的敏感字段
     */
    private void decryptSensitiveFields(List<StudentProfile> profiles) {
        for (StudentProfile profile : profiles) {
            if (profile.getUserName() != null) {
                try {
                    profile.setUserName(rsa256.rsaDecrypt(profile.getUserName()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密姓名失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getPhone() != null) {
                try {
                    profile.setPhone(rsa256.rsaDecrypt(profile.getPhone()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密手机号失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getEmail() != null) {
                try {
                    profile.setEmail(rsa256.rsaDecrypt(profile.getEmail()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密邮箱失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getCollege() != null) {
                try {
                    profile.setCollege(rsa256.rsaDecrypt(profile.getCollege()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密学校失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getMajor() != null) {
                try {
                    profile.setMajor(rsa256.rsaDecrypt(profile.getMajor()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密专业失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getGrade() != null) {
                try {
                    profile.setGrade(rsa256.rsaDecrypt(profile.getGrade()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密年级失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getCareerIntentions() != null) {
                try {
                    profile.setCareerIntentions(rsa256.rsaDecrypt(profile.getCareerIntentions()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密职业意向失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getJobIntentionDetail() != null) {
                try {
                    profile.setJobIntentionDetail(rsa256.rsaDecrypt(profile.getJobIntentionDetail()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密求职意向详细描述失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getTargetCity() != null) {
                try {
                    profile.setTargetCity(rsa256.rsaDecrypt(profile.getTargetCity()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密目标城市失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getExpectedSalary() != null) {
                try {
                    profile.setExpectedSalary(rsa256.rsaDecrypt(profile.getExpectedSalary()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密期望薪资失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getIndustryPreference() != null) {
                try {
                    profile.setIndustryPreference(rsa256.rsaDecrypt(profile.getIndustryPreference()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密行业偏好失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getEducation() != null) {
                try {
                    profile.setEducation(rsa256.rsaDecrypt(profile.getEducation()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密学历失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getWorkExperience() != null) {
                try {
                    profile.setWorkExperience(rsa256.rsaDecrypt(profile.getWorkExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密工作经历失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getProjectExperience() != null) {
                try {
                    profile.setProjectExperience(rsa256.rsaDecrypt(profile.getProjectExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密项目经历失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getSkill() != null) {
                try {
                    profile.setSkill(rsa256.rsaDecrypt(profile.getSkill()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密技能特长失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
            if (profile.getCertificate() != null) {
                try {
                    profile.setCertificate(rsa256.rsaDecrypt(profile.getCertificate()));
                } catch (Exception e) {
                    logger.error("【学生服务】解密证书失败", e);
                    // 解密失败时保持原加密值，避免影响其他字段的使用
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 所有异常都回滚事务
    public List<StudentProfile> insert(StudentProfile student) {
        try {
            // 使用雪花算法生成分布式ID
            student.setId(SnowIdCreater.generateId(3)); // 类别3=student_profile

            // 使用RSA加密所有敏感字段
            if (student.getUserName() != null) {
                try {
                    student.setUserName(rsa256.rsaEncrypt(student.getUserName()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密姓名失败", e);
                    throw new RuntimeException("加密姓名失败", e);
                }
            }
            if (student.getPhone() != null) {
                try {
                    student.setPhone(rsa256.rsaEncrypt(student.getPhone()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密手机号失败", e);
                    throw new RuntimeException("加密手机号失败", e);
                }
            }
            if (student.getEmail() != null) {
                try {
                    student.setEmail(rsa256.rsaEncrypt(student.getEmail()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密邮箱失败", e);
                    throw new RuntimeException("加密邮箱失败", e);
                }
            }
            if (student.getCollege() != null) {
                try {
                    student.setCollege(rsa256.rsaEncrypt(student.getCollege()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密学校失败", e);
                    throw new RuntimeException("加密学校失败", e);
                }
            }
            if (student.getMajor() != null) {
                try {
                    student.setMajor(rsa256.rsaEncrypt(student.getMajor()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密专业失败", e);
                    throw new RuntimeException("加密专业失败", e);
                }
            }
            if (student.getGrade() != null) {
                try {
                    student.setGrade(rsa256.rsaEncrypt(student.getGrade()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密年级失败", e);
                    throw new RuntimeException("加密年级失败", e);
                }
            }
            if (student.getCareerIntentions() != null) {
                try {
                    student.setCareerIntentions(rsa256.rsaEncrypt(student.getCareerIntentions()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密职业意向失败", e);
                    throw new RuntimeException("加密职业意向失败", e);
                }
            }
            if (student.getJobIntentionDetail() != null) {
                try {
                    student.setJobIntentionDetail(rsa256.rsaEncrypt(student.getJobIntentionDetail()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密求职意向详细描述失败", e);
                    throw new RuntimeException("加密求职意向详细描述失败", e);
                }
            }
            if (student.getTargetCity() != null) {
                try {
                    student.setTargetCity(rsa256.rsaEncrypt(student.getTargetCity()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密目标城市失败", e);
                    throw new RuntimeException("加密目标城市失败", e);
                }
            }
            if (student.getExpectedSalary() != null) {
                try {
                    student.setExpectedSalary(rsa256.rsaEncrypt(student.getExpectedSalary()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密期望薪资失败", e);
                    throw new RuntimeException("加密期望薪资失败", e);
                }
            }
            if (student.getIndustryPreference() != null) {
                try {
                    student.setIndustryPreference(rsa256.rsaEncrypt(student.getIndustryPreference()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密行业偏好失败", e);
                    throw new RuntimeException("加密行业偏好失败", e);
                }
            }
            if (student.getEducation() != null) {
                try {
                    student.setEducation(rsa256.rsaEncrypt(student.getEducation()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密学历失败", e);
                    throw new RuntimeException("加密学历失败", e);
                }
            }
            if (student.getWorkExperience() != null) {
                try {
                    student.setWorkExperience(rsa256.rsaEncrypt(student.getWorkExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密工作经历失败", e);
                    throw new RuntimeException("加密工作经历失败", e);
                }
            }
            if (student.getProjectExperience() != null) {
                try {
                    student.setProjectExperience(rsa256.rsaEncrypt(student.getProjectExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密项目经历失败", e);
                    throw new RuntimeException("加密项目经历失败", e);
                }
            }
            if (student.getSkill() != null) {
                try {
                    student.setSkill(rsa256.rsaEncrypt(student.getSkill()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密技能特长失败", e);
                    throw new RuntimeException("加密技能特长失败", e);
                }
            }
            if (student.getCertificate() != null) {
                try {
                    student.setCertificate(rsa256.rsaEncrypt(student.getCertificate()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密证书失败", e);
                    throw new RuntimeException("加密证书失败", e);
                }
            }

            return studentProfileMapper.insert(student);
        } catch (Exception e) {
            logger.error("【学生服务】新增学生信息失败，学生数据：{}", student, e);
            throw new RuntimeException("新增学生信息失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentProfile> update(StudentProfile student) {
        try {
            // 参数前置校验
            if (student.getId() == null || student.getId() <= 0) {
                throw new IllegalArgumentException("学生ID不能为空且必须大于0");
            }

            // 敏感字段更新时重新加密
            if (student.getUserName() != null) {
                try {
                    student.setUserName(rsa256.rsaEncrypt(student.getUserName()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密姓名失败", e);
                    throw new RuntimeException("加密姓名失败", e);
                }
            }
            if (student.getPhone() != null) {
                try {
                    student.setPhone(rsa256.rsaEncrypt(student.getPhone()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密手机号失败", e);
                    throw new RuntimeException("加密手机号失败", e);
                }
            }
            if (student.getEmail() != null) {
                try {
                    student.setEmail(rsa256.rsaEncrypt(student.getEmail()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密邮箱失败", e);
                    throw new RuntimeException("加密邮箱失败", e);
                }
            }
            if (student.getCollege() != null) {
                try {
                    student.setCollege(rsa256.rsaEncrypt(student.getCollege()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密学校失败", e);
                    throw new RuntimeException("加密学校失败", e);
                }
            }
            if (student.getMajor() != null) {
                try {
                    student.setMajor(rsa256.rsaEncrypt(student.getMajor()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密专业失败", e);
                    throw new RuntimeException("加密专业失败", e);
                }
            }
            if (student.getGrade() != null) {
                try {
                    student.setGrade(rsa256.rsaEncrypt(student.getGrade()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密年级失败", e);
                    throw new RuntimeException("加密年级失败", e);
                }
            }
            if (student.getCareerIntentions() != null) {
                try {
                    student.setCareerIntentions(rsa256.rsaEncrypt(student.getCareerIntentions()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密职业意向失败", e);
                    throw new RuntimeException("加密职业意向失败", e);
                }
            }
            if (student.getJobIntentionDetail() != null) {
                try {
                    student.setJobIntentionDetail(rsa256.rsaEncrypt(student.getJobIntentionDetail()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密求职意向详细描述失败", e);
                    throw new RuntimeException("加密求职意向详细描述失败", e);
                }
            }
            if (student.getTargetCity() != null) {
                try {
                    student.setTargetCity(rsa256.rsaEncrypt(student.getTargetCity()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密目标城市失败", e);
                    throw new RuntimeException("加密目标城市失败", e);
                }
            }
            if (student.getExpectedSalary() != null) {
                try {
                    student.setExpectedSalary(rsa256.rsaEncrypt(student.getExpectedSalary()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密期望薪资失败", e);
                    throw new RuntimeException("加密期望薪资失败", e);
                }
            }
            if (student.getIndustryPreference() != null) {
                try {
                    student.setIndustryPreference(rsa256.rsaEncrypt(student.getIndustryPreference()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密行业偏好失败", e);
                    throw new RuntimeException("加密行业偏好失败", e);
                }
            }
            if (student.getEducation() != null) {
                try {
                    student.setEducation(rsa256.rsaEncrypt(student.getEducation()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密学历失败", e);
                    throw new RuntimeException("加密学历失败", e);
                }
            }
            if (student.getWorkExperience() != null) {
                try {
                    student.setWorkExperience(rsa256.rsaEncrypt(student.getWorkExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密工作经历失败", e);
                    throw new RuntimeException("加密工作经历失败", e);
                }
            }
            if (student.getProjectExperience() != null) {
                try {
                    student.setProjectExperience(rsa256.rsaEncrypt(student.getProjectExperience()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密项目经历失败", e);
                    throw new RuntimeException("加密项目经历失败", e);
                }
            }
            if (student.getSkill() != null) {
                try {
                    student.setSkill(rsa256.rsaEncrypt(student.getSkill()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密技能特长失败", e);
                    throw new RuntimeException("加密技能特长失败", e);
                }
            }
            if (student.getCertificate() != null) {
                try {
                    student.setCertificate(rsa256.rsaEncrypt(student.getCertificate()));
                } catch (Exception e) {
                    logger.error("【学生服务】加密证书失败", e);
                    throw new RuntimeException("加密证书失败", e);
                }
            }

            // 查询原数据
            List<StudentProfile> originalProfiles = studentProfileMapper.selectById(student.getId());
            if (!originalProfiles.isEmpty()) {
                StudentProfile original = originalProfiles.get(0);
                // 比较主要字段是否相同
                if (isProfileEqual(original, student)) {
                    throw new RuntimeException("更新后与原先相同");
                }
            }

            return studentProfileMapper.update(student);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                throw e;
            }
            if (e instanceof IllegalArgumentException) {
                logger.warn("【学生服务】更新学生参数错误：{}", e.getMessage());
                throw e;
            }
            logger.error("【学生服务】更新学生信息失败，学生数据：{}", student, e);
            throw new RuntimeException("更新学生信息失败", e);
        } catch (Exception e) {
            logger.error("【学生服务】更新学生信息失败，学生数据：{}", student, e);
            throw new RuntimeException("更新学生信息失败", e);
        }
    }

    // 比较两个学生档案是否相同
    private boolean isProfileEqual(StudentProfile original, StudentProfile updated) {
        return (updated.getUserId() == null || updated.getUserId().equals(original.getUserId())) &&
                (updated.getUserName() == null || updated.getUserName().equals(original.getUserName())) &&
                (updated.getGender() == null || updated.getGender().equals(original.getGender())) &&
                (updated.getPhone() == null || updated.getPhone().equals(original.getPhone())) &&
                (updated.getEmail() == null || updated.getEmail().equals(original.getEmail())) &&
                (updated.getCollege() == null || updated.getCollege().equals(original.getCollege())) &&
                (updated.getMajor() == null || updated.getMajor().equals(original.getMajor())) &&
                (updated.getGrade() == null || updated.getGrade().equals(original.getGrade())) &&
                (updated.getProfileStatus() == null || updated.getProfileStatus().equals(original.getProfileStatus())) &&
                (updated.getAge() == null || updated.getAge().equals(original.getAge())) &&
                (updated.getGraduationDate() == null || updated.getGraduationDate().equals(original.getGraduationDate())) &&
                (updated.getCareerIntentions() == null || updated.getCareerIntentions().equals(original.getCareerIntentions())) &&
                (updated.getJobIntentionDetail() == null || updated.getJobIntentionDetail().equals(original.getJobIntentionDetail())) &&
                (updated.getTargetCity() == null || updated.getTargetCity().equals(original.getTargetCity())) &&
                (updated.getExpectedSalary() == null || updated.getExpectedSalary().equals(original.getExpectedSalary())) &&
                (updated.getIndustryPreference() == null || updated.getIndustryPreference().equals(original.getIndustryPreference())) &&
                (updated.getWorkTypePreference() == null || updated.getWorkTypePreference().equals(original.getWorkTypePreference())) &&
                (updated.getMaxLearningCycle() == null || updated.getMaxLearningCycle().equals(original.getMaxLearningCycle())) &&
                (updated.getEducation() == null || updated.getEducation().equals(original.getEducation())) &&
                (updated.getWorkExperience() == null || updated.getWorkExperience().equals(original.getWorkExperience())) &&
                (updated.getProjectExperience() == null || updated.getProjectExperience().equals(original.getProjectExperience())) &&
                (updated.getSkill() == null || updated.getSkill().equals(original.getSkill())) &&
                (updated.getCertificate() == null || updated.getCertificate().equals(original.getCertificate())) &&
                (updated.getStudentGroup() == null || updated.getStudentGroup().equals(original.getStudentGroup())) &&
                (updated.getPrivacyLevel() == null || updated.getPrivacyLevel().equals(original.getPrivacyLevel()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentProfile> deleteById(Long studentId) {
        try {
            if (studentId == null || studentId <= 0) {
                throw new IllegalArgumentException("学生ID不能为空且必须大于0");
            }
            return studentProfileMapper.deleteById(studentId);
        } catch (IllegalArgumentException e) {
            logger.warn("【学生服务】删除学生参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【学生服务】删除学生信息失败，学生ID：{}", studentId, e);
            throw new RuntimeException("删除学生信息失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentProfile> batchDelete(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量删除的ID列表不能为空");
            }
            return studentProfileMapper.batchDelete(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【学生服务】批量删除参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【学生服务】批量删除学生信息失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量删除学生信息失败", e);
        }
    }

    @Override
    public List<StudentProfile> batchSelect(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("批量查询的ID列表不能为空");
            }
            return studentProfileMapper.batchSelect(ids);
        } catch (IllegalArgumentException e) {
            logger.warn("【学生服务】批量查询参数错误：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【学生服务】批量查询学生信息失败，ID列表：{}", ids, e);
            throw new RuntimeException("批量查询学生信息失败", e);
        }
    }


}
