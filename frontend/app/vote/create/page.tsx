'use client';

import { ApiResponse, categoryApi, CreatePollRequest, CreatePollResponse, pollApi, PollType, PollVisibility } from '@/lib/api';
import { pollKeys, queryKeys } from '@/lib/queryKeys';
import { ActionIcon, Button, Container, Grid, Group, NumberInput, Paper, SegmentedControl, Select, Stack, Switch, Text, Textarea, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import dayjs from 'dayjs';
import { useRouter } from 'next/navigation';

export default function CreateVotePage() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: categories } = useQuery({
    queryKey: queryKeys.categories.all.queryKey,
    queryFn: categoryApi.getAll,
  });

  const form = useForm({
    initialValues: {
      title: '',
      description: '',
      categoryId: null as string | null,
      pollType: PollType.VOTE,
      options: ['찬성', '반대'],
      durationDays: 7,
      allowAnonymous: true,
      allowChange: true,
      maxSelections: 1,
      visibility: PollVisibility.PUBLIC
    },
    validate: {
      title: (v) => (v.length < 5 ? '제목은 5자 이상이어야 합니다.' : null),
      options: (v) => {
        if (v.some(opt => opt.trim() === '')) return '모든 옵션을 입력해야 합니다.';
        if (v.length < 2) return '최소 2개의 옵션이 필요합니다.';
        return null;
      }
    }
  });

  const createMutation = useMutation({
    mutationFn: (data: CreatePollRequest) => pollApi.create(data),
    onSuccess: (res: ApiResponse<CreatePollResponse>) => {
        notifications.show({ title: '투표 생성 완료!', message: '준비 되었습니다.', color: 'green' });
        queryClient.invalidateQueries({ queryKey: pollKeys.all.queryKey });
        const pollId = res.data?.pollId;
        if (pollId) {
            router.push(`/vote/${pollId}`);
        } else {
            router.push('/');
        }
    },
    onError: (err: AxiosError<ApiResponse<null>>) => {
        notifications.show({ 
            title: '생성 실패', 
            message: (err.response?.data as ApiResponse<null>)?.message || '투표를 만들 수 없습니다.', 
            color: 'red' 
        });
    }
  });

  const handleAddOption = () => {
    form.insertListItem('options', '');
  };

  const handleRemoveOption = (index: number) => {
    if (form.values.options.length <= 2) {
        notifications.show({ color: 'red', message: '최소 2개의 옵션이 필요합니다.' });
        return;
    }
    form.removeListItem('options', index);
  };

  const handleSubmit = async (values: typeof form.values) => {
    const endsAt = dayjs().add(values.durationDays, 'day').toISOString();
    
    const requestData: CreatePollRequest = {
        title: values.title,
        description: values.description || null, // Kept original logic for description being optional
        categoryId: values.categoryId ? Number(values.categoryId) : null,
        pollType: values.pollType as PollType, // Use form value
        visibility: values.visibility,
        allowAnonymous: values.allowAnonymous,
        allowChange: values.allowChange,
        maxSelections: values.maxSelections,
        endsAt: endsAt,
        options: values.options
    };

    createMutation.mutate(requestData);
  };

  return (
    <Container size="sm" py={20}>
      <Title order={2} mb="xl" className="text-white">새로운 투표 만들기</Title>
      
      <Paper p="xl" radius="lg" bg="#1A1B1E">
        <Stack gap="lg">
             {/* Type Selection */}
              <div>
                <Text fw={600} mb="xs" c="gray.3">투표 유형</Text>
                <SegmentedControl
                    fullWidth
                    value={form.values.pollType} // Use form value
                    onChange={(v: string) => {
                        const newType = v as PollType;
                        form.setFieldValue('pollType', newType);
                        if (newType === PollType.VOTE) {
                            form.setFieldValue('options', ['찬성', '반대']);
                            form.setFieldValue('maxSelections', 1);
                        } else if (newType === PollType.RANK && form.values.options[0] === '찬성') {
                            form.setFieldValue('options', ['', '']);
                        }
                    }}
                    data={[
                        { label: '찬반 투표 (A vs B)', value: PollType.VOTE },
                        { label: '순위 투표 (리스트)', value: PollType.RANK },
                    ]}
                    color="violet"
                />
              </div>

             <form onSubmit={form.onSubmit(handleSubmit)}>
                <Stack gap="md">
                    <TextInput 
                        label="제목" 
                        placeholder={form.values.pollType === PollType.VOTE ? "예: 나는 민초파다" : "예: 다음 회식 메뉴는?"} 
                        required 
                        withAsterisk
                        {...form.getInputProps('title')}
                    />
                    
                    <Textarea 
                        label="상세 설명 (선택 사항)" 
                        placeholder="투표에 대한 추가 정보를 입력하세요" 
                        minRows={3}
                        {...form.getInputProps('description')}
                    />

                    <Select
                        label="카테고리"
                        placeholder="선택하세요"
                        data={categories?.map(c => ({ value: String(c.id), label: c.name })) || []}
                        {...form.getInputProps('categoryId')}
                        clearable
                        searchable
                    />

                    {/* Options Management */}
                    {form.values.pollType === PollType.RANK && (
                        <div>
                            <Group justify="space-between" mb="xs">
                                <Text fw={500} size="sm" c="gray.3">옵션</Text>
                                <Button variant="subtle" size="compact-xs" leftSection={<IconPlus size={14}/>} onClick={handleAddOption}>
                                    옵션 추가하기
                                </Button>
                            </Group>

                            <Stack gap="sm">
                                {form.values.options.map((_, index) => (
                                    <Group key={index} gap="xs">
                                        <TextInput 
                                            placeholder={`옵션 ${index + 1}`}
                                            className="flex-1"
                                            {...form.getInputProps(`options.${index}`)}
                                        />
                                        {form.values.options.length > 2 && (
                                            <ActionIcon color="red" variant="subtle" onClick={() => handleRemoveOption(index)}>
                                                <IconTrash size={16} />
                                            </ActionIcon>
                                        )}
                                    </Group>
                                ))}
                            </Stack>
                        </div>
                    )}

                    {form.values.pollType === PollType.VOTE && (
                         <div className="bg-white/5 p-4 rounded-lg border border-white/5">
                            <Text size="sm" c="gray.4" mb="xs" fw={500}>투표 옵션</Text>
                            <Group grow>
                                <Paper p="xs" ta="center" className="border border-blue-500/30 bg-blue-500/10">
                                    <Text size="sm" fw={700} c="blue.3">찬성</Text>
                                </Paper>
                                <Paper p="xs" ta="center" className="border border-cyan-500/30 bg-cyan-500/10">
                                    <Text size="sm" fw={700} c="cyan.3">반대</Text>
                                </Paper>
                            </Group>
                            <Text size="xs" c="dimmed" mt="sm">찬반 투표는 &apos;찬성&apos; 또는 &apos;반대&apos; 중에서 선택하는 방식으로 진행됩니다.</Text>
                         </div>
                    )}

                    <Grid>
                        <Grid.Col span={6}>
                            <NumberInput 
                                label="투표 기간 (일)" 
                                min={1} 
                                max={30} 
                                {...form.getInputProps('durationDays')} 
                            />
                        </Grid.Col>
                         <Grid.Col span={6}>
                            <NumberInput 
                                label="최대 선택 개수" 
                                min={1} 
                                max={form.values.pollType === PollType.VOTE ? 1 : 10} // Use form value
                                disabled={form.values.pollType === PollType.VOTE} // Use form value
                                {...form.getInputProps('maxSelections')} 
                            />
                        </Grid.Col>
                    </Grid>

                    <Group grow>
                        <Switch label="익명 투표 허용" {...form.getInputProps('allowAnonymous', { type: 'checkbox' })} />
                        <Switch label="투표 변경 허용" {...form.getInputProps('allowChange', { type: 'checkbox' })} />
                    </Group>

                    <Button type="submit" size="lg" fullWidth mt="xl" color="violet" loading={createMutation.isPending}>
                        투표 게시하기
                    </Button>
                </Stack>
             </form>
        </Stack>
      </Paper>
    </Container>
  );
}
