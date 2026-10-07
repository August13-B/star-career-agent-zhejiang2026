package org.example.web.service.profile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 画像更新幅度策略（B 口径）单测：首次 ±35、再次 ≤10 且降低需理由、简历加分只加不减。 */
class ProfileScorePolicyTest {

    @Test void firstTimeDetection() {
        assertTrue(ProfileScorePolicy.isFirstTime(null));
        assertTrue(ProfileScorePolicy.isFirstTime(""));
        assertTrue(ProfileScorePolicy.isFirstTime("   "));
        assertTrue(ProfileScorePolicy.isFirstTime("baseline"));
        assertFalse(ProfileScorePolicy.isFirstTime("assessment"));
        assertFalse(ProfileScorePolicy.isFirstTime("resume"));
        assertFalse(ProfileScorePolicy.isFirstTime("report"));
    }

    @Test void firstAssessmentAllowsBigSwingWithin25To95() {
        assertEquals(95, ProfileScorePolicy.clamp(true, 60, 100, null));
        assertEquals(25, ProfileScorePolicy.clamp(true, 60, 0, null));
        assertEquals(25, ProfileScorePolicy.clamp(true, 60, 25, null));
        assertEquals(80, ProfileScorePolicy.clamp(true, 60, 80, null));
        // 首次以基线 60 为锚（不锚定 before），因此当前值很低时也能大幅上调
        assertEquals(95, ProfileScorePolicy.clamp(true, 20, 100, null));
        assertEquals(25, ProfileScorePolicy.clamp(true, 90, 10, null));
    }

    @Test void againAllowsAtMost10EitherWay() {
        // 首次之后：升也要受 ±10 约束
        assertEquals(70, ProfileScorePolicy.clamp(false, 60, 90, "项目经历明显提升"));
        assertEquals(50, ProfileScorePolicy.clamp(false, 60, 20, "证据不足，下调"));
        assertEquals(63, ProfileScorePolicy.clamp(false, 60, 63, "小幅提升"));
        assertEquals(0, ProfileScorePolicy.clamp(false, 5, 0, "明显不足"));
    }

    @Test void againRejectsDropWithoutReason() {
        assertEquals(60, ProfileScorePolicy.clamp(false, 60, 40, null));
        assertEquals(60, ProfileScorePolicy.clamp(false, 60, 40, "   "));
        // 带理由才采纳下调
        assertEquals(50, ProfileScorePolicy.clamp(false, 60, 40, "本次测评沟通维度证据不足"));
    }

    @Test void resumeBonusOnlyUpAndCapped() {
        // 首次：单项最多 +15
        assertEquals(75, ProfileScorePolicy.resumeBonus(true, true, 60, 95));
        // 非首次但该维度此前为空（简历刚补上内容）：仍可补分，上限同样 +15
        assertEquals(75, ProfileScorePolicy.resumeBonus(false, true, 60, 95));
        // 非首次且该维度非空：不加分
        assertEquals(60, ProfileScorePolicy.resumeBonus(false, false, 60, 95));
        // 简历建议低于当前分：只加不减
        assertEquals(60, ProfileScorePolicy.resumeBonus(true, true, 60, 40));
        // 封顶 100
        assertEquals(100, ProfileScorePolicy.resumeBonus(true, true, 95, 100));
    }

    @Test void boundsAndTotal() {
        assertEquals(0, ProfileScorePolicy.bound(-5));
        assertEquals(100, ProfileScorePolicy.bound(120));
        assertEquals(60.0, ProfileScorePolicy.total(60, 60));
        assertEquals(66.0, ProfileScorePolicy.total(80, 60));
        assertEquals(29.8, ProfileScorePolicy.total(20, 34));   // 6 + 23.8
    }
}
