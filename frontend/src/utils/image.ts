/**
 * 图片处理：手机原图动辄好几 MB，上传前先缩到头像够用的尺寸再压成 JPEG。
 */

/** GIF 可能是动图，压缩会丢帧，原样上传 */
const PASSTHROUGH_TYPES = ['image/gif']

/**
 * 把图片缩放并压成 JPEG。
 *
 * @param file   用户选中的图片
 * @param maxSize 最长边的上限（像素），头像 512 足够
 */
export async function compressImage(file: File, maxSize = 512, quality = 0.85): Promise<Blob> {
  if (PASSTHROUGH_TYPES.includes(file.type)) return file

  const img = await loadImage(file)
  const longest = Math.max(img.width, img.height)
  const scale = Math.min(1, maxSize / longest)

  // 尺寸已经够小且文件不大，没必要重新编码（透明通道也能保住）
  if (scale === 1 && file.size <= 300 * 1024) return file

  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(img.width * scale))
  canvas.height = Math.max(1, Math.round(img.height * scale))

  const ctx = canvas.getContext('2d')
  if (!ctx) return file
  // JPEG 没有透明通道，先铺白底，避免 PNG 透明区域变成黑块
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

  return await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('图片处理失败'))),
      'image/jpeg',
      quality,
    )
  })
}

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败，请换一张试试'))
    }
    img.src = url
  })
}
