package com.aquilaoffice.adapter.out.ai;

import com.aquilaoffice.application.port.out.LoadModelCatalogPort;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

/**
 * classpath 리소스(model-catalog.json)에서 다중 AI 모델 공급자 카탈로그 프리셋을 1회 적재해 제공한다.
 */
@Repository
public class ModelCatalogRegistry implements LoadModelCatalogPort {

    private static final String CATALOG_RESOURCE_PATH = "model-catalog.json";

    private final List<ModelCatalogPreset> presets;

    @Autowired
    public ModelCatalogRegistry(ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    public ModelCatalogRegistry(ObjectMapper objectMapper) {
        this.presets = loadCatalog(objectMapper);
    }

    @Override
    public List<ModelCatalogPreset> loadPresets() {
        return presets;
    }

    private static List<ModelCatalogPreset> loadCatalog(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE_PATH);
        try (InputStream stream = resource.getInputStream()) {
            CatalogDocument doc = objectMapper.readValue(stream, CatalogDocument.class);
            if (doc == null || doc.presets() == null) {
                return List.of();
            }
            return List.copyOf(doc.presets());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to load model catalog resource", exception);
        }
    }

    private record CatalogDocument(List<ModelCatalogPreset> presets) {}
}
