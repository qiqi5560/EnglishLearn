package com.englishlearn.service;

import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.User;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.LevelPredictService.LevelPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 场景库查询服务（F002）。
 */
@Service
public class SceneService {

    private final SceneRepository sceneRepository;
    private final ConversationSessionRepository sessionRepository;
    private final AuthFacade authFacade;
    private final LevelPredictService levelPredictService;
    private final RecommendService recommendService;

    public SceneService(SceneRepository sceneRepository,
                        ConversationSessionRepository sessionRepository,
                        AuthFacade authFacade,
                        LevelPredictService levelPredictService,
                        RecommendService recommendService) {
        this.sceneRepository = sceneRepository;
        this.sessionRepository = sessionRepository;
        this.authFacade = authFacade;
        this.levelPredictService = levelPredictService;
        this.recommendService = recommendService;
    }

    public Map<String, Object> listScenes(String category, String level, String keyword, int page, int pageSize) {
        Page<Scene> result = sceneRepository.search(category, level, keyword, PageRequest.of(page - 1, pageSize));
        return Map.of(
                "list", result.getContent().stream().map(Dtos::sceneToDict).toList(),
                "total", result.getTotalElements());
    }

    /**
     * 首页推荐场景：已登录时走个性化推荐（水平预测 + 三策略融合），
     * 未登录或推荐结果为空时降级为全局热门（按历史会话数倒序）。
     */
    public List<Map<String, Object>> recommended(int limit) {
        User user = authFacade.currentUser();
        if (user == null) {
            return popular(limit);
        }
        LevelPrediction prediction = levelPredictService.predict(user);
        List<Map<String, Object>> list = recommendService.recommendScenes(user, prediction, limit);
        return list.isEmpty() ? popular(limit) : list;
    }

    /** 全局热门场景：按累计会话数倒序，会话数相同则按场景 ID 升序 */
    private List<Map<String, Object>> popular(int limit) {
        return sceneRepository.findByStatusOrderBySceneId(1).stream()
                .sorted(Comparator.<Scene>comparingLong(s -> sessionRepository.countBySceneId(s.sceneId))
                        .reversed()
                        .thenComparing((Scene s) -> s.sceneId))
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