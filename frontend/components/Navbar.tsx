'use client';

import { categoryApi } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { Code, Group, NavLink, ScrollArea, Text, UnstyledButton } from '@mantine/core';
import { IconFlame, IconHome, IconPlus, IconSettings, IconTrophy } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export function Navbar() {
  const pathname = usePathname();
  
  const { data: categories } = useQuery({
    queryKey: queryKeys.categories.all.queryKey,
    queryFn: categoryApi.getAll,
  });

  const isActive = (path: string) => pathname === path;

  return (
    <nav className="fixed top-0 left-0 w-[240px] h-full border-r border-white/5 bg-[#0A0A0B] flex flex-col z-40">
       {/* Logo Area */}
       <div className="h-[60px] flex items-center px-5 border-b border-white/5">
            <Link href="/" className="no-underline flex items-center gap-2 group">
                <div className="w-7 h-7 rounded-md bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center text-white font-bold shadow-lg group-hover:scale-105 transition-transform">
                    V
                </div>
                <Text fw={700} size="lg" c="white">
                    Vote<span className="text-violet-400">Board</span>
                </Text>
            </Link>
       </div>

       {/* 메인 네비게이션 */}
       <ScrollArea className="flex-1">
            <div className="p-3 space-y-1">
                <NavLink
                    component={Link}
                    href="/"
                    label="홈"
                    leftSection={<IconHome size={18} />}
                    active={isActive('/')}
                    variant="light"
                    color="violet"
                    className="rounded-md font-medium"
                />
                <NavLink
                    component={Link}
                    href="/rankings"
                    label="인기 랭킹"
                    leftSection={<IconTrophy size={18} />}
                    active={isActive('/rankings')}
                    variant="light"
                    color="violet"
                    className="rounded-md font-medium"
                />
                 <NavLink
                    component={Link}
                    href="/battles"
                    label="투표 배틀"
                    leftSection={<IconFlame size={18} />}
                    active={isActive('/battles')}
                    variant="light"
                    color="violet"
                    className="rounded-md font-medium"
                />
            </div>

            <div className="p-3 mt-2 space-y-1">
                 <Text size="xs" fw={700} c="dimmed" tt="uppercase" px={12} mb={4}>설정</Text>
                 <NavLink
                    component={Link}
                    href="/category/manage"
                    label="카테고리 설정"
                    leftSection={<IconSettings size={18} />}
                    active={isActive('/category/manage')}
                    variant="light"
                    color="gray"
                    className="rounded-md font-medium"
                />
            </div>
       </ScrollArea>

       {/* 하단 액션 */}
       <div className="p-3 border-t border-white/5">
            <Link href="/vote/create" className="no-underline">
                <UnstyledButton className="w-full bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white p-3 rounded-lg shadow-lg flex items-center justify-center gap-2 transition-all">
                    <IconPlus size={18} />
                    <Text fw={600} size="sm">투표 만들기</Text>
                </UnstyledButton>
            </Link>
            
            <Group justify="space-between" mt="md" px={2}>
                 <Code className="bg-transparent text-xs text-gray-600 font-medium">v1.2.0</Code>
            </Group>
       </div>
    </nav>
  );
}
