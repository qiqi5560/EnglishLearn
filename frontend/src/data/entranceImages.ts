/**
 * 入学测评「看图描述」静态题库。
 *
 * 使用方式：
 * 1. 把你的图片放进  frontend/public/entrance-images/  目录；
 * 2. 在下方数组里，每张图片配一条提问（image 填相对 public 的路径，例如 /entrance-images/my-pic.jpg）；
 * 3. 每次进入测评会从这里随机取一张图片 + 对应问题作为题目。
 *
 * 大模型只负责最后的评价，不负责生成图片与题目。
 */
export interface EntranceImageItem {
  /** 图片地址（相对 public 根路径） */
  image: string
  /** 对应的提问内容 */
  question: string
}

export const entranceImages: EntranceImageItem[] = [
  {
    image: '/entrance-images/sample-1.jpg',
    question: '请用英语描述：图片中的人物正在做什么，周围的环境是怎样的？',
  },
  {
    image: '/entrance-images/sample-2.jpg',
    question: '请用英语描述：图片里有哪些物品，它们的位置关系如何？',
  },
  {
    image: '/entrance-images/sample-3.jpg',
    question: '请用英语描述：图片中的场景是什么，你能看出什么细节？',
  },
]