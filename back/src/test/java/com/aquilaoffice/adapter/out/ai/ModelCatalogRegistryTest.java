package com.aquilaoffice.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelProviderTier;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModelCatalogRegistryTest {

    @Test
    void loadsCuratedPresetsForAllSupportedProviderKinds() {
        ModelCatalogRegistry registry = new ModelCatalogRegistry(new ObjectMapper());

        List<ModelCatalogPreset> presets = registry.loadPresets();
        Map<ModelProviderKind, ModelCatalogPreset> byKind = presets.stream()
                .collect(Collectors.toMap(ModelCatalogPreset::providerKind, Function.identity()));

        assertThat(byKind.keySet()).containsExactlyInAnyOrder(ModelProviderKind.values());
        assertThat(byKind.get(ModelProviderKind.OPENAI).wireProtocol()).isEqualTo(ModelWireProtocol.OPENAI_RESPONSES);
        assertThat(byKind.get(ModelProviderKind.ANTHROPIC).wireProtocol())
                .isEqualTo(ModelWireProtocol.ANTHROPIC_MESSAGES);
        assertThat(byKind.get(ModelProviderKind.GOOGLE).wireProtocol()).isEqualTo(ModelWireProtocol.GOOGLE_GEMINI);
        assertThat(byKind.get(ModelProviderKind.GOOGLE).tier()).isEqualTo(ModelProviderTier.FREE);
        assertThat(byKind.get(ModelProviderKind.OLLAMA).tier()).isEqualTo(ModelProviderTier.LOCAL);
        assertThat(byKind.get(ModelProviderKind.OLLAMA).authRequired()).isFalse();
        assertThat(presets).allSatisfy(preset -> assertThat(preset.models()).isNotEmpty());
    }
}
