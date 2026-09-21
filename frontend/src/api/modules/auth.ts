import { http } from '../request'
import type { LoginResult, PartnerDto, UserDto } from '@/types/api'

export function sendSmsCode(phone: string) {
  return http.post<null>('/auth/send-code', { phone })
}

export interface LoginPayload {
  phone: string
  code?: string
  password?: string
  nickname?: string
  ageGroup?: string
}

export function loginByCode(payload: LoginPayload) {
  return http.post<LoginResult>('/auth/login', payload)
}

export function loginByPassword(payload: LoginPayload) {
  return http.post<LoginResult>('/auth/login', payload)
}

export function register(payload: LoginPayload & { password: string }) {
  return http.post<LoginResult>('/auth/register', payload)
}

export function adminLogin(payload: { phone: string; password: string }) {
  return http.post<LoginResult>('/auth/admin/login', payload)
}

export function fetchMe() {
  return http.get<UserDto>('/users/me')
}

export interface UpdateProfilePayload {
  nickname?: string
  avatarUrl?: string
  /** 个性签名（他人主页展示） */
  bio?: string
  ageGroup?: string
  guardianId?: number
}

export function updateProfile(payload: UpdateProfilePayload) {
  return http.put<UserDto>('/users/me', payload)
}

/** 上传自定义头像：成功返回最新用户信息，直接覆盖本地缓存即可 */
export function uploadAvatar(file: Blob) {
  const form = new FormData()
  form.append('file', file, 'avatar.jpg')
  return http.post<UserDto>('/users/me/avatar', form, { timeout: 30000 })
}

export function updatePassword(payload: { oldPassword: string; newPassword: string }) {
  return http.put<null>('/users/me/password', payload)
}

export function bindGuardian(payload: { guardianPhone: string; code: string }) {
  return http.post<UserDto>('/users/me/bind-guardian', payload)
}

export function listPartners() {
  return http.get<PartnerDto[]>('/users/me/partners')
}

export function addPartner(phone: string) {
  return http.post<PartnerDto>('/users/me/partners', { phone })
}

export function deletePartner(partnerUserId: number) {
  return http.delete<null>(`/users/me/partners/${partnerUserId}`)
}
