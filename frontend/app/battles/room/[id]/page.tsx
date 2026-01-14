'use client';

import { useAuth } from '@/hooks/useAuth';
import {
    battleApi,
    RoomParticipant
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
    Grid,
    Group,
    Loader,
    Modal,
    Paper,
    SimpleGrid,
    Stack,
    Text,
    ThemeIcon,
    Title,
    Tooltip
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
    IconArrowLeft,
    IconCheck,
    IconCircle,
    IconClock,
    IconCrown,
    IconDoorExit,
    IconPlayerPlay,
    IconRotate,
    IconSquare,
    IconSwords,
    IconTriangle,
    IconUser,
    IconUserMinus
} from '@tabler/icons-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

interface SlotProps {
    participant?: RoomParticipant;
    team: 'A' | 'B';
    currentUserId?: number;
    matchType: string;
    isOwner: boolean;
    onKick: (userId: number, nickname: string) => void;
    isKickPending: boolean;
    kickVariables?: number;
    onTeamChange: (team: 'A' | 'B') => void;
    canChangeTeam: boolean;
    getCharIcon: (charId: number) => React.ReactNode;
}

const Slot = ({ 
    participant, 
    team, 
    currentUserId, 
    matchType, 
    isOwner, 
    onKick, 
    isKickPending, 
    kickVariables,
    onTeamChange,
    canChangeTeam,
    getCharIcon
}: SlotProps) => (
    <Card p="lg" radius="lg" bg={participant ? "#1A1B1E" : "rgba(255,255,255,0.02)"} 
          className={`border-2 transition-all ${participant ? (participant.isReady ? 'border-green-500/50 bg-green-500/5' : 'border-white/5') : 'border-dashed border-white/5'}`}
    >
        {participant ? (
            <Stack align="center" gap="md">
                <Box className="relative">
                    <ThemeIcon size={100} radius={50} variant="outline" color={participant.isReady ? "green.4" : "violet.4"} className="bg-white/5 border border-white/10">
                        {getCharIcon(participant.characterId)}
                    </ThemeIcon>
                    {participant.isOwner && (
                        <Box className="absolute -top-1 -right-1">
                            <ThemeIcon size={32} radius="xl" color="orange" className="shadow-lg">
                                <IconCrown size={18} />
                            </ThemeIcon>
                        </Box>
                    )}
                    {participant.isReady && (
                        <Box className="absolute -bottom-1 -right-1">
                            <ThemeIcon size={32} radius="xl" color="green">
                                <IconCheck size={18} />
                            </ThemeIcon>
                        </Box>
                    )}
                    {isOwner && participant.userId !== currentUserId && matchType === 'CUSTOM' && (
                        <Box className="absolute -bottom-1 -left-1">
                            <Tooltip label="추방하기">
                                <ActionIcon 
                                    variant="filled" 
                                    color="red" 
                                    radius="xl" 
                                    size="lg"
                                    onClick={() => onKick(participant.userId, participant.characterName)}
                                    loading={isKickPending && kickVariables === participant.userId}
                                >
                                    <IconUserMinus size={18} />
                                </ActionIcon>
                            </Tooltip>
                        </Box>
                    )}
                </Box>
                <div className="text-center">
                    <Text fw={700} size="lg">{participant.characterName}</Text>
                    <Group gap={6} justify="center" mt={4}>
                        <Badge color="violet" size="xs" variant="light">R: {participant.rating}</Badge>
                        <Badge color="gray" size="xs" variant="light">{participant.wins}W {participant.losses}L</Badge>
                    </Group>
                </div>
            </Stack>
        ) : (
            <Center h={160}>
                <Stack align="center" gap="xs">
                    <IconUser size={40} className="text-white/10" />
                    <Text size="xs" c="dimmed">대기중...</Text>
                    {canChangeTeam && (
                        <Button size="compact-xs" variant="light" color="violet" onClick={() => onTeamChange(team)}>
                            팀 변경
                        </Button>
                    )}
                </Stack>
            </Center>
        )}
    </Card>
);

export default function BattleRoomPage() {
    const { id } = useParams<{ id: string }>();
    const roomId = Number(id);
    const router = useRouter();
    const queryClient = useQueryClient();
    const { user, isLoading: isUserLoading } = useAuth();
    
    const [isCharModalOpen, setIsCharModalOpen] = useState(false);
    const [kickTarget, setKickTarget] = useState<{ userId: number, nickname: string } | null>(null);
    const [seconds, setSeconds] = useState(0);

    // Auth Check
    useEffect(() => {
        if (!isUserLoading && !user) {
            notifications.show({ title: '로그인 필요', message: '대기실에 입장하시려면 로그인이 필요합니다.', color: 'red' });
            router.push('/login');
        }
    }, [user, isUserLoading, router]);

    // Queries
    const { data: room, isLoading: isRoomLoading, error } = useQuery({
        queryKey: battleKeys.roomDetail(roomId).queryKey,
        queryFn: () => battleApi.getRoomDetail(roomId),
        enabled: !!user && !isNaN(roomId),
    });

    // Matching Timer for Ranked
    useEffect(() => {
        if (room?.matchType === 'RANKED' && room?.status === 'WAITING') {
            const timer = setInterval(() => {
                setSeconds(prev => prev + 1);
            }, 1000);
            return () => {
                clearInterval(timer);
                setSeconds(0);
            };
        }
    }, [room?.matchType, room?.status]);

    const formatTime = (s: number) => {
        const mins = Math.floor(s / 60);
        const secs = s % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    // Room SSE for Real-time Updates
    useEffect(() => {
        if (!user || isNaN(roomId)) return;

        const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
        const sseUrl = `${baseUrl}/api/v1/battles/rooms/${roomId}/events`;
        
        const eventSource = new EventSource(sseUrl, { withCredentials: true });

        const handleSseMessage = (event: MessageEvent) => {
            try {
                const data = JSON.parse(event.data);
                console.log(`[Room SSE] Event received:`, data.type, data);

                // 해당 방의 이벤트가 아니면 무시
                if (data.matchId && Number(data.matchId) !== roomId) return;

                if (data.type === 'ROOM_SNAPSHOT' && data.payload) {
                    // 스냅샷 데이터 즉시 반영 (API 호출 절약 및 즉각적인 UI 응답)
                    queryClient.setQueryData(battleKeys.roomDetail(roomId).queryKey, data.payload);
                }
                
                // 모든 상태 변화 이벤트에 대해 추가적으로 리프레시 수행하거나, 
                // 스냅샷이 아닌 이벤트는 서버의 최신 정보로 동기화
                if (data.type && data.type !== 'PING') {
                    queryClient.invalidateQueries({ queryKey: battleKeys.state.queryKey });
                    if (data.type !== 'ROOM_SNAPSHOT') {
                        queryClient.invalidateQueries({ queryKey: battleKeys.roomDetail(roomId).queryKey });
                    }
                }
            } catch (err) {
                console.error('Room SSE Parse Error:', err);
            }
        };

        eventSource.onmessage = handleSseMessage;
        
        // 서버에서 이벤트 이름을 명시적으로 보낼 경우에 대비
        eventSource.addEventListener('ROOM_SNAPSHOT', handleSseMessage as EventListener);
        eventSource.addEventListener('READY_CHANGED', handleSseMessage as EventListener);
        eventSource.addEventListener('PARTICIPANT_JOINED', handleSseMessage as EventListener);
        eventSource.addEventListener('PARTICIPANT_LEFT', handleSseMessage as EventListener);

        eventSource.onopen = () => {
            console.log(`Room SSE Connected: ${roomId}`);
        };

        eventSource.onerror = (err) => {
            console.error('Room SSE Connection Error:', err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [user, roomId, queryClient]);

    const { data: myState } = useQuery({
        queryKey: battleKeys.state.queryKey,
        queryFn: battleApi.getMyState,
        enabled: !!user,
    });

    // Auto Redirection based on MyState
    useEffect(() => {
        if (!myState) return;

        // 게임 중이 아니거나 로비 상태면 로비로 튕겨내기
        if (myState.state === 'IDLE' || !myState.matchId) {
            notifications.show({ title: '방 입장 불가', message: '참여 중인 전장이 없습니다. 로비로 이동합니다.', color: 'yellow' });
            router.push('/battles');
            return;
        }

        // 현재 방 번호와 내 상태의 방 번호가 다르면 내 방으로 강제 이동
        if (myState.matchId !== roomId) {
            notifications.show({ title: '위치 조정', message: '현재 참여 중인 전장으로 이동합니다.', color: 'blue' });
            router.push(`/battles/room/${myState.matchId}`);
        }
    }, [myState, roomId, router]);

    const { data: characters } = useQuery({
        queryKey: battleKeys.characters.queryKey,
        queryFn: battleApi.getCharacters,
        enabled: !!user,
    });

    // Mutations
    const readyMutation = useMutation({
        mutationFn: (ready: boolean) => battleApi.toggleReady(roomId, ready),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: battleKeys.roomDetail(roomId).queryKey }),
    });

    const teamMutation = useMutation({
        mutationFn: (team: 'A' | 'B') => battleApi.changeTeam(roomId, team),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: battleKeys.roomDetail(roomId).queryKey }),
    });

    const startMutation = useMutation({
        mutationFn: () => battleApi.startGame(roomId),
        onSuccess: () => {
            notifications.show({ title: '게임 시작!', message: '전투장으로 진입합니다.', color: 'violet' });
            queryClient.invalidateQueries({ queryKey: battleKeys.state.queryKey });
            // Logic for match start redirection if needed
        },
        onError: () => notifications.show({ title: '시작 불가', message: '모든 인원이 준비 완료 상태여야 합니다.', color: 'red' }),
    });

    const leaveMutation = useMutation({
        mutationFn: () => battleApi.leaveRoom(roomId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: battleKeys.state.queryKey });
            router.push('/battles');
            notifications.show({ message: '대기실에서 퇴장했습니다.', color: 'gray' });
        },
    });

    const kickMutation = useMutation({
        mutationFn: (targetUserId: number) => battleApi.kickPlayer(roomId, targetUserId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: battleKeys.roomDetail(roomId).queryKey });
            setKickTarget(null);
            notifications.show({ message: '플레이어를 추방했습니다.', color: 'gray' });
        },
        onError: () => notifications.show({ title: '추방 실패', message: '추방할 수 없습니다.', color: 'red' }),
    });

    const charMutation = useMutation({
        mutationFn: (charId: number) => battleApi.changeCharacter(roomId, charId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: battleKeys.roomDetail(roomId).queryKey });
            setIsCharModalOpen(false);
            notifications.show({ title: '캐릭터 변경 완료', message: '대기실 캐릭터가 업데이트되었습니다.', color: 'violet' });
        },
    });

    if (isUserLoading || isRoomLoading || !user) {
        return (
            <Center h="100vh" bg="#0A0A0B">
                <Stack align="center" gap="md">
                    <Loader color="violet" size="xl" type="bars" />
                    <Text c="dimmed">대기실 정보를 불러오고 있습니다...</Text>
                </Stack>
            </Center>
        );
    }

    if (error || !room) {
        return (
            <Center h="100vh" bg="#0A0A0B">
                <Stack align="center" gap="lg">
                    <Title order={3} c="dimmed">유효하지 않은 대기실이거나 연결이 끊어졌습니다.</Title>
                    <Button variant="outline" color="violet" leftSection={<IconArrowLeft size={18}/>} onClick={() => router.push('/battles')}>
                        로비로 돌아가기
                    </Button>
                </Stack>
            </Center>
        );
    }

    const me = room.participants.find(p => p.userId === user.id);
    const isOwner = !!me?.isOwner;
    const teamA = room.participants.filter(p => p.team === 'A');
    const teamB = room.participants.filter(p => p.team === 'B');

    const getCharIcon = (charId: number) => {
        const char = characters?.find(c => c.id === charId);
        switch (char?.code) {
            case 'CIRCLE': return <IconCircle size={60} stroke={1.5} />;
            case 'SQUARE': return <IconSquare size={60} stroke={1.5} />;
            case 'TRIANGLE': return <IconTriangle size={60} stroke={1.5} />;
            default: return <IconUser size={60} stroke={1.5} />;
        }
    };


    return (
        <Container size="xl" py="xl">
            <Stack gap="xl">
                <Stack gap="md">
                    <Group>
                        <Button variant="subtle" color="gray" leftSection={<IconArrowLeft size={18} />} onClick={() => leaveMutation.mutate()}>
                            로비로 돌아가기
                        </Button>
                    </Group>
                    <Group justify="space-between" align="center">
                        <Group gap="xs">
                            <Badge variant="outline" color="gray" size="lg" radius="sm">
                                MATCH #{room.matchId}
                            </Badge>
                            {room.matchType === 'RANKED' && (
                                <Badge variant="filled" color="dark" size="lg" radius="sm" leftSection={<IconClock size={14} />}>
                                    {formatTime(seconds)}
                                </Badge>
                            )}
                        </Group>
                        <Group gap="sm" align="center">
                            <Title order={2} className="text-white whitespace-nowrap">
                                {room.mode.replace(/_/g, ' ')}
                            </Title>
                            <Badge variant="filled" color={room.matchType === 'RANKED' ? 'red' : 'blue'} size="lg" radius="sm">
                                {room.matchType}
                            </Badge>
                        </Group>
                    </Group>
                </Stack>

                <Grid grow>
                    <Grid.Col span={{ base: 12, md: 5 }}>
                        <Title order={2} className="text-white mb-6 flex items-center gap-3">
                            <ThemeIcon size={36} radius="md" color="blue" variant="light">A</ThemeIcon>
                            TEAM ALPHA
                        </Title>
                        <SimpleGrid cols={1} spacing="lg">
                            <Slot 
                                participant={teamA[0]} 
                                team="A" 
                                currentUserId={user?.id}
                                matchType={room.matchType}
                                isOwner={isOwner}
                                onKick={(uId, nick) => setKickTarget({ userId: uId, nickname: nick })}
                                isKickPending={kickMutation.isPending}
                                kickVariables={kickMutation.variables}
                                onTeamChange={(t) => teamMutation.mutate(t)}
                                canChangeTeam={Boolean(me && me.team !== 'A' && !me.isReady && room?.matchType === 'CUSTOM')}
                                getCharIcon={getCharIcon}
                            />
                        </SimpleGrid>
                    </Grid.Col>

                    <Grid.Col span={{ base: 12, md: 2 }} className="flex items-center justify-center">
                        <Box className="hidden md:block py-20">
                            <ThemeIcon size={64} radius="xl" variant="outline" color="gray.7" className="border-2 border-dashed">
                                <IconSwords size={32} className="text-white/20" />
                            </ThemeIcon>
                        </Box>
                    </Grid.Col>

                    <Grid.Col span={{ base: 12, md: 5 }}>
                        <Title order={2} className="text-white mb-6 flex items-center gap-3 justify-end md:text-right">
                            TEAM BETA
                            <ThemeIcon size={36} radius="md" color="red" variant="light">B</ThemeIcon>
                        </Title>
                        <SimpleGrid cols={1} spacing="lg">
                            <Slot 
                                participant={teamB[0]} 
                                team="B" 
                                currentUserId={user?.id}
                                matchType={room.matchType}
                                isOwner={isOwner}
                                onKick={(uId, nick) => setKickTarget({ userId: uId, nickname: nick })}
                                isKickPending={kickMutation.isPending}
                                kickVariables={kickMutation.variables}
                                onTeamChange={(t) => teamMutation.mutate(t)}
                                canChangeTeam={Boolean(me && me.team !== 'B' && !me.isReady && room?.matchType === 'CUSTOM')}
                                getCharIcon={getCharIcon}
                            />
                        </SimpleGrid>
                    </Grid.Col>
                </Grid>

                {/* Footer Controls */}
                <Paper p="xl" radius="lg" bg="#1A1B1E" className="border border-white/5 mt-auto shadow-2xl">
                    <Group justify={room.matchType === 'RANKED' ? 'center' : 'space-between'}>
                        <Group gap="md">
                            {room.matchType === 'CUSTOM' && (
                                <Button size="lg" variant="outline" color="violet" leftSection={<IconRotate size={20} />} onClick={() => setIsCharModalOpen(true)}>
                                    캐릭터 변경
                                </Button>
                            )}
                        </Group>
                        
                        <Group gap="md">
                            <Button 
                                size="lg" 
                                variant="subtle" 
                                color="gray" 
                                leftSection={<IconDoorExit size={20} />}
                                onClick={() => leaveMutation.mutate()}
                                loading={leaveMutation.isPending}
                            >
                                나가기
                            </Button>
                            
                            {room.matchType === 'RANKED' ? (
                                <Button 
                                    size="lg" 
                                    color="red" 
                                    variant="light"
                                    className="px-10 border border-red-500/20"
                                    leftSection={<Loader size="xs" color="red" />}
                                    disabled
                                >
                                    매칭 대기 중... ({formatTime(seconds)})
                                </Button>
                            ) : (
                                <Group gap="md">
                                    <Button 
                                        size="lg" 
                                        variant={me?.isReady ? "light" : "filled"}
                                        color={me?.isReady ? "gray" : "green"}
                                        className="px-10"
                                        onClick={() => readyMutation.mutate(!me?.isReady)}
                                        loading={readyMutation.isPending}
                                    >
                                        {me?.isReady ? "준비 해제" : "준비 완료"}
                                    </Button>
                                    {isOwner && (
                                        <Button 
                                            size="lg" 
                                            color="violet" 
                                            disabled={!room.canStart} 
                                            leftSection={<IconPlayerPlay size={20} />}
                                            onClick={() => startMutation.mutate()}
                                            loading={startMutation.isPending}
                                            className="px-10"
                                        >
                                            게임 시작
                                        </Button>
                                    )}
                                </Group>
                            )}
                        </Group>
                    </Group>
                </Paper>
            </Stack>

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
                            className={`border-2 cursor-pointer transition-all ${me?.characterId === char.id ? 'border-violet-500 bg-violet-600/5' : 'border-white/5 hover:border-white/10'}`}
                            onClick={() => charMutation.mutate(char.id)}
                        >
                            <Stack align="center" gap="md">
                                <ThemeIcon size={80} radius="xl" variant="light" color="violet">
                                    {getCharIcon(char.id)}
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

            {/* Kick Confirmation Modal */}
            <Modal
                opened={!!kickTarget}
                onClose={() => setKickTarget(null)}
                title={<Text fw={700}>플레이어 추방</Text>}
                centered
                radius="lg"
                overlayProps={{ backgroundOpacity: 0.5, blur: 4 }}
            >
                <Stack gap="lg" py="md">
                    <Text size="sm">
                        <Text component="span" fw={700} c="red">{kickTarget?.nickname}</Text> 님을 정말 대기실에서 추방하시겠습니까?
                    </Text>
                    <Group justify="flex-end" gap="sm">
                        <Button variant="subtle" color="gray" onClick={() => setKickTarget(null)}>
                            취소
                        </Button>
                        <Button 
                            color="red" 
                            loading={kickMutation.isPending}
                            onClick={() => kickTarget && kickMutation.mutate(kickTarget.userId)}
                        >
                            추방하기
                        </Button>
                    </Group>
                </Stack>
            </Modal>
        </Container>
    );
}
