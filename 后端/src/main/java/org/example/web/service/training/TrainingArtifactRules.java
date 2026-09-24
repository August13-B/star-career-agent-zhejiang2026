package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class TrainingArtifactRules {
    private final ObjectMapper json;
    public TrainingArtifactRules(ObjectMapper json) { this.json=json; }
    public ObjectNode validate(TrainingTemplate.Definition template, JsonNode input, boolean complete) {
        if (!template.needsArtifact() || input == null || !input.isObject()) throw bad("该场景不接受此作品格式");
        Set<String> allowed=new HashSet<>(); template.fields().forEach(f->allowed.add(f.path("key").asText()));
        input.fieldNames().forEachRemaining(key->{if (!allowed.contains(key)) throw bad("作品包含未知字段");});
        ObjectNode normalized=json.createObjectNode(); int length=0;
        for (JsonNode field:template.fields()) {
            String key=field.path("key").asText(); JsonNode value=input.get(key);
            if (value!=null && !value.isTextual()) throw bad("作品字段必须是文本");
            String text=value==null?"":value.asText(); length+=text.length();
            if (text.length()>field.path("maxLength").asInt(4000)) throw bad(field.path("label").asText()+"过长");
            if (complete && field.path("required").asBoolean() && text.isBlank()) throw bad("请填写："+field.path("label").asText());
            if (!text.isBlank() && field.has("options")) {
                boolean found=false; for(JsonNode option:field.path("options")) if(option.asText().equals(text)) found=true;
                if (!found) throw bad("请选择有效的"+field.path("label").asText());
            }
            normalized.put(key,text);
        }
        if(length>20000) throw bad("作品最多20000字");
        return normalized;
    }
    public List<Map<String,Object>> factChecks(TrainingTemplate.Definition template, JsonNode artifact) {
        if(!"ai_assisted_office".equals(template.scenario())) return List.of();
        return List.of(check("总报名人数",decimal(artifact.path("totalRegistrations").asText(),"54"),"24 + 20 + 10 = 54 人"),
                check("总体转化率",decimal(artifact.path("overallConversion").asText().replace("%",""),"18"),"54 / (120 + 80 + 100) = 18%"),
                check("最高转化渠道","社群".equals(artifact.path("bestChannel").asText()),"社群 20 / 80 = 25%，公众号20%，短视频10%"),
                check("投放决策","未决定，需进一步讨论".equals(artifact.path("investmentDecision").asText()),"会议纪要未批准增加投放"));
    }
    private Map<String,Object> check(String name,boolean passed,String explanation){return Map.of("name",name,"passed",passed,"explanation",explanation);}
    private boolean decimal(String value,String expected){try{return new BigDecimal(value.strip()).compareTo(new BigDecimal(expected))==0;}catch(Exception e){return false;}}
    private TrainingException bad(String message){return new TrainingException(422,"ARTIFACT_INVALID",message);}
}
