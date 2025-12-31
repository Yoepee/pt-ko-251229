import { apiClient } from './axios';

interface SignupRequest {
  username: string;
  password: string;
  nickname: string;
}

interface LoginRequest {
  username: string;
  password: string;
}

export const authApi = {
  signup: (data: SignupRequest) => apiClient.post('/api/v1/auth/signup', data),
  login: (data: LoginRequest) => apiClient.post('/api/v1/auth/login', data),
  logout: () => apiClient.post('/api/v1/auth/logout'),
  me: () => apiClient.get('/api/v1/auth/me').then((res) => res.data.data),
  withdraw: () => apiClient.post('/api/v1/auth/withdraw'),
  updateNickname: (nickname: string) => apiClient.patch('/api/v1/auth/me/nickname', { nickname }),
  updatePassword: (data: { currentPassword: string; newPassword: string }) => apiClient.patch('/api/v1/auth/me/password', data),
};
