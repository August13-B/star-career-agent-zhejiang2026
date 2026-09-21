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
@CrossOrigin
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class GraphController {

    private final JobRequirementProfileService profileService;
    private final JobPromotionGraphService promotionGraphService;
    private final JobTransferGraphService transferGraphService;
    private final org.example.web.service.TboxAgentService tboxAgentService;
    private final org.example.web.service.impl.StudentProfileContextService profileContext;

    /** 按岗位画像生成个人探索预览，不把模型建议写入公共岗位库。 */
    @PostMapping("/graph/{profileId}/preview")
    public Result<GraphVO> preview(@PathVariable Long profileId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(org.example.web.tool.JwtUtil.parseToken(token).get("id")));
        } catch (Exception e) { return Result.unauthorized("请登录后生成个人职业星图"); }
        JobRequirementProfile profile = profileService.findById(profileId);
        if (profile == null) return Result.notFound("岗位画像不存在");
        try {
            String prompt = "基于以下资料生成职业探索路径，仅输出JSON。它是AI参考建议，不是确定的晋升承诺。"
                    + "promotions与transfers各最多3条，没有依据返回空数组；name必须是岗位名称，"
                    + "skillDiff说明与当前能力差距。learningCycle无法估计时为null，不编造学历和经历。"
                    + "结构：{\"promotions\":[{\"name\":\"岗位名\",\"skillDiff\":\"能力差距\",\"experience\":\"参考经验\",\"learningCycle\":null}],"
                    + "\"transfers\":[{\"name\":\"岗位名\",\"skillDiff\":\"差距\",\"education\":\"参考学历\",\"experience\":\"参考经验\",\"learningCycle\":null,\"difficulty\":2}]}\n"
                    + profileContext.build(userId, profile.getPositionName(), "以选择的岗位画像为基准")
                    + "\n选中的岗位资料：" + cn.hutool.json.JSONUtil.toJsonStr(profile);
            String text = tboxAgentService.chatSync(userId, null, prompt);
            if (text == null || text.indexOf('{') < 0 || text.lastIndexOf('}') <= text.indexOf('{'))
                return Result.error("AI未返回有效路径，请稍后重试");
            GraphVO graph = cn.hutool.json.JSONUtil.toBean(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1), GraphVO.class);
            if (graph.getPromotions() == null || graph.getTransfers() == null)
                return Result.error("AI路径结构不完整，请重试");
            graph.setPromotions(graph.getPromotions().stream().filter(n -> n != null && n.getName() != null && !n.getName().isBlank()).limit(3).toList());
            graph.setTransfers(graph.getTransfers().stream().filter(n -> n != null && n.getName() != null && !n.getName().isBlank()).limit(3).toList());
            if (graph.getPromotions().isEmpty() && graph.getTransfers().isEmpty())
                return Result.error("当前资料不足以形成路径，请先完善个人画像");
            GraphVO.CenterNode center = new GraphVO.CenterNode();
            center.setName(profile.getPositionName()); center.setCategory(profile.getCategory());
            graph.setCenter(center);
            return Result.success("AI个人探索预览，不写入公共岗位库", graph);
        } catch (Exception e) { return Result.error("AI路径生成失败，请稍后重试"); }
    }

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
