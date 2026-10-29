package io.student.rangiffler.jupiter.condition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Включает класс-образец только в запуске из EngineTestKit, который передаёт параметр конфигурации {@link #ENABLED}.
 */
public final class EngineTestKitSampleCondition implements ExecutionCondition {

    public static final String ENABLED = "engine-test-kit.sample.enabled";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context.getConfigurationParameter(ENABLED, Boolean::parseBoolean).orElse(false)
                ? ConditionEvaluationResult.enabled("Образец запущен из EngineTestKit")
                : ConditionEvaluationResult.disabled("Образец для EngineTestKit не запускается в обычном прогоне");
    }
}
