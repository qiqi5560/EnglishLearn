import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/modules/auth'
import type { LoginResult, UserDto } from '@/types/api'

const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'

function readStoredUser(): UserDto | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserDto) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserDto | null>(readStoredUser())

  const role = computed(() => userInfo.value?.role ?? 'learner')
  const isLoggedIn = computed(() => !!token.value)
  const level = computed(() => userInfo.value?.level ?? null)

  function setToken(val: string) {
    token.value = val
    localStorage.setItem(TOKEN_KEY, val)
  }

  function setUserInfo(info: UserDto) {
    userInfo.value = info
    localStorage.setItem(USER_KEY, JSON.stringify(info))
  }

  function applyLogin(res: LoginResult) {
    setToken(res.accessToken)
    setUserInfo(res.user)
  }

  /** 手机号验证码登录（未注册自动注册） */
  async function smsLogin(payload: { phone: string; code: string; nickname?: string; ageGroup?: string }) {
    const res = await authApi.loginByCode(payload)
    applyLogin(res)
  }

  /** 密码登录 */
  async function passwordLogin(payload: { phone: string; password: string }) {
    const res = await authApi.loginByPassword(payload)
    applyLogin(res)
  }

  /** 密码注册并登录 */
  async function register(payload: { phone: string; password: string; nickname?: string; ageGroup?: string }) {
    const res = await authApi.register(payload)
    applyLogin(res)
  }

  async function sendCode(phone: string) {
    await authApi.sendSmsCode(phone)
  }

  /** 管理员登录（后端校验 admin 角色） */
  async function adminLogin(payload: { phone: string; password: string }) {
    const res = await authApi.adminLogin(payload)
    applyLogin(res)
  }

  /** 拉取最新用户信息（含等级等） */
  async function fetchMe() {
    const me = await authApi.fetchMe()
    setUserInfo(me)
    return me
  }

  async function updateProfile(payload: authApi.UpdateProfilePayload) {
    const me = await authApi.updateProfile(payload)
    setUserInfo(me)
    return me
  }

  /** 绑定监护人（监护人手机号 + 验证码） */
  async function bindGuardian(payload: { guardianPhone: string; code: string }) {
    const me = await authApi.bindGuardian(payload)
    setUserInfo(me)
    return me
  }

  /** 修改登录密码 */
  async function updatePassword(payload: { oldPassword: string; newPassword: string }) {
    return authApi.updatePassword(payload)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    userInfo,
    role,
    isLoggedIn,
    level,
    setToken,
    setUserInfo,
    smsLogin,
    passwordLogin,
    register,
    sendCode,
    adminLogin,
    fetchMe,
    updateProfile,
    bindGuardian,
    updatePassword,
    logout,
  }
})
