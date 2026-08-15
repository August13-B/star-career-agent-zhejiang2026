package org.example.web.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.example.web.entity.StudentProfile;
import org.example.web.entity.User;
import org.example.web.mapper.StudentProfileMapper;
import org.example.web.mapper.UserMapper;
import org.example.web.service.ResumeExportService;
import org.example.web.tool.RSA_256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeExportServiceImpl implements ResumeExportService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeExportServiceImpl.class);

    /**
     * 简历存储目录
     */
    private static final String RESUME_DIR = "resumes";
    private String resumeDirAbsolutePath;

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final RSA_256 rsa256;

    @PostConstruct
    public void init() {
        // 创建简历存储目录
        String projectRoot = System.getProperty("user.dir");
        resumeDirAbsolutePath = projectRoot + File.separator + RESUME_DIR;
        File dir = new File(resumeDirAbsolutePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("【简历导出服务】成功创建简历存储目录：{}", resumeDirAbsolutePath);
            } else {
                logger.warn("【简历导出服务】创建简历存储目录失败：{}", resumeDirAbsolutePath);
            }
        }
    }

    @Override
    public File exportResume(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不能为空且必须大于0");
        }

        // 查询用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 查询学生档案信息
        List<StudentProfile> profileList = studentProfileMapper.selectByUserId(userId);
        StudentProfile profile = profileList.isEmpty() ? null : profileList.get(0);

        // 创建Word文档
        return createResumeDocument(userId, user, profile);
    }

    /**
     * 创建简历Word文档
     */
    private File createResumeDocument(Long userId, User user, StudentProfile profile) {
        XWPFDocument document = new XWPFDocument();

        // 创建标题
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("个人简历");
        titleRun.setFontSize(24);
        titleRun.setBold(true);
        titleRun.addBreak();

        // 基本信息部分
        XWPFParagraph basicInfoTitle = document.createParagraph();
        XWPFRun basicInfoTitleRun = basicInfoTitle.createRun();
        basicInfoTitleRun.setText("一、基本信息");
        basicInfoTitleRun.setBold(true);

        // 用户账号
        addInfoRow(document, "用户账号", user.getUserAccount());

        // 用户角色
        String roleName = getUserRoleName(user.getUserRole());
        addInfoRow(document, "角色", roleName);

        // 账号状态
        String statusName = getUserStatusName(user.getUserStatus());
        addInfoRow(document, "账号状态", statusName);

        // 创建时间
        String createTime = user.getCreateTime() != null 
            ? user.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) 
            : "未知";
        addInfoRow(document, "注册时间", createTime);

        // 如果有学生档案信息
        if (profile != null) {
            // 解密敏感字段
            decryptProfile(profile);

            // 个人信息部分
            XWPFParagraph personalTitle = document.createParagraph();
            XWPFRun personalTitleRun = personalTitle.createRun();
            personalTitleRun.setText("二、个人信息");
            personalTitleRun.setBold(true);

            addInfoRow(document, "姓名", profile.getUserName());
            addInfoRow(document, "性别", profile.getGender() != null && profile.getGender() == 1 ? "男" : "女");
            addInfoRow(document, "年龄", profile.getAge() != null ? profile.getAge().toString() : "未知");
            addInfoRow(document, "联系电话", profile.getPhone());
            addInfoRow(document, "邮箱", profile.getEmail());
            addInfoRow(document, "学校", profile.getCollege());
            addInfoRow(document, "专业", profile.getMajor());
            addInfoRow(document, "年级", profile.getGrade());
            addInfoRow(document, "学历", profile.getEducation());

            // 求职意向部分
            XWPFParagraph intentionTitle = document.createParagraph();
            XWPFRun intentionTitleRun = intentionTitle.createRun();
            intentionTitleRun.setText("三、求职意向");
            intentionTitleRun.setBold(true);

            addInfoRow(document, "职业意向", profile.getCareerIntentions());
            addInfoRow(document, "求职详情", profile.getJobIntentionDetail());
            addInfoRow(document, "目标城市", profile.getTargetCity());
            addInfoRow(document, "期望薪资", profile.getExpectedSalary());
            addInfoRow(document, "行业偏好", profile.getIndustryPreference());
            String workType = profile.getWorkTypePreference() != null ? profile.getWorkTypePreference().toString() : "未知";
            addInfoRow(document, "工作类型偏好", workType);

            // 工作经历部分
            XWPFParagraph experienceTitle = document.createParagraph();
            XWPFRun experienceTitleRun = experienceTitle.createRun();
            experienceTitleRun.setText("四、工作经历");
            experienceTitleRun.setBold(true);
            addInfoRow(document, "工作经验", profile.getWorkExperience());
            addInfoRow(document, "项目经验", profile.getProjectExperience());

            // 技能证书部分
            XWPFParagraph skillTitle = document.createParagraph();
            XWPFRun skillTitleRun = skillTitle.createRun();
            skillTitleRun.setText("五、技能特长");
            skillTitleRun.setBold(true);
            addInfoRow(document, "专业技能", profile.getSkill());
            addInfoRow(document, "证书资质", profile.getCertificate());

            // 能力评价部分
            XWPFParagraph abilityTitle = document.createParagraph();
            XWPFRun abilityTitleRun = abilityTitle.createRun();
            abilityTitleRun.setText("六、其他信息");
            abilityTitleRun.setBold(true);
            addInfoRow(document, "毕业日期", profile.getGraduationDate() != null ? profile.getGraduationDate().toString() : "未知");
            String cycle = profile.getMaxLearningCycle() != null ? profile.getMaxLearningCycle().toString() + "个月" : "未知";
            addInfoRow(document, "最长学习周期", cycle);
            String privacy = profile.getPrivacyLevel() != null ? profile.getPrivacyLevel().toString() : "未知";
            addInfoRow(document, "隐私等级", privacy);
        }

        // 添加文档生成时间
        XWPFParagraph footer = document.createParagraph();
        footer.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun footerRun = footer.createRun();
        footerRun.setText("生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        footerRun.setFontSize(10);

        // 保存文档
        String fileName = "resume_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ".docx";
        File outputFile = new File(resumeDirAbsolutePath + File.separator + fileName);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.write(out);
            logger.info("【简历导出服务】简历导出成功，文件路径：{}", outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            logger.error("【简历导出服务】简历导出失败", e);
            throw new RuntimeException("简历导出失败：" + e.getMessage(), e);
        } finally {
            try {
                document.close();
            } catch (IOException e) {
                logger.warn("【简历导出服务】关闭文档失败", e);
            }
        }
    }

    /**
     * 添加信息行
     */
    private void addInfoRow(XWPFDocument document, String label, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setText(label + "：");
        run.setBold(false);
        run.setText(value != null ? value : "未填写");
    }

    /**
     * 获取用户角色名称
     */
    private String getUserRoleName(Integer role) {
        if (role == null) return "未知";
        return switch (role) {
            case 1 -> "学生";
            case 2 -> "管理员";
            case 3 -> "企业端";
            case 4 -> "导师";
            default -> "未知";
        };
    }

    /**
     * 获取用户状态名称
     */
    private String getUserStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "未激活";
            case 3 -> "冻结";
            case 4 -> "注销";
            default -> "未知";
        };
    }

    /**
     * 解密学生档案敏感字段
     */
    private void decryptProfile(StudentProfile profile) {
        if (profile.getUserName() != null) {
            try {
                profile.setUserName(rsa256.rsaDecrypt(profile.getUserName()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密姓名失败", e);
            }
        }
        if (profile.getPhone() != null) {
            try {
                profile.setPhone(rsa256.rsaDecrypt(profile.getPhone()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密手机号失败", e);
            }
        }
        if (profile.getEmail() != null) {
            try {
                profile.setEmail(rsa256.rsaDecrypt(profile.getEmail()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密邮箱失败", e);
            }
        }
        if (profile.getCollege() != null) {
            try {
                profile.setCollege(rsa256.rsaDecrypt(profile.getCollege()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密学校失败", e);
            }
        }
        if (profile.getMajor() != null) {
            try {
                profile.setMajor(rsa256.rsaDecrypt(profile.getMajor()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密专业失败", e);
            }
        }
        if (profile.getGrade() != null) {
            try {
                profile.setGrade(rsa256.rsaDecrypt(profile.getGrade()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密年级失败", e);
            }
        }
        if (profile.getCareerIntentions() != null) {
            try {
                profile.setCareerIntentions(rsa256.rsaDecrypt(profile.getCareerIntentions()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密职业意向失败", e);
            }
        }
        if (profile.getJobIntentionDetail() != null) {
            try {
                profile.setJobIntentionDetail(rsa256.rsaDecrypt(profile.getJobIntentionDetail()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密求职详情失败", e);
            }
        }
        if (profile.getTargetCity() != null) {
            try {
                profile.setTargetCity(rsa256.rsaDecrypt(profile.getTargetCity()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密目标城市失败", e);
            }
        }
        if (profile.getExpectedSalary() != null) {
            try {
                profile.setExpectedSalary(rsa256.rsaDecrypt(profile.getExpectedSalary()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密期望薪资失败", e);
            }
        }
        if (profile.getIndustryPreference() != null) {
            try {
                profile.setIndustryPreference(rsa256.rsaDecrypt(profile.getIndustryPreference()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密行业偏好失败", e);
            }
        }
        if (profile.getWorkTypePreference() != null) {
            try {
                String decrypted = rsa256.rsaDecrypt(profile.getWorkTypePreference().toString());
                profile.setWorkTypePreference(Integer.parseInt(decrypted));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密工作类型偏好失败", e);
            }
        }
        if (profile.getEducation() != null) {
            try {
                profile.setEducation(rsa256.rsaDecrypt(profile.getEducation()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密学历失败", e);
            }
        }
        if (profile.getWorkExperience() != null) {
            try {
                profile.setWorkExperience(rsa256.rsaDecrypt(profile.getWorkExperience()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密工作经验失败", e);
            }
        }
        if (profile.getProjectExperience() != null) {
            try {
                profile.setProjectExperience(rsa256.rsaDecrypt(profile.getProjectExperience()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密项目经验失败", e);
            }
        }
        if (profile.getSkill() != null) {
            try {
                profile.setSkill(rsa256.rsaDecrypt(profile.getSkill()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密技能失败", e);
            }
        }
        if (profile.getCertificate() != null) {
            try {
                profile.setCertificate(rsa256.rsaDecrypt(profile.getCertificate()));
            } catch (Exception e) {
                logger.warn("【简历导出服务】解密证书失败", e);
            }
        }
    }

    @Override
    public String getResumeDir() {
        return resumeDirAbsolutePath;
    }
}