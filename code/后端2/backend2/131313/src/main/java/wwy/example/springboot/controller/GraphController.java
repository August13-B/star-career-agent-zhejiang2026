package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.dto.GraphVO;
import wwy.example.springboot.entity.JobPromotionGraph;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.entity.JobTransferGraph;
import wwy.example.springboot.service.JobPromotionGraphService;
import wwy.example.springboot.service.JobRequirementProfileService;
import wwy.example.springboot.service.JobTransferGraphService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class GraphController {

    private final JobRequirementProfileService profileService;
    private final JobPromotionGraphService promotionGraphService;
    private final JobTransferGraphService transferGraphService;

    @GetMapping("/graph/{profileId}")
    public Result<GraphVO> getGraphData(@PathVariable Long profileId) {
        // 1. 查询中心岗位
        JobRequirementProfile profile = profileService.findById(profileId);
        if (profile == null) {
            return Result.error("岗位画像不存在");
        }

        GraphVO graphVO = new GraphVO();

        // 2. 组装中心节点
        GraphVO.CenterNode center = new GraphVO.CenterNode();
        center.setName(profile.getPositionName());
        center.setCategory(profile.getCategory());
        graphVO.setCenter(center);

        // 3. 晋升图谱
        List<JobPromotionGraph> promotionList = promotionGraphService.findByMainJobId(profileId);
        JobPromotionGraph promotionGraph = promotionList.isEmpty() ? null : promotionList.get(0);
        List<GraphVO.PromotionNode> promotions = new ArrayList<>();
        if (promotionGraph != null) {
            // 晋升岗位 1
            addPromotionNode(promotions, promotionGraph.getPromotionJob1Desc(),
                    promotionGraph.getPromotionJob1SkillDiff(),
                    promotionGraph.getPromotionJob1Experience(),
                    promotionGraph.getPromotionJob1LearningCycle());
            // 晋升岗位 2
            addPromotionNode(promotions, promotionGraph.getPromotionJob2Desc(),
                    promotionGraph.getPromotionJob2SkillDiff(),
                    promotionGraph.getPromotionJob2Experience(),
                    promotionGraph.getPromotionJob2LearningCycle());
            // 晋升岗位 3
            addPromotionNode(promotions, promotionGraph.getPromotionJob3Desc(),
                    promotionGraph.getPromotionJob3SkillDiff(),
                    promotionGraph.getPromotionJob3Experience(),
                    promotionGraph.getPromotionJob3LearningCycle());
            // 晋升岗位 4
            addPromotionNode(promotions, promotionGraph.getPromotionJob4Desc(),
                    promotionGraph.getPromotionJob4SkillDiff(),
                    promotionGraph.getPromotionJob4Experience(),
                    promotionGraph.getPromotionJob4LearningCycle());
            // 晋升岗位 5
            addPromotionNode(promotions, promotionGraph.getPromotionJob5Desc(),
                    promotionGraph.getPromotionJob5SkillDiff(),
                    promotionGraph.getPromotionJob5Experience(),
                    promotionGraph.getPromotionJob5LearningCycle());
        }
        graphVO.setPromotions(promotions);

        // 4. 换岗图谱
        List<JobTransferGraph> transferList = transferGraphService.findByMainJobId(profileId);
        JobTransferGraph transferGraph = transferList.isEmpty() ? null : transferList.get(0);
        List<GraphVO.TransferNode> transfers = new ArrayList<>();
        if (transferGraph != null) {
            // 换岗岗位 1
            addTransferNode(transfers, transferGraph.getTransferJob1Desc(),
                    transferGraph.getTransferJob1SkillDiff(),
                    transferGraph.getTransferJob1Education(),
                    transferGraph.getTransferJob1Experience(),
                    transferGraph.getTransferJob1LearningCycle(),
                    transferGraph.getTransferJob1Difficulty());
            // 换岗岗位 2
            addTransferNode(transfers, transferGraph.getTransferJob2Desc(),
                    transferGraph.getTransferJob2SkillDiff(),
                    transferGraph.getTransferJob2Education(),
                    transferGraph.getTransferJob2Experience(),
                    transferGraph.getTransferJob2LearningCycle(),
                    transferGraph.getTransferJob2Difficulty());
            // 换岗岗位 3
            addTransferNode(transfers, transferGraph.getTransferJob3Desc(),
                    transferGraph.getTransferJob3SkillDiff(),
                    transferGraph.getTransferJob3Education(),
                    transferGraph.getTransferJob3Experience(),
                    transferGraph.getTransferJob3LearningCycle(),
                    transferGraph.getTransferJob3Difficulty());
            // 换岗岗位 4
            addTransferNode(transfers, transferGraph.getTransferJob4Desc(),
                    transferGraph.getTransferJob4SkillDiff(),
                    transferGraph.getTransferJob4Education(),
                    transferGraph.getTransferJob4Experience(),
                    transferGraph.getTransferJob4LearningCycle(),
                    transferGraph.getTransferJob4Difficulty());
            // 换岗岗位 5
            addTransferNode(transfers, transferGraph.getTransferJob5Desc(),
                    transferGraph.getTransferJob5SkillDiff(),
                    transferGraph.getTransferJob5Education(),
                    transferGraph.getTransferJob5Experience(),
                    transferGraph.getTransferJob5LearningCycle(),
                    transferGraph.getTransferJob5Difficulty());
            // 换岗岗位 6
            addTransferNode(transfers, transferGraph.getTransferJob6Desc(),
                    transferGraph.getTransferJob6SkillDiff(),
                    transferGraph.getTransferJob6Education(),
                    transferGraph.getTransferJob6Experience(),
                    transferGraph.getTransferJob6LearningCycle(),
                    transferGraph.getTransferJob6Difficulty());
            // 换岗岗位 7
            addTransferNode(transfers, transferGraph.getTransferJob7Desc(),
                    transferGraph.getTransferJob7SkillDiff(),
                    transferGraph.getTransferJob7Education(),
                    transferGraph.getTransferJob7Experience(),
                    transferGraph.getTransferJob7LearningCycle(),
                    transferGraph.getTransferJob7Difficulty());
            // 换岗岗位 8
            addTransferNode(transfers, transferGraph.getTransferJob8Desc(),
                    transferGraph.getTransferJob8SkillDiff(),
                    transferGraph.getTransferJob8Education(),
                    transferGraph.getTransferJob8Experience(),
                    transferGraph.getTransferJob8LearningCycle(),
                    transferGraph.getTransferJob8Difficulty());
        }
        graphVO.setTransfers(transfers);

        return Result.success(graphVO);
    }

    private void addPromotionNode(List<GraphVO.PromotionNode> list, String desc, String skillDiff,
                                  String experience, Integer learningCycle) {
        if (desc != null && !desc.trim().isEmpty()) {
            GraphVO.PromotionNode node = new GraphVO.PromotionNode();
            node.setName(desc);
            node.setSkillDiff(skillDiff);
            node.setExperience(experience);
            node.setLearningCycle(learningCycle);
            list.add(node);
        }
    }

    private void addTransferNode(List<GraphVO.TransferNode> list, String desc, String skillDiff,
                                 String education, String experience, Integer learningCycle, Integer difficulty) {
        if (desc != null && !desc.trim().isEmpty()) {
            GraphVO.TransferNode node = new GraphVO.TransferNode();
            node.setName(desc);
            node.setSkillDiff(skillDiff);
            node.setEducation(education);
            node.setExperience(experience);
            node.setLearningCycle(learningCycle);
            node.setDifficulty(difficulty);
            list.add(node);
        }
    }
}