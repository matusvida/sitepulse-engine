package com.sitepulse.engine.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {

    private final ObjectMapper objectMapper;

    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize JSON", ex);
        }
    }

    public List<Double> readDoubleList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse JSON list", ex);
        }
    }

    public List<List<Double>> readPolygon(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse ROI polygon", ex);
        }
    }

    public List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse JSON string list", ex);
        }
    }

    public Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse JSON object", ex);
        }
    }

    public <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse JSON object", ex);
        }
    }
}
