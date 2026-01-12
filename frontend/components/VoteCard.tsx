import { Poll, PollType } from '@/lib/api';
import { Badge, Card, Group, Progress, Stack, Text, ThemeIcon } from '@mantine/core';
import { IconChecks, IconClock } from '@tabler/icons-react';

interface VoteCardProps {
  poll: Poll;
}

export function VoteCard({ poll }: VoteCardProps) {
  const isBinary = poll.pollType === PollType.VOTE;
  
  // Calculate time left helper
  const getTimeLeft = (seconds: number) => {
    if (seconds <= 0) return '종료됨';
    const days = Math.floor(seconds / (3600 * 24));
    if (days > 0) return `${days}일 남음`;
    const hours = Math.floor(seconds / 3600);
    if (hours > 0) return `${hours}시간 남음`;
    const mins = Math.floor(seconds / 60);
    return `${mins}분 남음`;
  };

  return (
    <Card className="group hover:scale-[1.02] transition-transform duration-300 border-white/5 bg-[#1A1B1E]/60 hover:bg-[#1A1B1E]/80 h-full flex flex-col">
      {/* Header */}
      <Group justify="space-between" mb="xs">
        <Badge 
          variant="gradient" 
          gradient={isBinary ? { from: 'blue', to: 'cyan' } : { from: 'violet', to: 'grape' }}
        >
          {isBinary ? '찬반 투표' : '순위 선정'}
        </Badge>
        <Group gap={6}>
            <ThemeIcon size="xs" variant="transparent" color="gray">
                <IconClock size={12} />
            </ThemeIcon>
            <Text size="xs" c="dimmed">{getTimeLeft(poll.stats?.endsInSeconds ?? 0)}</Text>
        </Group>
      </Group>

      {/* Content */}
      <Text fw={700} size="lg" c="white" mb="md" lineClamp={1} className="group-hover:text-violet-300 transition-colors">
        {poll.title}
      </Text>

      {/* Visualization */}
      <div className="mb-6 flex-1 min-h-[100px] flex flex-col justify-center">
        {isBinary && (poll.preview || poll.results) && (
            <Stack gap="xs">
                {/* Option 1 (Yes/Left) */}
                <div>
                    <Group justify="space-between" mb={2}>
                        <Text size="xs" fw={500} c="gray.3">찬성</Text>
                        <Text size="xs" fw={700} c="white">{poll.stats.totalVotes > 0 ? (poll.results?.yesPercent ?? poll.preview?.yesPercent ?? 0) : 0}%</Text>
                    </Group>
                    <Progress 
                        value={poll.stats.totalVotes > 0 ? (poll.results?.yesPercent ?? poll.preview?.yesPercent ?? 0) : 0} 
                        color="blue" 
                        size="sm" 
                        radius="xl" 
                        bg="rgba(255,255,255,0.05)"
                    />
                </div>
                {/* Option 2 (No/Right) */}
                 <div>
                    <Group justify="space-between" mb={2}>
                        <Text size="xs" fw={500} c="gray.3">반대</Text>
                        <Text size="xs" fw={700} c="white">{poll.stats.totalVotes > 0 ? (poll.results?.noPercent ?? poll.preview?.noPercent ?? 0) : 0}%</Text>
                    </Group>
                    <Progress 
                        value={poll.stats.totalVotes > 0 ? (poll.results?.noPercent ?? poll.preview?.noPercent ?? 0) : 0} 
                        color="cyan" 
                        size="sm" 
                        radius="xl" 
                        bg="rgba(255,255,255,0.05)"
                    />
                </div>
            </Stack>
        )}

        {/* For Ranking, show top items from preview */}
        {!isBinary && poll.preview?.items && (
            <Stack gap="xs">
                {poll.preview.items.slice(0, 5).map((item) => (
                    <div key={item.optionId}>
                        <Group justify="space-between" mb={2}>
                            <Text size="xs" fw={500} c="gray.3" truncate className="flex-1 min-w-0">{item.label}</Text>
                            <Text size="xs" fw={700} c="white" ml="xs">{Math.round(item.percent)}%</Text>
                        </Group>
                        <Progress 
                            value={item.percent} 
                            color="violet" 
                            size="xs" 
                            radius="xl" 
                            bg="rgba(255,255,255,0.05)"
                        />
                    </div>
                ))}
                {poll.preview.hasEtc && (
                    <div key="etc">
                        <Group justify="space-between" mb={2}>
                            <Text size="xs" fw={500} c="gray.5" truncate className="flex-1 min-w-0">기타</Text>
                            <Text size="xs" fw={700} c="white" ml="xs">{Math.round(poll.preview.etcPercent ?? 0)}%</Text>
                        </Group>
                        <Progress 
                            value={poll.preview.etcPercent ?? 0} 
                            color="gray" 
                            size="xs" 
                            radius="xl" 
                            bg="rgba(255,255,255,0.05)"
                        />
                    </div>
                )}
            </Stack>
        )}

        {!isBinary && !poll.preview?.items && (
            <div className="flex items-center justify-center h-full bg-white/5 rounded-lg border border-white/5 border-dashed">
                <Text size="xs" c="dimmed">아직 데이터가 없습니다</Text>
            </div>
        )}
      </div>

      {/* Footer */}
      <Group justify="space-between" mt="auto" pt="md" className="border-t border-white/5">
        <Group gap={6}>
            <IconChecks size={16} className="text-gray-500" />
            <Text size="xs" c="dimmed">{poll.stats.totalVotes.toLocaleString()}명 참여</Text>
        </Group>
      </Group>
    </Card>
  );
}
