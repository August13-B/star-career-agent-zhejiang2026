package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;

public interface ScenarioAgentGateway {
    record Output(String text, JsonNode score) {}
    Output execute(String namespace, String prompt, Consumer<String> onText);
}
