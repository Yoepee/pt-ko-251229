'use client';

import { VoteCard } from '@/components/VoteCard';
import { pollApi, PollType, RankingRange, RankingTrack } from '@/lib/api';
import { Container, Group, Loader, SegmentedControl, SimpleGrid, Stack, Tabs, Text } from '@mantine/core';
import { IconCircleCheck, IconCircleDashed, IconFlame, IconList } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useState } from 'react';

export default function RankingsPage() {
  const [range, setRange] = useState<string>(RankingRange.TODAY);
  const [activeTab, setActiveTab] = useState<string | null>('ALL');

  // Queries
  const { data: trendingData, isLoading: loadingTrending } = useQuery({
    queryKey: ['polls', 'rankings', range, activeTab],
    queryFn: () => pollApi.getRankings({ 
        range: range as RankingRange, 
        track: RankingTrack.ALL, 
        type: activeTab === 'ALL' ? undefined : activeTab as PollType,
        size: 12 
    }),
  });

  return (
    <div className="pb-20 pt-10">
      <Container size="xl">
        <Stack gap={0}>
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

            <div className="mt-4">
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
        </Stack>
      </Container>
    </div>
  );
}
