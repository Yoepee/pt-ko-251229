'use client';

import { useAuth } from '@/hooks/useAuth';
import {
    AutoMatchRequest,
    battleApi,
    BattleMatchType,
    BattleMode,
    CreateRoomRequest
} from '@/lib/api';
import { battleKeys } from '@/lib/queryKeys';
import {
    ActionIcon,
    Badge,
    Box,
    Button,
    Card,
    Center,
    Container,
    Divider,
    Grid,
    Group,
    Loader,
    Modal,
    Pagination,
    Paper,
    Select,
    SimpleGrid,
    Stack,
    Text,
    ThemeIcon,
    Title
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
    IconCheck,
    IconCircle,
    IconPlus,
    IconRotate,
    IconSquare,
    IconSwords,
    IconTriangle,
    IconTrophy,
    IconUser,
    IconUsers
} from '@tabler/icons-react';
import {
    useMutation,
    useQuery,
    useQueryClient
} from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

export default function BattleLobbyPage() {
  const router = useRouter();
  const { user, isLoading: isUserLoading } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [selectedCharId, setSelectedCharId] = useState<number | null>(null);
  const [isCharModalOpen, setIsCharModalOpen] = useState(false);
  const [isRoomModalOpen, setIsRoomModalOpen] = useState(false);
  const [isAutoMatchModalOpen, setIsAutoMatchModalOpen] = useState(false);
  const [pendingJoinRoomId, setPendingJoinRoomId] = useState<number | null>(null);

  // Authentication Check
  useEffect(() => {
    if (!isUserLoading && !user) {
      notifications.show({
        title: '로그인 필요',
        message: '투표 배틀에 참여하시려면 먼저 로그인이 필요합니다.',
        color: 'red',
      });
      router.push('/login');
    }
  }, [user, isUserLoading, router]);

  // Room Creation Form State
  const [createForm, setCreateForm] = useState({
    mode: BattleMode.SOLO_1LANE_RUSH,
  });

  const [autoMatchForm, setAutoMatchForm] = useState<AutoMatchRequest>({
    matchType: BattleMatchType.RANKED,
    mode: BattleMode.SOLO_1LANE_RUSH,
    characterId: null,
  });

  // SSE for Real-time Room Updates
  useEffect(() => {
    if (!user) return;

    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
    const sseUrl = `${baseUrl}/api/v1/battles/lobby/events`;
    
    // Using native EventSource (assumes no custom headers needed for this GET stream, 
    // or handled by cookies if withCredentials-like behavior is default for SSE)
    const eventSource = new EventSource(sseUrl, { withCredentials: true });

    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.type === 'LOBBY_SNAPSHOT') {
                // We could directly set data into cache, but simple invalidation is safer for pagination consistency
                queryClient.invalidateQueries({ queryKey: battleKeys.rooms({ page: page - 1, size: 6 }).queryKey });
            }
        } catch (err) {
            console.error('SSE Parse Error:', err);
        }
    };

    eventSource.onerror = (err) => {
        console.error('SSE Connection Error:', err);
        eventSource.close();
    };

    return () => {
        eventSource.close();
    };
  }, [user, page, queryClient]);

  // Load selected character from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('selected_character_id');
    if (saved) {
      setSelectedCharId(parseInt(saved));
    }
  }, []);

  const handleCharSelect = (id: number) => {
    setSelectedCharId(id);
    localStorage.setItem('selected_character_id', id.toString());
    setIsCharModalOpen(false);
    notifications.show({
      title: '캐릭터 선택 완료',
      message: '전투 준비가 되었습니다!',
      color: 'violet',
      icon: <IconCheck size={16} />,
    });

    if (pendingJoinRoomId) {
        joinRoomMutation.mutate({ roomId: pendingJoinRoomId, characterId: id });
        setPendingJoinRoomId(null);
    }
  };

  const { data: myState, isLoading: isStateLoading } = useQuery({
    queryKey: battleKeys.state.queryKey,
    queryFn: battleApi.getMyState,
    enabled: !!user,
  });

  // Auto Redirection if already in a match
  useEffect(() => {
    if (myState && myState.matchId && myState.state !== 'IDLE') {
        router.push(`/battles/room/${myState.matchId}`);
    }
  }, [myState, router]);

  const { data: stats } = useQuery({
    queryKey: battleKeys.stats.queryKey,
    queryFn: battleApi.getMyStats,
    enabled: !!user,
  });

  const { data: characters } = useQuery({
    queryKey: battleKeys.characters.queryKey,
    queryFn: battleApi.getCharacters,
    enabled: !!user,
  });

  const { data: roomsData, isLoading: isRoomsLoading } = useQuery({
    queryKey: battleKeys.rooms({ page: page - 1, size: 6 }).queryKey,
    queryFn: () => battleApi.getRooms({ page: page - 1, size: 6 }),
    enabled: !!user,
  });

  const createRoomMutation = useMutation({
    mutationFn: (data: CreateRoomRequest) => battleApi.createRoom(data),
    onSuccess: (res) => {
        notifications.show({ title: '방 생성 완료', message: '전투 대기실로 이동합니다.', color: 'green' });
        setIsRoomModalOpen(false);
        router.push(`/battles/room/${res.matchId}`);
    },
    onError: () => {
        notifications.show({ title: '생성 실패', message: '방을 만들 수 없습니다.', color: 'red' });
    }
  });

  const joinRoomMutation = useMutation({
    mutationFn: ({ roomId, characterId }: { roomId: number, characterId: number }) => battleApi.joinRoom(roomId, characterId),
    onSuccess: (_, variables) => {
        notifications.show({ title: '입장 완료', message: '대기실로 이동합니다.', color: 'green' });
        queryClient.invalidateQueries({ queryKey: battleKeys.state.queryKey });
        router.push(`/battles/room/${variables.roomId}`);
    },
    onError: () => {
        notifications.show({ title: '입장 실패', message: '인원이 가득 찼거나 이미 진행 중인 게임입니다.', color: 'red' });
    }
  });

  const autoMatchMutation = useMutation({
    mutationFn: (data: AutoMatchRequest) => battleApi.autoMatch(data),
    onSuccess: (res) => {
        notifications.show({ title: '매칭 성공!', message: '전투 대기실로 이동합니다.', color: 'green' });
        setIsAutoMatchModalOpen(false);
        queryClient.invalidateQueries({ queryKey: battleKeys.state.queryKey });
        router.push(`/battles/room/${res.matchId}`);
    },
    onError: () => {
        notifications.show({ title: '매칭 실패', message: '사용 가능한 방이 없거나 매칭 중 오류가 발생했습니다.', color: 'red' });
    }
  });

  const handleJoinRoom = (roomId: number) => {
    if (!selectedCharId) {
        setPendingJoinRoomId(roomId);
        setIsCharModalOpen(true);
        notifications.show({ message: '먼저 전투에 참여할 캐릭터를 선택해 주세요.', color: 'violet' });
        return;
    }
    joinRoomMutation.mutate({ roomId, characterId: selectedCharId });
  };

  const handleCreateRoom = () => {
    const charId = selectedCharId || characters?.[0]?.id;
    
    if (!charId) {
        notifications.show({ message: '캐릭터를 선택해 주세요.', color: 'red' });
        setIsCharModalOpen(true);
        return;
    }
    createRoomMutation.mutate({
        mode: createForm.mode,
        characterId: charId
    });
  };

  const handleAutoMatch = () => {
    const charId = selectedCharId || characters?.[0]?.id;
    
    if (!charId) {
        notifications.show({ message: '캐릭터를 선택해 주세요.', color: 'red' });
        setIsCharModalOpen(true);
        return;
    }

    autoMatchMutation.mutate({
        ...autoMatchForm,
        characterId: charId
    });
  };

  const selectedChar = characters?.find(c => c.id === selectedCharId) || characters?.[0];

  const getCharIcon = (code: string) => {
    switch (code) {
      case 'CIRCLE': return <IconCircle size={80} stroke={1.5} />;
      case 'SQUARE': return <IconSquare size={80} stroke={1.5} />;
      case 'TRIANGLE': return <IconTriangle size={80} stroke={1.5} />;
      default: return <IconUser size={80} stroke={1.5} />;
    }
  };

  const winRate = stats ? (stats.matches > 0 ? (stats.wins / stats.matches) * 100 : 0) : 0;

  if (isUserLoading || !user) {
    return (
        <Center h="100vh" bg="#0A0A0B" className="fixed inset-0 z-[1000]">
            <Stack align="center" gap="md">
                <Loader color="violet" size="xl" type="bars" />
                <Text c="dimmed" size="sm">사용자 정보를 확인 중입니다...</Text>
            </Stack>
        </Center>
    );
  }

  return (
    <Container size="xl" py="xl">
      <Grid gutter="xl">
        {/* Left Panel: Profile & Actions */}
        <Grid.Col span={{ base: 12, md: 4, lg: 3.2 }}>
          <Stack gap="md">
            {/* Character Profile Card */}
            <Card p="lg" radius="lg" bg="#1A1B1E" className="border border-white/5 shadow-xl overflow-visible relative">
                {/* Ranking Badge */}
                <Box className="absolute -top-2 -right-2">
                    <ThemeIcon size={44} radius="xl" variant="gradient" gradient={{ from: 'violet.7', to: 'indigo.7' }} className="shadow-violet-900/40 shadow-lg">
                        <IconTrophy size={22} />
                    </ThemeIcon>
                </Box>

                <Stack align="center" gap="xs">
                    <Box className="relative group">
                        <div className="absolute inset-0 bg-violet-600/20 blur-2xl rounded-full opacity-50 group-hover:opacity-80 transition-opacity" />
                        <ThemeIcon size={100} radius={50} variant="outline" color="violet.4" className="bg-white/5 border border-white/10 relative z-10">
                            {getCharIcon(selectedChar?.code || '')}
                        </ThemeIcon>
                    </Box>
                    
                    <div className="text-center">
                        <Title order={4} className="text-white mb-0.5">{selectedChar?.name || '캐릭터 미선택'}</Title>
                        <Text size="xs" c="dimmed">{selectedChar?.description || '캐릭터를 선택해 주세요'}</Text>
                    </div>

                    <Group gap={6}>
                        <Badge variant="light" color="violet" size="sm" radius="sm">Rating: {stats?.rating ?? 1500}</Badge>
                        <Badge variant="light" color="cyan" size="sm" radius="sm">Rank: #{stats?.userId ?? '?'}</Badge>
                    </Group>
                </Stack>

                <Divider my="md" color="white/5" />

                <Stack gap="xs">
                    <SimpleGrid cols={2} spacing="xs">
                        <Paper p="xs" radius="md" bg="white/3" className="border border-white/5 text-center">
                            <Text size="10px" c="dimmed" tt="uppercase" fw={700}>Matches</Text>
                            <Text fw={800} size="md" className="text-white">{stats?.matches ?? 0}</Text>
                        </Paper>
                        <Paper p="xs" radius="md" bg="white/3" className="border border-white/5 text-center">
                            <Text size="10px" c="dimmed" tt="uppercase" fw={700}>Win Rate</Text>
                            <Text fw={800} size="md" className="text-white">{winRate.toFixed(1)}%</Text>
                        </Paper>
                    </SimpleGrid>

                    <Paper p="xs" radius="md" bg="white/3" className="border border-white/5">
                        <Group justify="center" gap="md">
                            <div className="text-center">
                                <Text size="10px" c="green.4" fw={700}>WIN</Text>
                                <Text fw={700} size="sm">{stats?.wins ?? 0}</Text>
                            </div>
                            <div className="text-center">
                                <Text size="10px" c="red.4" fw={700}>LOSS</Text>
                                <Text fw={700} size="sm">{stats?.losses ?? 0}</Text>
                            </div>
                            <div className="text-center">
                                <Text size="10px" c="dimmed" fw={700}>DRAW</Text>
                                <Text fw={700} size="sm">{stats?.draws ?? 0}</Text>
                            </div>
                        </Group>
                    </Paper>
                </Stack>

                <Stack gap="xs" mt="md">
                    <Button 
                        size="md"
                        variant="gradient" 
                        gradient={{ from: 'violet.6', to: 'indigo.6' }}
                        className="shadow-md"
                        leftSection={<IconSwords size={18} />}
                        onClick={() => setIsAutoMatchModalOpen(true)}
                    >
                        무작위 빠른 매칭
                    </Button>
                    <Button 
                        size="sm" 
                        color="dark.4" 
                        leftSection={<IconPlus size={18} />}
                        onClick={() => setIsRoomModalOpen(true)}
                    >
                        전투방 생성하기
                    </Button>
                    <Button 
                        size="sm" 
                        variant="outline" 
                        color="violet" 
                        leftSection={<IconUser size={18} />}
                        onClick={() => setIsCharModalOpen(true)}
                    >
                        배틀 캐릭터 변경
                    </Button>
                </Stack>
            </Card>
          </Stack>
        </Grid.Col>

        {/* Right Panel: Room List */}
        <Grid.Col span={{ base: 12, md: 8, lg: 8.8 }}>
          <Stack gap="md">
            <Group justify="space-between" mb="xs">
                <div>
                    <Title order={2} className="text-white flex items-center gap-2">
                        <IconUsers size={28} className="text-violet-400" />
                        배틀 라운지
                    </Title>
                    <Text size="sm" c="dimmed">현재 대기 중인 커스텀 대전 목록입니다.</Text>
                </div>
                <ActionIcon variant="light" color="violet" size="lg" radius="md">
                    <IconRotate size={20} />
                </ActionIcon>
            </Group>

            {isRoomsLoading ? (
                <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                    {[1,2,3,4].map(i => (
                        <Card key={i} h={140} bg="#1A1B1E" className="animate-pulse border border-white/5" radius="lg" />
                    ))}
                </SimpleGrid>
            ) : !roomsData || roomsData.content.length === 0 ? (
                <Paper p={60} radius="lg" bg="#1A1B1E" className="border border-white/5 border-dashed text-center">
                    <IconSwords size={48} className="mx-auto mb-4 text-gray-700" />
                    <Title order={4} c="dimmed">진행 중인 방이 없습니다.</Title>
                    <Text size="sm" c="dimmed" mt="xs">직접 방을 만들고 경쟁자들을 기다려보세요!</Text>
                    <Button variant="light" color="violet" mt="xl" leftSection={<IconPlus size={18}/>} onClick={() => setIsRoomModalOpen(true)}>
                        첫 번째 방 만들기
                    </Button>
                </Paper>
            ) : (
                <Stack>
                    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                        {roomsData.content.map((room) => (
                            <Card key={room.matchId} p="md" radius="lg" bg="#1A1B1E" className="border border-white/5 hover:border-violet-500/30 transition-colors group cursor-pointer">
                                <Group justify="space-between" mb="sm">
                                    <Badge color={room.matchType === 'RANKED' ? 'orange' : 'cyan'} variant="filled" size="sm">
                                        {room.matchType === 'RANKED' ? '랭크 게임' : '커스텀'}
                                    </Badge>
                                    <Text size="xs" c="dimmed">{new Date(room.createdAtEpochMs).toLocaleTimeString()}</Text>
                                </Group>
                                
                                <Title order={4} className="text-white mb-1 group-hover:text-violet-300 transition-colors">
                                    {room.mode.replace(/_/g, ' ')}
                                </Title>

                                <Group justify="space-between" mt="md" align="flex-end">
                                    <Group gap="xs">
                                        <IconUsers size={16} className="text-gray-500" />
                                        <Text size="sm" fw={600}>{room.currentPlayers} / {room.maxPlayers}</Text>
                                    </Group>
                                    <Button 
                                        size="xs" 
                                        color="violet" 
                                        variant="light"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleJoinRoom(room.matchId);
                                        }}
                                        loading={joinRoomMutation.isPending && joinRoomMutation.variables?.roomId === room.matchId}
                                    >
                                        입장하기
                                    </Button>
                                </Group>
                            </Card>
                        ))}
                    </SimpleGrid>
                    
                    <Group justify="center" mt="xl">
                        <Pagination 
                            total={roomsData.totalPages || 1} 
                            value={page} 
                            onChange={setPage} 
                            color="violet" 
                            radius="md"
                        />
                    </Group>
                </Stack>
            )}
          </Stack>
        </Grid.Col>
      </Grid>

      {/* Character Selection Modal */}
      <Modal 
        opened={isCharModalOpen} 
        onClose={() => setIsCharModalOpen(false)} 
        title={<Text fw={700}>캐릭터 선택</Text>}
        size="lg"
        radius="lg"
        padding="xl"
        centered
        overlayProps={{ backgroundOpacity: 0.5, blur: 4 }}
      >
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="lg">
            {characters?.map((char) => (
                <Card 
                    key={char.id} 
                    p="lg" 
                    radius="md" 
                    className={`border-2 cursor-pointer transition-all ${selectedCharId === char.id ? 'border-violet-500 bg-violet-600/5' : 'border-white/5 hover:border-white/10'}`}
                    onClick={() => handleCharSelect(char.id)}
                >
                    <Stack align="center" gap="md">
                        <ThemeIcon size={80} radius="xl" variant="light" color="violet">
                            {getCharIcon(char.code)}
                        </ThemeIcon>
                        <div className="text-center">
                            <Text fw={700}>{char.name}</Text>
                            <Text size="xs" c="dimmed" mt={4}>{char.description}</Text>
                        </div>
                    </Stack>
                </Card>
            ))}
        </SimpleGrid>
      </Modal>

      {/* Create Room Modal */}
      <Modal
        opened={isRoomModalOpen}
        onClose={() => setIsRoomModalOpen(false)}
        title={<Text fw={700}>전투방 생성</Text>}
        radius="lg"
        centered
        overlayProps={{ backgroundOpacity: 0.5, blur: 4 }}
      >
        <Stack gap="md">
            <Select
                label="전투 모드"
                placeholder="모드를 선택하세요"
                data={[
                    { label: '1라인 러쉬 (솔로)', value: BattleMode.SOLO_1LANE_RUSH },
                    { label: '2라인 푸쉬 (솔로)', value: BattleMode.SOLO_2LANE_PUSH },
                    { label: '1라인 러쉬 (팀)', value: BattleMode.TEAM_1LANE_RUSH },
                    { label: '2라인 푸쉬 (팀)', value: BattleMode.TEAM_2LANE_PUSH },
                ]}
                value={createForm.mode}
                onChange={(v) => setCreateForm({ ...createForm, mode: v as BattleMode })}
                required
            />
            
            <Box mt="md">
                <Button 
                    fullWidth 
                    size="md" 
                    color="violet" 
                    onClick={handleCreateRoom}
                    loading={createRoomMutation.isPending}
                    leftSection={<IconPlus size={18} />}
                >
                    방 생성 및 입장
                </Button>
            </Box>
        </Stack>
      </Modal>

      {/* Auto Match Modal */}
      <Modal
        opened={isAutoMatchModalOpen}
        onClose={() => setIsAutoMatchModalOpen(false)}
        title={<Text fw={700}>무작위 빠른 매칭</Text>}
        radius="lg"
        centered
        overlayProps={{ backgroundOpacity: 0.5, blur: 4 }}
      >
        <Stack gap="md">
            <Select
                label="매칭 타입"
                placeholder="매칭 타입을 선택하세요"
                data={[
                    { label: '랭크전 (RANKED)', value: BattleMatchType.RANKED },
                    { label: '친선전 (CUSTOM)', value: BattleMatchType.CUSTOM },
                ]}
                value={autoMatchForm.matchType}
                onChange={(v) => setAutoMatchForm({ ...autoMatchForm, matchType: v as BattleMatchType })}
                required
            />
            
            <Select
                label="전투 모드"
                placeholder="전투 모드를 선택하세요"
                data={[
                    { label: '1라인 러쉬 (솔로)', value: BattleMode.SOLO_1LANE_RUSH },
                    { label: '2라인 푸쉬 (솔로)', value: BattleMode.SOLO_2LANE_PUSH },
                    { label: '1라인 러쉬 (팀)', value: BattleMode.TEAM_1LANE_RUSH },
                    { label: '2라인 푸쉬 (팀)', value: BattleMode.TEAM_2LANE_PUSH },
                ]}
                value={autoMatchForm.mode}
                onChange={(v) => setAutoMatchForm({ ...autoMatchForm, mode: v as BattleMode })}
                required
            />
            
            <Box mt="md">
                <Button 
                    fullWidth 
                    size="md" 
                    color="violet" 
                    onClick={handleAutoMatch}
                    loading={autoMatchMutation.isPending}
                    leftSection={<IconSwords size={18} />}
                >
                    매칭 시작
                </Button>
            </Box>
        </Stack>
      </Modal>
    </Container>
  );
}
