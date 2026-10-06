package wwy.example.springboot;

import org.junit.jupiter.api.Test;
import org.example.web.service.TboxAgentService;
import org.example.web.service.impl.StudentProfileContextService;
import org.example.web.tool.JwtUtil;
import wwy.example.springboot.controller.GraphController;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.service.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphPreviewTest {
    static {
        new JwtUtil("test-only-jwt-signing-key-32-bytes-long");
    }

    final JobRequirementProfileService profiles = mock(JobRequirementProfileService.class);
    final JobPromotionGraphService promotions = mock(JobPromotionGraphService.class);
    final JobTransferGraphService transfers = mock(JobTransferGraphService.class);
    final TboxAgentService ai = mock(TboxAgentService.class);
    final StudentProfileContextService context = mock(StudentProfileContextService.class);
    final GraphController controller = new GraphController(profiles, promotions, transfers, ai, context);
    final String token = JwtUtil.genToken(Map.of("id", 42L));
    @Test void rejectsAnonymous() {
        assertEquals(401, controller.preview(12L, null).getCode());
        verifyNoInteractions(ai);
    }
    @Test void usesProfileIdAndReturnsPreviewWithoutDatabaseWrites() {
        var profile = new JobRequirementProfile(); profile.setPositionName("前端开发");
        when(profiles.findById(12L)).thenReturn(profile);
        when(context.build(eq(42L), eq("前端开发"), anyString())).thenReturn("个人画像");
        when(ai.chatSync(eq(42L), isNull(), anyString())).thenReturn("{\"promotions\":[{\"name\":\"高级前端\"}],\"transfers\":[]}");
        var result = controller.preview(12L, token);
        assertEquals(200, result.getCode());
        assertEquals("前端开发", result.getData().getCenter().getName());
        assertEquals(1, result.getData().getPromotions().size());
        verifyNoInteractions(promotions, transfers);
    }
    @Test void rejectsMalformedOutput() {
        when(profiles.findById(12L)).thenReturn(new JobRequirementProfile());
        when(ai.chatSync(eq(42L), isNull(), anyString())).thenReturn("{\"error\":\"unavailable\"}");
        assertEquals(500, controller.preview(12L, token).getCode());
    }
}
