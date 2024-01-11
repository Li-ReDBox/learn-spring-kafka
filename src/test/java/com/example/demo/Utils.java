package com.example.demo;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {
    public static String loadContentAsString(String aFile) throws IOException {
        var resource = new ClassPathResource(aFile);
        return new String(resource.getInputStream().readAllBytes());
    }

    public static JsonNode readAsJsonNode(String jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        var resource = new ClassPathResource(jsonFile);
        return objectMapper.readTree(resource.getInputStream());
    }
}
