'use client';

import { VoteCard } from '@/components/VoteCard';
import { categoryApi, pollApi, PollType, RankingRange, RankingTrack } from '@/lib/api';
import { Button, Container, Group, Loader, ScrollArea, SegmentedControl, Select, SimpleGrid, Stack, Tabs, Text } from '@mantine/core';
import { IconCircleCheck, IconCircleDashed, IconFilter, IconFlame, IconList } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useState } from 'react';

export default function RankingsPage() {
  const [range, setRange] = useState<string>(RankingRange.TODAY);
  const [activeTab, setActiveTab] = useState<string | null>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [sortBy, setSortBy] = useState<string>('id,desc');

  // Queries
  const { data: categoriesData } = useQuery({
    queryKey: ['categories', 'all'],
    queryFn: categoryApi.getAll,
  });

  const { data: trendingData, isLoading: loadingTrending } = useQuery({
    queryKey: ['polls', 'rankings', range, activeTab, selectedCategory, sortBy],
    queryFn: () => pollApi.getRankings({ 
        range: range as RankingRange, 
        track: RankingTrack.ALL, 
        type: activeTab === 'ALL' ? undefined : activeTab as PollType,
        categoryId: selectedCategory || undefined,
        sort: sortBy === 'endsAt,asc' ? ['endsAt,asc', 'id,desc'] : [sortBy],
        size: 12 
    }),
  });

  return (
    <div className="pb-20 pt-10">
      <Container size="xl">
        <Stack gap="md">
            {/* Standardized Filter Bar (One Row) */}
            <Group justify="space-between" align="center" className="bg-[#1A1B1E]/40 p-3 px-6 rounded-2xl border border-white/5 backdrop-blur-sm">
                <Tabs value={activeTab} onChange={setActiveTab} variant="pills" color="violet" classNames={{
                    tab: "px-6 py-1.5 font-semibold transition-all text-xs md:text-sm",
                }}>
                    <Tabs.List>
                        <Tabs.Tab value="ALL" leftSection={<IconList size={14} />}>
                            전체
                        </Tabs.Tab>
                        <Tabs.Tab value={PollType.VOTE} leftSection={<IconCircleCheck size={14} />}>
                            찬반
                        </Tabs.Tab>
                        <Tabs.Tab value={PollType.RANK} leftSection={<IconCircleDashed size={14} />}>
                            순위
                        </Tabs.Tab>
                    </Tabs.List>
                </Tabs>
                
                <SegmentedControl 
                    value={range}
                    onChange={setRange}
                    data={[
                        { label: '오늘', value: RankingRange.TODAY },
                        { label: '최근 7일', value: RankingRange.LAST_7_DAYS },
                        { label: '최근 30일', value: RankingRange.LAST_30_DAYS }
                    ]}
                    radius="md"
                    bg="#0A0A0B"
                    color="violet"
                    size="sm"
                />
            </Group>

            {/* Area 2: Categories and Sort (Unified Row) */}
            <div className="bg-[#1A1B1E]/40 p-6 rounded-3xl border border-white/5 backdrop-blur-sm">
                <div className="flex flex-col md:flex-row items-end gap-10">
                    <div className="flex-1 min-w-0 w-full">
                        <Group justify="space-between" mb="xs">
                            <Group gap={6} pl={2}>
                                <IconFilter size={14} className="text-violet-400" />
                                <Text size="xs" fw={700} c="dimmed" tt="uppercase" lts="0.05em">카테고리</Text>
                            </Group>
                            {selectedCategory && (
                                <Button variant="subtle" size="compact-xs" color="gray" onClick={() => setSelectedCategory(null)} className="text-[10px]">초기화</Button>
                            )}
                        </Group>
                        <ScrollArea scrollbarSize={4}>
                            <Group gap="xs" wrap="nowrap" pb="sm">
                                <Button 
                                    variant={selectedCategory === null ? 'filled' : 'light'} 
                                    color={selectedCategory === null ? 'violet' : 'gray'}
                                    radius="xl"
                                    size="compact-sm"
                                    onClick={() => setSelectedCategory(null)}
                                >
                                    전체
                                </Button>
                                {categoriesData?.map((cat) => (
                                    <Button 
                                        key={cat.id}
                                        variant={selectedCategory === cat.id ? 'filled' : 'light'} 
                                        color={selectedCategory === cat.id ? 'violet' : 'gray'}
                                        radius="xl"
                                        size="compact-sm"
                                        onClick={() => setSelectedCategory(cat.id)}
                                    >
                                        {cat.name}
                                    </Button>
                                ))}
                            </Group>
                        </ScrollArea>
                    </div>

                    <div className="w-full md:w-[200px] pb-1">
                        <Text size="xs" fw={700} c="dimmed" mb={5} pl={4} tt="uppercase" lts="0.05em">정렬 기준</Text>
                        <Select
                            value={sortBy}
                            onChange={(val: string | null) => setSortBy(val || 'id,desc')}
                            data={[
                                { label: '최신순', value: 'id,desc' },
                                { label: '마감 임박순', value: 'endsAt,asc' },
                                { label: '누적 투표순', value: 'popular,desc' }
                            ]}
                            radius="md"
                            variant="filled"
                            comboboxProps={{ transitionProps: { transition: 'pop-top-right', duration: 200 } }}
                            classNames={{ input: 'bg-[#0A0A0B] border-white/5' }}
                        />
                    </div>
                </div>

                {/* Content Area */}
                <div className="mt-8">
                {loadingTrending ? (
                    <div className="flex justify-center py-40"><Loader color="violet" type="bars" size="lg" /></div>
                ) : (
                    <>
                        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="xl">
                            {(trendingData?.content || []).map((poll, index) => poll && (
                                <Link href={`/vote/${poll.id || poll.pollId || index}`} key={`rank-${poll.id || poll.pollId || index}`} className="no-underline">
                                    <VoteCard poll={poll} />
                                </Link>
                            ))}
                        </SimpleGrid>
                        
                        {trendingData?.content?.length === 0 && (
                            <div className="py-40 text-center bg-[#1A1B1E]/30 rounded-3xl border border-white/5 border-dashed">
                                 <IconFlame size={48} className="text-gray-700 mx-auto mb-4 opacity-20" />
                                 <Text c="dimmed" size="lg">해당 조건에 맞는 데이터가 없습니다.</Text>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
        </Stack>
      </Container>
    </div>
  );
}
