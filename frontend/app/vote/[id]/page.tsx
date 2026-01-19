'use client';

import { useAuth } from '@/hooks/useAuth';
import { ApiResponse, pollApi, PollType } from '@/lib/api';
import { pollKeys } from '@/lib/queryKeys';
import {
    Avatar,
    Badge,
    Button,
    Card,
    Container,
    Group,
    Loader,
    Paper,
    Progress,
    Stack,
    Text,
    ThemeIcon,
    Title
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconArrowLeft, IconCheck, IconClock, IconRotate, IconTrophy } from '@tabler/icons-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useState } from 'react';

dayjs.extend(relativeTime);

export default function VoteDetailPage() {
  const { id } = useParams<{ id: string }>();
  // Safe ID parsing
  const pollId = Number(id);
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  // Queries
  const { data: poll, isLoading, error } = useQuery({
    queryKey: [...pollKeys.detail(pollId).queryKey],
    queryFn: () => pollApi.getOne(pollId),
    enabled: !isNaN(pollId),
  });

  const [selectedOptionIds, setSelectedOptionIds] = useState<number[]>([]);

  // Mutations
  const voteMutation = useMutation({
    mutationFn: (optionIds: number[]) => pollApi.vote(pollId, { optionIds }),
    onSuccess: () => {
      notifications.show({ title: '투표 완료!', message: '당신의 의견이 성공적으로 반영되었습니다.', color: 'green' });
      queryClient.invalidateQueries({ queryKey: pollKeys.detail(pollId).queryKey });
      queryClient.invalidateQueries({ queryKey: pollKeys.all.queryKey });
      setSelectedOptionIds([]);
    },
    onError: (err: AxiosError<ApiResponse<null>>) => {
      notifications.show({ 
        title: '투표 실패', 
        message: (err.response?.data as ApiResponse<null>)?.message || '투표를 처리할 수 없습니다.', 
        color: 'red' 
      });
    },
  });

  if (isLoading) {
    return (
      <Container size="md" py={100} className="flex justify-center">
        <Loader type="bars" color="violet" size="lg" />
      </Container>
    );
  }

  if (error || !poll) {
    return (
      <Container size="md" py={100} className="text-center">
        <Title order={2} mb="md">투표를 찾을 수 없습니다</Title>
        <Text c="dimmed" mb="xl">찾으시는 투표가 존재하지 않거나 삭제되었습니다.</Text>
        <Link href="/">
          <Button variant="subtle" leftSection={<IconArrowLeft size={16}/>}>대시보드로 돌아가기</Button>
        </Link>
      </Container>
    );
  }

  const isBinary = poll.pollType === PollType.VOTE;
  const isEnded = dayjs(poll.endsAt).isBefore(dayjs());
  const totalVotes = poll.stats.totalVotes;
  const maxSelections = poll.maxSelections || 1;
  const myVotedOptionIds = poll.stats.myVoteOptionIds || [];
  const hasVoted = poll.stats.myVoted || myVotedOptionIds.length > 0;
  const canChange = poll.allowChange;
  const isSingleSelect = isBinary || maxSelections === 1;

  const handleToggleOption = (optionId: number) => {
    if (!user && !poll.allowAnonymous) {
      notifications.show({ title: '로그인이 필요합니다', message: '이 투표에 참여하려면 로그인이 필요합니다.', color: 'yellow' });
      router.push('/login');
      return;
    }

    if (isEnded) return;
    if (hasVoted && !canChange) {
        notifications.show({ title: '변경 불가', message: '이미 투표에 참여하셨으며, 투표 변경이 허용되지 않는 투표입니다.', color: 'gray' });
        return;
    }

    if (isSingleSelect) {
        voteMutation.mutate([optionId]);
        return;
    }

    setSelectedOptionIds((prev: number[]) => {
      if (prev.includes(optionId)) {
        return prev.filter((id: number) => id !== optionId);
      }
      
      if (prev.length >= maxSelections) {
        notifications.show({ 
            title: '선택 제한', 
            message: `최대 ${maxSelections}개까지만 선택할 수 있습니다.`, 
            color: 'orange' 
        });
        return prev;
      }
      
      return [...prev, optionId];
    });
  };

  const handleSubmitVote = () => {
    if (selectedOptionIds.length === 0) {
        notifications.show({ message: '최소 한 개의 항목을 선택해주세요.', color: 'orange' });
        return;
    }
    voteMutation.mutate(selectedOptionIds);
  };

  return (
    <div className="pb-20">
      {/* Header Background */}
      <div className="h-[200px] bg-gradient-to-b from-violet-900/20 to-transparent absolute top-0 left-0 right-0 -z-10" />

      <Container size="md" pt={40}>
        <Link href="/" className="inline-block mb-6">
          <Button variant="subtle" color="gray" size="sm" leftSection={<IconArrowLeft size={16}/>}>
            뒤로가기
          </Button>
        </Link>

        {/* Poll Header */}
        <div className="mb-10 text-center relative">
          <Badge 
            size="lg" 
            variant="gradient" 
            gradient={isBinary ? { from: 'blue', to: 'cyan' } : { from: 'violet', to: 'grape' }}
            mb="md"
          >
            {isBinary ? '찬반 투표' : (maxSelections > 1 ? `다중 선택 (${maxSelections})` : '순위 선정')}
          </Badge>
          
          <Title className="text-4xl md:text-5xl font-extrabold text-white mb-6 leading-tight">
            {poll.title}
          </Title>
          
          {poll.description && (
             <Text size="lg" c="gray.4" maw={700} mx="auto" mb="lg">
                {poll.description}
             </Text>
          )}

          <Group justify="center" gap="lg" mt="lg">
             <Group gap={6}>
                <Avatar size="sm" radius="xl" src={null} color="violet">{poll.createdBy?.charAt(0) || '?'}</Avatar>
                <Text size="sm" c="gray.3">작성자: <span className="font-semibold text-white">{poll.createdBy || '익명'}</span></Text>
             </Group>
             <Group gap={6}>
                <IconClock size={16} className="text-gray.5" />
                <Text size="sm" c="dimmed">
                    {isEnded ? '종료됨' : `${dayjs(poll.endsAt).fromNow()} 종료`}
                </Text>
             </Group>
             <Group gap={6}>
                <IconCheck size={16} className="text-gray.5" />
                <Text size="sm" c="dimmed">{totalVotes.toLocaleString()}명 참여</Text>
             </Group>
             {hasVoted && (
                 <Badge color="green" variant="light" size="sm" leftSection={<IconCheck size={12}/>}>투표 참여 완료</Badge>
             )}
          </Group>
        </div>

        {/* Binary Vote Layout */}
        {isBinary && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12 relative">
            {/* VS Badge */}
            <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-10 hidden md:flex bg-[#0A0A0B] rounded-full p-2 border border-white/10 shadow-xl">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-gray-800 to-black flex items-center justify-center font-black text-xl italic text-gray-500">
                    VS
                </div>
            </div>

            {poll.options.map((option, index) => {
               const isLeft = index === 0;
                const percent = totalVotes > 0 ? (isLeft ? (poll.results?.yesPercent ?? 0) : (poll.results?.noPercent ?? 0)) : 0;
                const count = isLeft ? (poll.results?.yesCount ?? 0) : (poll.results?.noCount ?? 0);
               const color = isLeft ? 'blue' : 'cyan';
               const isSelected = selectedOptionIds.includes(option.id);
               const isVoted = myVotedOptionIds.includes(option.id);
               
               return (
                 <Card 
                    key={option.id} 
                    padding="xl" 
                    radius="lg" 
                    className={`
                        group relative overflow-hidden transition-all duration-300 border-2 bg-[#1A1B1E]
                        ${!isEnded && (!hasVoted || canChange) ? 'cursor-pointer' : 'cursor-default'}
                        ${isSelected 
                            ? (isLeft ? 'border-blue-500 shadow-[0_0_30px_-10px_var(--mantine-color-blue-9)]' : 'border-cyan-500 shadow-[0_0_30px_-10px_var(--mantine-color-cyan-9)]') 
                            : 'border-white/10 hover:border-white/20'}
                    `}
                    onClick={() => handleToggleOption(option.id)}
                 >
                    {/* Background Progress Fill */}
                    <div 
                        className={`absolute top-0 bottom-0 ${isLeft ? 'left-0 bg-blue-500' : 'right-0 bg-cyan-500'} opacity-10 transition-all duration-1000 ease-out`}
                        style={{ width: `${percent}%` }}
                    />
                    
                    <Stack align="center" gap="md" className="relative z-10 py-6">
                        <Group justify="center" gap="xs">
                            <Text size="xl" fw={700} ta="center" className="uppercase tracking-wide">
                                {option.text}
                            </Text>
                            {(isSelected || isVoted) && <IconCheck size={24} className={isLeft ? 'text-blue-500' : 'text-cyan-500'} />}
                        </Group>

                        {isVoted && (
                             <Badge size="xs" color={color} variant="filled">내 투표</Badge>
                        )}
                        
                        <div className="text-4xl font-black text-white">
                            {Math.round(percent)}%
                        </div>
                        
                        <Text c="dimmed" size="sm" mb="md">{count.toLocaleString()} 표</Text>

                        {!isEnded && (!hasVoted || canChange) && (
                            <Text size="xs" fw={700} c={color} className="uppercase tracking-widest opacity-0 group-hover:opacity-100 transition-opacity">
                                {isSelected ? '선택 취소' : '선택하기'}
                            </Text>
                        )}
                    </Stack>
                 </Card>
               );
            })}
          </div>
        )}

        {/* Ranking Vote Layout */}
        {!isBinary && (
            <Stack gap="md" mb="xl">
                {poll.options
                    .map(opt => {
                        const resFromItems = poll.results?.items?.find(r => r.optionId === opt.id);
                        const resFromOptions = poll.results?.options?.find(r => r.optionId === opt.id);
                        
                        const count = resFromItems?.count ?? resFromOptions?.count ?? opt.voteCount ?? 0;
                        const percent = resFromItems ? resFromItems.percent : (totalVotes > 0 ? (count / totalVotes) * 100 : 0);
                        const rank = resFromItems?.rank ?? resFromOptions?.rank;
                        const isSelected = selectedOptionIds.includes(opt.id);
                        const isVoted = myVotedOptionIds.includes(opt.id);
                        
                        return { ...opt, count, percent, rank, isSelected, isVoted };
                    })
                    .sort((a, b) => (b.count || 0) - (a.count || 0))
                    .map((item, index) => {
                    const rank = item.rank || index + 1;
                    const isTop3 = rank <= 3;

                    return (
                        <Paper 
                            key={item.id}
                            p="md" 
                            radius="md" 
                            bg="#1A1B1E" 
                            onClick={() => handleToggleOption(item.id)}
                            className={`
                                border transition-all relative overflow-hidden group 
                                ${!isEnded && (!hasVoted || canChange) ? 'cursor-pointer' : 'cursor-default'}
                                ${item.isSelected ? 'border-violet-500 bg-violet-500/5' : 'border-white/5 hover:border-white/10'}
                            `}
                        >
                            {/* Progress bar background */}
                            <div 
                                className="absolute bottom-0 left-0 top-0 bg-violet-500/5 transition-all duration-1000"
                                style={{ width: `${item.percent}%` }}
                            />

                            <Group wrap="nowrap" align="center" className="relative z-10">
                                <div className={`
                                    w-10 h-10 shrink-0 rounded-lg flex items-center justify-center font-bold text-lg
                                    ${rank === 1 ? 'bg-yellow-500/20 text-yellow-400' : 
                                      rank === 2 ? 'bg-gray-400/20 text-gray-300' :
                                      rank === 3 ? 'bg-orange-700/20 text-orange-400' : 
                                      'bg-gray-800 text-gray-500'}
                                `}>
                                    {isTop3 ? <IconTrophy size={20} /> : rank}
                                </div>

                                <div className="flex-1 min-w-0">
                                    <Group gap="xs" wrap="nowrap">
                                        <Text fw={600} size="lg" truncate>{item.text}</Text>
                                        {item.isVoted && (
                                            <Badge size="xs" color="violet" variant="filled">내 투표</Badge>
                                        )}
                                    </Group>
                                    <Group gap="xs">
                                        <Progress value={item.percent} size="sm" radius="xl" color="violet" className="flex-1 max-w-[100px]" />
                                        <Text size="xs" c="dimmed">{item.count.toLocaleString()} 표 ({Math.round(item.percent)}%)</Text>
                                    </Group>
                                </div>

                                {item.isSelected ? (
                                    <ThemeIcon variant="filled" color="violet" radius="xl">
                                        <IconCheck size={16} />
                                    </ThemeIcon>
                                ) : (
                                    !isEnded && (!hasVoted || canChange) && (
                                        <div className="w-6 h-6 rounded-full border-2 border-white/10 group-hover:border-violet-500/50 transition-colors" />
                                    )
                                )}
                            </Group>
                        </Paper>
                    );
                })}
            </Stack>
        )}

        {/* Floating Submit Button (Multi-select only) */}
        {!isEnded && !isSingleSelect && (
            <div className="sticky bottom-8 left-0 right-0 z-50 flex justify-center px-4">
                {hasVoted && !canChange ? (
                    <Button
                        size="xl"
                        radius="xl"
                        color="gray"
                        variant="light"
                        className="shadow-2xl px-12 opacity-80"
                        disabled
                        leftSection={<IconCheck size={20} />}
                    >
                        투표 완료 (변경 불가)
                    </Button>
                ) : (
                    <Button
                        size="xl"
                        radius="xl"
                        className="shadow-2xl px-12 bg-gradient-to-r from-violet-600 to-indigo-600 hover:scale-105 transition-transform"
                        disabled={selectedOptionIds.length === 0 || voteMutation.isPending}
                        loading={voteMutation.isPending}
                        onClick={handleSubmitVote}
                        leftSection={<IconRotate size={20} />}
                    >
                        {selectedOptionIds.length > 0 
                            ? `${selectedOptionIds.length}개 항목으로 ${hasVoted ? '변경하기' : '투표하기'}`
                            : (hasVoted ? '변경할 항목을 선택해주세요' : (maxSelections > 1 ? '항목을 선택해주세요' : '투표하기'))}
                    </Button>
                )}
            </div>
        )}

        {/* Metadata Footer */}
        <Group justify="center" mt={40} pb={40}>
            {poll.tags?.map(tag => (
                <Badge key={tag} variant="dot" color="gray" size="lg">#{tag}</Badge>
            ))}
        </Group>

      </Container>
    </div>
  );
}
