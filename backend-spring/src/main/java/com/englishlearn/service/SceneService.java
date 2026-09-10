package com.englishlearn.service;

import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.Scene;
import com.englishlearn.repository.SceneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 场景库查询服务（F002）。
 */
@Service
public class SceneService {

    private final SceneRepository sceneRepository;

    public SceneService(SceneRepository sceneRepository) {
        this.sceneRepository = sceneRepository;
    }

    public Map<String, Object> listScenes(String category, String level, String keyword, int page, int pageSize) {
        Page<Scene> result = sceneRepository.search(category, level, keyword, PageRequest.of(page - 1, pageSize));
        return Map.of(
                "list", result.getContent().stream().map(Dtos::sceneToDict).toList(),
                "total", result.getTotalElements());
    }

    public List<Map<String, Object>> recommended(int limit) {
        return sceneRepository.findByStatusOrderBySceneId(1).stream()
                .limit(limit)
                .map(Dtos::sceneToDict)
                .toList();
    }

    public Scene getScene(Integer id) {
        return sceneRepository.findById(id).orElse(null);
    }

    public Scene getAvailableScene(Integer id) {
        Scene s = getScene(id);
        return (s != null && s.status != null && s.status == 1) ? s : null;
    }
}