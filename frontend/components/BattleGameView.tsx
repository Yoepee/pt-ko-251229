import { BattleFinishedMessage, BattleStateMessage } from '@/hooks/useBattleWebSocket';
import { RoomDetail } from '@/lib/api';
import {
    Badge,
    Box,
    Button,
    Container,
    Grid,
    Group,
    Modal,
    Paper,
    Progress,
    SimpleGrid,
    Stack,
    Text,
    ThemeIcon,
    Title
} from '@mantine/core';
import {
    IconAlertTriangle,
    IconClick,
    IconClock,
    IconTrophy
} from '@tabler/icons-react';
import { useEffect, useState } from 'react';

interface BattleGameViewProps {
  room: RoomDetail;
  lastState: BattleStateMessage | null;
  finishedData: BattleFinishedMessage | null;
  sendInput: (lane: number) => void;
  onExit: () => void;
}

export const BattleGameView = ({ room, lastState, finishedData, sendInput, onExit }: BattleGameViewProps) => {
  const [timeLeft, setTimeLeft] = useState<number | null>(null);

  useEffect(() => {
    if (!lastState?.endsAtEpochMs) return;

    const updateTimer = () => {
      const now = new Date().getTime();
      const end = lastState.endsAtEpochMs;
      const diff = Math.max(0, Math.floor((end - now) / 1000));
      setTimeLeft(diff);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [lastState?.endsAtEpochMs]);

  const formatTime = (s: number) => {
    const mins = Math.floor(s / 60);
    const secs = s % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // UI state derived from lastState
  const totalScoreA = lastState?.scores.teamA || 0;
  const totalScoreB = lastState?.scores.teamB || 0;
  const inputsA = lastState?.inputs.teamA || 0;
  const inputsB = lastState?.inputs.teamB || 0;

  // Filter lanes based on mode
  const isOneLane = room.mode.includes('1LANE');
  const activeLanes = isOneLane ? [1] : [0, 1, 2];
  
  const getLaneScore = (idx: number) => {
    return lastState?.lanes[idx.toString()] || 0;
  };

  const getLaneLabel = (idx: number) => {
    if (isOneLane) return 'ARENA';
    if (idx === 0) return 'LEFT';
    if (idx === 1) return 'CENTER';
    if (idx === 2) return 'RIGHT';
    return `LANE ${idx}`;
  };

  return (
    <Container size="lg" py="xl">
      <Stack gap="xl">
        {/* Header: Timer & Mode */}
        <Paper p="md" radius="lg" bg="rgba(255,255,255,0.03)" className="border border-white/5">
          <Group justify="space-between">
            <Stack gap={0}>
              <Title order={3} c="white">{room.mode.replace(/_/g, ' ')}</Title>
              <Text size="xs" c="dimmed">MATCH #{room.matchId}</Text>
            </Stack>
            <Paper px="xl" py="xs" radius="xl" bg="violet.9" className="shadow-lg">
              <Group gap="xs">
                <IconClock size={20} stroke={2.5} />
                <Text fw={900} size="xl" ff="monospace">
                  {timeLeft !== null ? formatTime(timeLeft) : '--:--'}
                </Text>
              </Group>
            </Paper>
          </Group>
        </Paper>

        {/* Global Score Summary */}
        <SimpleGrid cols={2} spacing="md">
          <Paper p="md" radius="md" bg="blue.9" className="text-center shadow-lg border border-blue-400/20">
            <Text size="xs" fw={700} tt="uppercase" opacity={0.8}>TEAM ALPHA</Text>
            <Title order={1}>{totalScoreA}</Title>
            <Group justify="center" gap={4}>
              <IconClick size={12} />
              <Text size="xs" fw={700}>{inputsA}</Text>
            </Group>
          </Paper>
          <Paper p="md" radius="md" bg="red.9" className="text-center shadow-lg border border-red-400/20">
            <Text size="xs" fw={700} tt="uppercase" opacity={0.8}>TEAM BETA</Text>
            <Title order={1}>{totalScoreB}</Title>
            <Group justify="center" gap={4}>
              <IconClick size={12} />
              <Text size="xs" fw={700}>{inputsB}</Text>
            </Group>
          </Paper>
        </SimpleGrid>

        {/* Lanes Interactive Section */}
        <Stack gap="lg">
          {activeLanes.map((laneIdx) => {
            const score = getLaneScore(laneIdx);
            return (
              <Paper 
                key={laneIdx} 
                p="xl" 
                radius="xl" 
                bg="#1A1B1E" 
                className="border border-white/5 shadow-xl relative overflow-hidden"
              >
                <Box 
                  className="absolute inset-x-0 bottom-0 h-1 transition-all duration-300"
                  style={{ 
                    background: score > 0 ? 'var(--mantine-color-blue-6)' : score < 0 ? 'var(--mantine-color-red-6)' : 'transparent',
                    width: `${Math.min(100, Math.abs(score))}%`,
                    left: score >= 0 ? 0 : 'auto',
                    right: score < 0 ? 0 : 'auto'
                  }}
                />
                
                <Grid align="center" gutter="xl">
                  <Grid.Col span={{ base: 12, sm: 8 }}>
                    <Stack gap="xs">
                      <Group justify="space-between">
                        <Badge color="violet" variant="light" size="lg">{getLaneLabel(laneIdx)}</Badge>
                        <Group gap="xs">
                          <Text fw={900} size="xl" c={score > 0 ? "blue.4" : score < 0 ? "red.4" : "white"}>
                            {score > 0 ? `+${score}` : score}
                          </Text>
                          <Text size="sm" c="dimmed">PTS</Text>
                        </Group>
                      </Group>
                      <Progress 
                        value={Math.min(100, Math.abs(score))} 
                        size="xl" 
                        radius="xl" 
                        color={score >= 0 ? "blue" : "red"} 
                        animated={Math.abs(score) > 0}
                        className="bg-black/20"
                      />
                    </Stack>
                  </Grid.Col>
                  
                  <Grid.Col span={{ base: 12, sm: 4 }}>
                    <Button 
                      fullWidth 
                      size="xl" 
                      radius="md" 
                      color="violet"
                      variant="filled"
                      leftSection={<IconClick size={24} />}
                      onClick={() => !finishedData && sendInput(laneIdx)}
                      className="shadow-lg active:scale-95 transition-transform h-20"
                    >
                      ATTACK
                    </Button>
                  </Grid.Col>
                </Grid>
              </Paper>
            );
          })}
        </Stack>
      </Stack>

      {/* Result Modal */}
      <Modal
        opened={!!finishedData}
        onClose={() => {}} 
        withCloseButton={false}
        centered
        size="lg"
        radius="xl"
        padding={0}
      >
        <Box p={40} className="text-center relative">
          <Box 
            className="absolute inset-0 opacity-10 pointer-events-none" 
            style={{ 
              background: finishedData?.winner !== 'DRAW' ? (finishedData?.winner === 'A' ? 'radial-gradient(circle, blue 0%, transparent 70%)' : 'radial-gradient(circle, red 0%, transparent 70%)') : 'none'
            }} 
          />
          
          <Stack align="center" gap="xl">
            <ThemeIcon size={80} radius="xl" color={finishedData?.winner !== 'DRAW' ? "yellow" : "gray"} variant="filled">
              {finishedData?.winner !== 'DRAW' ? <IconTrophy size={48} /> : <IconAlertTriangle size={48} />}
            </ThemeIcon>
            
            <Stack gap="xs">
              <Title order={1} c="white">
                {finishedData?.winner === 'DRAW' ? 'MATCH DRAW' : `TEAM ${finishedData?.winner === 'A' ? 'ALPHA' : 'BETA'} WINS!`}
              </Title>
              <Text c="dimmed" fw={500}>
                {finishedData?.reason === 'TIMEUP' && 'Match ended due to time limit.'}
                {finishedData?.reason === 'FORFEIT' && 'Match ended by forfeit.'}
                {finishedData?.reason === 'EARLY_WIN' && 'Quick victory!'}
                {finishedData?.reason === 'CANCELED' && 'Match was canceled.'}
              </Text>
            </Stack>

            <Group gap="xl" mt="xl">
              <Box>
                <Text size="xs" c="blue.4" fw={700}>TEAM ALPHA</Text>
                <Text size="xl" fw={900}>{finishedData?.extra?.sumA || 0} pts</Text>
                <Text size="xs" c="dimmed">{finishedData?.inputsA || 0} inputs</Text>
              </Box>
              <Box className="w-[1px] h-10 bg-white/10" />
              <Box>
                <Text size="xs" c="red.4" fw={700}>TEAM BETA</Text>
                <Text size="xl" fw={900}>{finishedData?.extra?.sumB || 0} pts</Text>
                <Text size="xs" c="dimmed">{finishedData?.inputsB || 0} inputs</Text>
              </Box>
            </Group>

            <Button size="lg" variant="light" color="gray" mt="xl" radius="md" fullWidth onClick={onExit}>
              Return to Lobby
            </Button>
          </Stack>
        </Box>
      </Modal>
    </Container>
  );
};
