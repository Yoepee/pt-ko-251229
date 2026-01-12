'use client';

import { useAuth } from '@/hooks/useAuth';
import { Avatar, Button, Container, Group, Text } from '@mantine/core';
import {
    IconChartBar,
    IconHome,
    IconLayoutDashboard,
    IconLock,
    IconPlus,
    IconSettings,
    IconTrophy,
    IconUser,
    IconUserPlus
} from '@tabler/icons-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ReactNode } from 'react';

export function Header() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const getPageInfo = (): { title: string; icon: ReactNode } => {
    if (pathname === '/') return { title: '홈', icon: <IconHome size={20} className="text-violet-400" /> };
    if (pathname === '/rankings') return { title: '실시간 인기 랭킹', icon: <IconTrophy size={20} className="text-yellow-400" /> };
    if (pathname === '/vote/create') return { title: '투표 만들기', icon: <IconPlus size={20} className="text-blue-400" /> };
    if (pathname?.startsWith('/vote/')) return { title: '투표 상세', icon: <IconChartBar size={20} className="text-cyan-400" /> };
    if (pathname === '/category/manage') return { title: '카테고리 설정', icon: <IconSettings size={20} className="text-gray-400" /> };
    if (pathname === '/profile') return { title: '내 프로필', icon: <IconUser size={20} className="text-violet-400" /> };
    if (pathname === '/login') return { title: '로그인', icon: <IconLock size={20} className="text-gray-400" /> };
    if (pathname === '/register') return { title: '회원가입', icon: <IconUserPlus size={20} className="text-violet-400" /> };
    return { title: '대시보드', icon: <IconLayoutDashboard size={20} className="text-gray-400" /> };
  };

  const { title, icon } = getPageInfo();

  return (
    <header className="fixed top-0 left-[240px] right-0 z-30 h-[60px] border-b border-white/5 bg-[#0A0A0B]/80 backdrop-blur-md transition-all">
      <Container fluid className="h-full flex items-center justify-between px-6">
        {/* Left: Dynamic Page Title with Icon */}
        <Group gap="xs">
            {icon}
            <Text fw={700} size="lg" c="white">{title}</Text>
        </Group>

        {/* Right: User Actions */}
        <Group gap="md">
            {user ? (
                <Group gap="sm">
                    <Link href="/profile" className="no-underline">
                        <Group gap="xs" className="px-3 py-1.5 rounded-full hover:bg-white/5 transition-colors border border-transparent hover:border-white/10 cursor-pointer">
                            <Avatar src={user.avatarUrl} size="sm" color="violet" radius="xl">
                                {user.nickname?.[0]}
                            </Avatar>
                            <Text size="sm" fw={500} c="gray.3" visibleFrom="xs">{user.nickname}</Text>
                        </Group>
                    </Link>
                    <Button 
                        variant="subtle" 
                        color="red" 
                        size="xs" 
                        onClick={() => logout()}
                        className="hover:bg-red-500/10"
                    >
                        로그아웃
                    </Button>
                </Group>
            ) : (
                <Group gap="xs">
                    <Link href="/login">
                        <Button variant="subtle" color="gray" size="xs">로그인</Button>
                    </Link>
                    <Link href="/register">
                        <Button variant="filled" color="violet" size="xs" radius="xl">회원가입</Button>
                    </Link>
                </Group>
            )}
        </Group>
      </Container>
    </header>
  );
}
