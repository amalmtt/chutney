package com.chutneytesting.admin.infra.gitbackup;

import static com.chutneytesting.admin.domain.gitbackup.ChutneyContentCategory.SCENARIO;

import com.chutneytesting.admin.domain.gitbackup.ChutneyContent;
import com.chutneytesting.admin.domain.gitbackup.ChutneyContentCategory;
import com.chutneytesting.admin.domain.gitbackup.ChutneyContentProvider;
import com.chutneytesting.design.domain.scenario.compose.ComposableTestCaseRepository;
import com.chutneytesting.tools.ui.ComposableIdUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ChutneyComposedScenarioContent implements ChutneyContentProvider {

    private final ComposableTestCaseRepository repository;
    private final ObjectMapper mapper;

    public ChutneyComposedScenarioContent(ComposableTestCaseRepository repository,
                                          @Qualifier("gitObjectMapper") ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public String provider() {
        return "composed";
    }

    @Override
    public ChutneyContentCategory category() {
        return SCENARIO;
    }

    @Override
    public Stream<ChutneyContent> getContent() {
        return repository.findAll().stream()
            .map(tcm -> ComposableIdUtils.fromFrontId(tcm.id()))
            .map(repository::findById)
            .map(t -> {
                ChutneyContent.ChutneyContentBuilder builder = ChutneyContent.builder()
                    .withProvider(provider())
                    .withCategory(category())
                    .withName("[" + ComposableIdUtils.toFrontId(t.id()) + "]-" + t.metadata().title());
                try {
                    builder
                        .withContent(mapper.writeValueAsString(t))
                        .withFormat("json");
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                return builder.build();
            });
    }

    @Override
    public void importDefaultFolder(Path workingDirectory) {
        // TODO
    }
}
