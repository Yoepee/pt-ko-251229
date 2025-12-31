import { apiClient } from './axios';

export const authApi = {
  signup: (data: any) => apiClient.post('/api/v1/auth/signup', data),
  login: (data: any) => apiClient.post('/api/v1/auth/login', data),
  logout: () => apiClient.post('/api/v1/auth/logout'),
  me: () => apiClient.get('/api/v1/auth/me').then((res) => res.data.data),
  withdraw: () => apiClient.delete('/api/v1/auth/withdraw'),
  updateNickname: (nickname: string) => apiClient.patch('/api/v1/auth/me/nickname', { nickname }),
  updatePassword: (data: { currentPassword: string; newPassword: string }) => apiClient.patch('/api/v1/auth/me/password', data),
};
