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

export interface Category {
  id: number;
  name: string;
  slug: string | null;
  sortOrder: number;
}

export interface CreateCategoryRequest {
  name: string;
  slug: string | null;
  sortOrder: number;
}

export interface UpdateCategoryRequest {
  name: string;
  slug: string | null;
  sortOrder: number;
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

export const categoryApi = {
  getAll: () => apiClient.get('/api/v1/categories').then((res) => res.data.data as Category[]),
  create: (data: CreateCategoryRequest) => apiClient.post('/api/v1/categories', data),
  update: (id: number, data: UpdateCategoryRequest) => apiClient.patch(`/api/v1/categories/${id}`, data),
  delete: (id: number) => apiClient.delete(`/api/v1/categories/${id}`),
};
