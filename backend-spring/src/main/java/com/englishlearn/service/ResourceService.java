package com.englishlearn.service;

import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.repository.LearningResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 学习资源查询服务（F004）。
 */
@Service
public class ResourceService {

    private final LearningResourceRepository resourceRepository;

    public ResourceService(LearningResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Map<String, Object> listResources(String type, String category, String level, String keyword, int page, int pageSize) {
        Page<LearningResource> result = resourceRepository.search(type, category, level, keyword, PageRequest.of(page - 1, pageSize));
        return Map.of(
                "list", result.getContent().stream().map(Dtos::resourceToDict).toList(),
                "total", result.getTotalElements());
    }

    public LearningResource getResource(Integer id) {
        return resourceRepository.findById(id).orElse(null);
    }

    public LearningResource getAvailableResource(Integer id) {
        LearningResource r = getResource(id);
        return (r != null && r.status != null && r.status == 1) ? r : null;
    }
}