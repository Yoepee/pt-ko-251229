'use client';

import { ApiResponse, authApi } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useUserStore } from '@/store/userStore';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { setUser } = useUserStore();

  const { data: user, isLoading } = useQuery({
    queryKey: queryKeys.auth.me.queryKey,
    queryFn: authApi.me,
    retry: false,
    staleTime: 1000 * 60 * 5, // 5 mins
  });

  // Sync with Zustand
  useEffect(() => {
    if (user) {
      setUser(user);
    }
  }, [user, setUser]);

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.me.queryKey });
      notifications.show({
        title: 'Welcome back!',
        message: 'You have successfully logged in.',
        color: 'green',
      });
      router.push('/');
    },
    onError: (error: AxiosError<ApiResponse<null>>) => {
      notifications.show({
        title: 'Login Failed',
        message: error.response?.data?.message || 'Invalid username or password.',
        color: 'red',
      });
    },
  });

  const signupMutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: () => {
      notifications.show({
        title: 'Account Created',
        message: 'Registration successful! Please log in.',
        color: 'green',
      });
      router.push('/login');
    },
    onError: (error: AxiosError<ApiResponse<null>>) => {
      notifications.show({
        title: 'Registration Failed',
        message: error.response?.data?.message || 'Something went wrong during registration.',
        color: 'red',
      });
    },
  });

  const logoutMutation = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      setUser(null);
      queryClient.setQueryData(queryKeys.auth.me.queryKey, null);
      notifications.show({
        title: 'Goodbye',
        message: 'You have been logged out.',
        color: 'blue',
      });
      router.push('/login');
    },
  });

  return {
    user,
    isLoading,
    login: loginMutation.mutate,
    signup: signupMutation.mutate,
    logout: logoutMutation.mutate,
    isLoginPending: loginMutation.isPending,
    isSignupPending: signupMutation.isPending,
  };
}
