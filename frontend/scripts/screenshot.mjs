// 页面截图验证脚本：启动 dev server 后运行  node scripts/screenshot.mjs
import { mkdirSync } from 'node:fs'
import puppeteer from 'puppeteer-core'

const BASE = 'http://localhost:5173'
const OUT = 'screenshots'
mkdirSync(OUT, { recursive: true })

const CHROME = 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'

const pages = [
  { name: 'login', path: '/login', auth: null, w: 390, h: 844 },
  { name: 'home', path: '/home', auth: 'learner', w: 390, h: 844 },
  { name: 'practice', path: '/practice', auth: 'learner', w: 390, h: 844 },
  { name: 'dialogue', path: '/dialogue/1', auth: 'learner', w: 390, h: 844 },
  { name: 'summary', path: '/dialogue/1/summary', auth: 'learner', w: 390, h: 844 },
  { name: 'report', path: '/report', auth: 'learner', w: 390, h: 844 },
  { name: 'community', path: '/community', auth: 'learner', w: 390, h: 844 },
  { name: 'admin-dashboard', path: '/admin/dashboard', auth: 'admin', w: 1440, h: 900 },
]

const users = {
  learner: {
    token: 'mock-token',
    userInfo: {
      userId: 1,
      nickname: '学习者',
      phone: '13800000000',
      ageGroup: 'adult',
      role: 'learner',
      level: 'A2',
    },
  },
  admin: {
    token: 'admin-token',
    userInfo: {
      userId: 100,
      nickname: '管理员',
      phone: '13800000000',
      ageGroup: 'adult',
      role: 'admin',
    },
  },
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars'],
})

for (const p of pages) {
  const page = await browser.newPage()
  await page.setViewport({ width: p.w, height: p.h, deviceScaleFactor: 2 })

  // 先访问一次以建立 origin，再写入登录态（覆盖路由守卫）
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  if (p.auth) {
    const u = users[p.auth]
    await page.evaluate(
      (a) => {
        localStorage.setItem('token', a.token)
        localStorage.setItem('userInfo', JSON.stringify(a.userInfo))
      },
      u,
    )
  }

  await page.goto(`${BASE}${p.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await sleep(1200) // 等待 ECharts / 懒加载路由渲染

  await page.screenshot({ path: `${OUT}/${p.name}.png` })
  console.log(`saved ${p.name}.png`)
  await page.close()
}

await browser.close()
console.log('done')
