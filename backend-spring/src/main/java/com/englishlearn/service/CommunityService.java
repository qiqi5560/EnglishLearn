package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.CommunityComment;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.PostLike;
import com.englishlearn.entity.User;
import com.englishlearn.repository.CommunityCommentRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.PostLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习社区服务（F008）：帖子、点赞、评论、话题。
 */
@Service
public class CommunityService {

    private static final List<String> DEFAULT_TOPICS = List.of("学习心得", "结伴练习", "资源分享", "提问求助");

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;

    public CommunityService(CommunityPostRepository postRepository,
                            CommunityCommentRepository commentRepository,
                            PostLikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    public List<String> topics() {
        Set<String> merged = new LinkedHashSet<>();
        for (String t : postRepository.findDistinctTopics()) {
            if (t != null && !t.isBlank()) {
                merged.add(t);
            }
        }
        merged.addAll(DEFAULT_TOPICS);
        return new ArrayList<>(merged);
    }

    public Map<String, Object> listPosts(User user, String topic, int page, int pageSize) {
        Page<CommunityPost> result = postRepository.searchPublished(topic, PageRequest.of(page - 1, pageSize));
        Set<Integer> liked = likedSet(user.userId, result.getContent().stream()
                .filter(p -> p.postId != null).map(p -> p.postId).toList());
        return Map.of(
                "list", result.getContent().stream().map(p -> Dtos.postToDict(p, liked.contains(p.postId))).toList(),
                "total", result.getTotalElements());
    }

    @Transactional
    public Map<String, Object> createPost(User user, String title, String content, String topic) {
        if (title == null || title.isBlank()) {
            throw new ApiException(422, "标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new ApiException(422, "内容不能为空");
        }
        CommunityPost post = new CommunityPost();
        post.author = user;
        post.title = title.strip();
        post.content = content.strip();
        post.topic = (topic == null || topic.isBlank()) ? "学习心得" : topic.strip();
        post.likes = 0;
        post.commentCount = 0;
        post.status = 1;
        post.isTop = false;
        post = postRepository.save(post);
        return Dtos.postToDict(post, false);
    }

    public Map<String, Object> postDetail(User user, Integer postId) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null || (post.status != null && post.status != 1 && !user.userId.equals(authorId(post)))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        List<CommunityComment> comments = commentRepository.findByPostIdOrderByCommentIdAsc(postId);
        boolean liked = likeRepository.existsByPostIdAndUserId(postId, user.userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("post", Dtos.postToDict(post, liked));
        data.put("comments", comments.stream().map(Dtos::commentToDict).toList());
        return data;
    }

    @Transactional
    public Map<String, Object> toggleLike(User user, Integer postId) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        PostLike existing = likeRepository.findByPostIdAndUserId(postId, user.userId).orElse(null);
        boolean liked;
        int likes = post.likes == null ? 0 : post.likes;
        if (existing != null) {
            likeRepository.delete(existing);
            likes = Math.max(0, likes - 1);
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.postId = postId;
            like.userId = user.userId;
            likeRepository.save(like);
            likes += 1;
            liked = true;
        }
        post.likes = likes;
        postRepository.save(post);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", liked);
        data.put("likes", likes);
        return data;
    }

    @Transactional
    public Map<String, Object> addComment(User user, Integer postId, String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException(422, "评论内容不能为空");
        }
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null || (post.status != null && post.status != 1)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        CommunityComment comment = new CommunityComment();
        comment.postId = postId;
        comment.author = user;
        comment.content = content.strip();
        comment = commentRepository.save(comment);
        post.commentCount = (post.commentCount == null ? 0 : post.commentCount) + 1;
        postRepository.save(post);
        return Dtos.commentToDict(comment);
    }

    @Transactional
    public void deletePost(User user, Integer postId) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        if (!user.userId.equals(authorId(post)) && !"admin".equals(user.userRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能删除自己的帖子");
        }
        commentRepository.deleteAll(commentRepository.findByPostIdOrderByCommentIdAsc(postId));
        likeRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private Integer authorId(CommunityPost post) {
        return post.author != null ? post.author.userId : null;
    }

    private Set<Integer> likedSet(Integer userId, List<Integer> postIds) {
        if (postIds.isEmpty()) {
            return Set.of();
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (PostLike like : likeRepository.findByUserIdAndPostIdIn(userId, postIds)) {
            result.add(like.postId);
        }
        return result;
    }
}