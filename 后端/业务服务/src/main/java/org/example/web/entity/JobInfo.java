package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobInfo {
    /**
     * 非自增主键（雪花算法生成）
     */
    private Long id;

    /**
     * 关联岗位要求画像表的id（初始为空，后绑定）
     */
    private Long jobId;

    /**
     * 岗位名称
     */
    private String jobName;

    /**
     * 工作地址
     */
    private String address;

    /**
     * 薪资范围
     */
    private String salaryRange;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 所属行业
     */
    private String industry;

    /**
     * 公司规模
     */
    private String companyScale;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 岗位编码
     */
    private String jobCode;

    /**
     * 岗位详情
     */
    private String jobDetail;

    /**
     * 更新日期
     */
    private String updateDate;

    /**
     * 公司详情
     */
    private String companyDetail;

    /**
     * 岗位来源地址
     */
    private String jobSourceUrl;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 删除标识（0-未删除，1-已删除）
     */
    private Integer isDeleted;
}
