import { apiClient } from './axios';

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

export interface User {
  id: number;
  username: string;
  nickname: string;
  avatarUrl?: string;
  role?: string;
}

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
  signup: (data: SignupRequest) => apiClient.post<ApiResponse<null>>('/api/v1/auth/signup', data),
  login: (data: LoginRequest) => apiClient.post<ApiResponse<null>>('/api/v1/auth/login', data),
  logout: () => apiClient.post<ApiResponse<null>>('/api/v1/auth/logout'),
  me: () => apiClient.get<ApiResponse<User>>('/api/v1/auth/me').then((res) => res.data.data),
  withdraw: () => apiClient.post<ApiResponse<null>>('/api/v1/auth/withdraw'),
  updateNickname: (nickname: string) => apiClient.patch<ApiResponse<null>>('/api/v1/auth/me/nickname', { nickname }),
  updatePassword: (data: { currentPassword: string; newPassword: string }) => apiClient.patch<ApiResponse<null>>('/api/v1/auth/me/password', data),
};

export const categoryApi = {
  getAll: () => apiClient.get<ApiResponse<Category[]>>('/api/v1/categories').then((res) => res.data.data),
  create: (data: CreateCategoryRequest) => apiClient.post<ApiResponse<null>>('/api/v1/categories', data),
  update: (id: number, data: UpdateCategoryRequest) => apiClient.patch<ApiResponse<null>>(`/api/v1/categories/${id}`, data),
  delete: (id: number) => apiClient.delete<ApiResponse<null>>(`/api/v1/categories/${id}`),
};

// --- Poll & Vote API ---

export enum PollType {
  VOTE = 'VOTE',
  RANK = 'RANK',
}

export enum PollVisibility {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

export enum RankingRange {
  TODAY = 'TODAY',
  LAST_7_DAYS = 'LAST_7_DAYS',
  LAST_30_DAYS = 'LAST_30_DAYS',
}

export enum RankingTrack {
  ALL = 'ALL',
  TRUSTED = 'TRUSTED',
}

export interface PollOption {
  id: number;
  text: string;
  sortOrder: number;
  voteCount?: number;
}

export interface PollStats {
  totalVotes: number;
  viewCount: number;
  endsInSeconds: number;
  myVoteOptionIds?: number[];
  myVoted?: boolean;
}

export interface PollResults {
  // Binary results fields
  yesOptionId?: number;
  noOptionId?: number;
  yesCount?: number;
  noCount?: number;
  yesPercent?: number;
  noPercent?: number;
  
  // List results fields (for Ranking)
  pollId?: number;
  options?: {
    optionId: number;
    count: number;
    rank?: number;
  }[];
  items?: {
    optionId: number;
    label: string;
    count: number;
    percent: number;
    rank: number;
  }[];
}

export interface Poll {
  id: number;
  pollId?: number; // For compatibility with ranking responses
  creatorUserId?: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  pollType: PollType;
  visibility: PollVisibility;
  allowAnonymous: boolean;
  allowChange: boolean;
  maxSelections: number;
  endsAt: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string; // Added field
  tags: string[];
  options: PollOption[];
  stats: PollStats;
  preview?: {
    pollId: number;
    // Binary
    yesPercent?: number;
    noPercent?: number;
    // Ranking
    items?: {
      optionId: number;
      label: string;
      count: number;
      percent: number;
      rank: number;
    }[];
    etcPercent?: number | null;
    etcCount?: number;
    hasEtc?: boolean;
  };
  results?: PollResults;
}

export interface CreatePollRequest {
  title: string;
  description: string | null;
  categoryId: number | null;
  pollType: PollType;
  visibility: PollVisibility;
  allowAnonymous: boolean;
  allowChange: boolean;
  maxSelections: number;
  endsAt: string;
  options: string[];
}

export interface UpdatePollRequest {
  title?: string;
  description?: string;
  visibility?: PollVisibility;
  endsAt?: string;
}

export interface CreatePollResponse {
  pollId: number;
}

export interface VoteRequest {
  optionIds: number[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export const pollApi = {
  create: async (data: CreatePollRequest) => {
    const response = await apiClient.post<ApiResponse<CreatePollResponse>>('/api/v1/polls', data);
    return response.data;
  },
  getAll: async (params?: { categoryId?: number; type?: PollType; q?: string; page?: number; size?: number; sort?: string | string[] }) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Poll>>>(`/api/v1/polls`, { params });
    return response.data.data;
  },
  getOne: async (id: number) => {
    const response = await apiClient.get<ApiResponse<Poll>>(`/api/v1/polls/${id}`);
    return response.data.data;
  },
  update: async (id: number, data: UpdatePollRequest) => {
    const response = await apiClient.put<ApiResponse<null>>(`/api/v1/polls/${id}`, data);
    return response.data;
  },
  delete: async (id: number) => {
    const response = await apiClient.delete<ApiResponse<null>>(`/api/v1/polls/${id}`);
    return response.data;
  },
  vote: async (id: number, data: VoteRequest) => {
    const response = await apiClient.post<ApiResponse<null>>(`/api/v1/polls/${id}/votes`, data);
    return response.data;
  },
  getRankings: async (params: { range: RankingRange; track: RankingTrack; type?: PollType; page?: number; size?: number }) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Poll>>>(`/api/v1/polls/rankings`, { params });
    return response.data.data;
  }
};
