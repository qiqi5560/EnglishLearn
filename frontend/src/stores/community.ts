import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as communityApi from '@/api/modules/community'
import type { CommentDto, PostDto } from '@/types/api'

export const useCommunityStore = defineStore('community', () => {
  const posts = ref<PostDto[]>([])
  const total = ref(0)
  const topics = ref<string[]>([])
  const loading = ref(false)
  const currentPost = ref<PostDto | null>(null)
  const currentComments = ref<CommentDto[]>([])

  async function loadTopics() {
    topics.value = await communityApi.listTopics()
    return topics.value
  }

  async function loadPosts(params: { topic?: string; page?: number; pageSize?: number } = {}) {
    loading.value = true
    try {
      const res = await communityApi.listPosts({ topic: params.topic, page: params.page ?? 1, pageSize: params.pageSize ?? 10 })
      posts.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  async function loadDetail(id: number) {
    const res = await communityApi.postDetail(id)
    currentPost.value = res.post
    currentComments.value = res.comments
    return res
  }

  async function publish(payload: { title: string; content: string; topic: string }) {
    const post = await communityApi.createPost(payload)
    return post
  }

  /** 点赞/取消点赞并同步本地计数 */
  async function toggleLike(post: PostDto) {
    const res = await communityApi.toggleLike(post.id)
    post.likes = res.likes
    post.liked = res.liked
    return res
  }

  async function comment(postId: number, content: string) {
    const c = await communityApi.addComment(postId, content)
    currentComments.value.push(c)
    const p = posts.value.find((x) => x.id === postId)
    if (p) p.comments += 1
    if (currentPost.value && currentPost.value.id === postId) currentPost.value.comments += 1
    return c
  }

  async function remove(postId: number) {
    await communityApi.deletePost(postId)
  }

  return {
    posts,
    total,
    topics,
    loading,
    currentPost,
    currentComments,
    loadTopics,
    loadPosts,
    loadDetail,
    publish,
    toggleLike,
    comment,
    remove,
  }
})
