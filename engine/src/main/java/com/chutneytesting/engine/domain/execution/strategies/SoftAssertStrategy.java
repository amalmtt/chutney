package com.chutneytesting.engine.domain.execution.strategies;

import com.chutneytesting.engine.domain.execution.ScenarioExecution;
import com.chutneytesting.engine.domain.execution.engine.scenario.ScenarioContext;
import com.chutneytesting.engine.domain.execution.engine.step.Step;
import com.chutneytesting.engine.domain.execution.report.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftAssertStrategy implements StepExecutionStrategy {

    private static final String TYPE = "soft-assert";
    private static final Logger LOGGER = LoggerFactory.getLogger(SoftAssertStrategy.class);

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Status execute(ScenarioExecution scenarioExecution,
                          Step step,
                          ScenarioContext scenarioContext,
                          StepExecutionStrategies strategies) {

        if (step.isParentStep()) {
            return softenStatus(executeSubSteps(scenarioExecution, step, scenarioContext, strategies));
        }

        return softenStatus(step.execute(scenarioExecution, scenarioContext));
    }

    private Status executeSubSteps(ScenarioExecution scenarioExecution, Step step, ScenarioContext scenarioContext, StepExecutionStrategies strategies) {
        final Map<Step, List<Status>> subStepsStatuses = new HashMap<>();
        subStepsStatuses.putIfAbsent(step, new ArrayList<>());
        Iterator<Step> subStepsIterator = step.subSteps().iterator();
        step.beginExecution(scenarioExecution);
        try {
            Step currentRunningStep = step;
            while (subStepsIterator.hasNext()) {
                try {
                    currentRunningStep = subStepsIterator.next();
                    StepExecutionStrategy strategy = strategies.buildStrategyFrom(currentRunningStep);
                    Status childStatus = strategy.execute(scenarioExecution, currentRunningStep, scenarioContext, strategies);
                    subStepsStatuses.get(step).add(childStatus);
                } catch (RuntimeException e) {
                    LOGGER.warn("Intercepted exception!", e);
                    currentRunningStep.failure(e);
                    subStepsStatuses.get(step).add(step.status());
                }
            }
        } finally {
            step.endExecution(scenarioExecution);
        }
        return Status.worst(subStepsStatuses.get(step));
    }

    private Status softenStatus(Status status) {
        return status == Status.FAILURE ? Status.WARN : status;
    }
}
