'use client';

import { Category, categoryApi, CreateCategoryRequest, UpdateCategoryRequest } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { DragDropContext, Draggable, Droppable, DropResult } from '@hello-pangea/dnd';
import { ActionIcon, Badge, Button, Container, Group, LoadingOverlay, Modal, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { IconEdit, IconGripVertical, IconPlus, IconTrash } from '@tabler/icons-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

export default function CategoryManage() {
  const queryClient = useQueryClient();
  const [opened, { open, close }] = useDisclosure(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null);

  const { data: categories, isLoading } = useQuery({
    queryKey: queryKeys.categories.all.queryKey,
    queryFn: categoryApi.getAll,
  });

  const form = useForm({
    initialValues: {
      name: '',
      slug: '',
      sortOrder: 0,
    },
    validate: {
      name: (v) => (v.length < 1 ? 'Name required' : null),
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoryApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all.queryKey });
      close();
      form.reset();
      notifications.show({ title: 'Success', message: 'Category created', color: 'green' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateCategoryRequest) => categoryApi.update(editingCategory!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all.queryKey });
      close();
      setEditingCategory(null);
      form.reset();
      notifications.show({ title: 'Success', message: 'Category updated', color: 'green' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: categoryApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all.queryKey });
      notifications.show({ title: 'Deleted', message: 'Category deleted', color: 'blue' });
    },
  });

  const confirmDelete = () => {
      if (deletingCategory) {
          deleteMutation.mutate(deletingCategory.id);
          setDeletingCategory(null);
      }
  };

  const handleSubmit = (values: typeof form.values) => {
    let finalOrder = values.sortOrder;
    
    // Auto-calculate logic if creating
    if (!editingCategory && categories) {
        const maxOrder = Math.max(...categories.map(s => s.sortOrder), -1);
        finalOrder = maxOrder + 1;
    }

    const payload = {
      ...values,
      slug: values.slug || null, // Send null if empty string
      sortOrder: finalOrder,
    };

    if (editingCategory) {
      updateMutation.mutate(payload);
    } else {
      createMutation.mutate(payload);
    }
  };

  const openCreate = () => {
    setEditingCategory(null);
    form.reset();
    open();
  };

  const openEdit = (cat: Category) => {
    setEditingCategory(cat);
    form.setValues({
      name: cat.name,
      slug: cat.slug || '',
      sortOrder: cat.sortOrder,
    });
    open();
  };

  const handleDelete = (category: Category) => {
    setDeletingCategory(category);
  };

  const onDragEnd = async (result: DropResult) => {
    const { source, destination } = result;

    if (!destination || !categories) return;
    
    if (source.index === destination.index) return;

    // 1. Snapshot previous state
    const previousCategories = queryClient.getQueryData<Category[]>(queryKeys.categories.all.queryKey);

    // 2. Calculate New State
    const newCategories = JSON.parse(JSON.stringify(previousCategories || [])) as Category[];
    const listToReorder = newCategories;

    // Sort to ensure we invoke splice on correct indices
    listToReorder.sort((a,b) => a.sortOrder - b.sortOrder);

    // Move item
    const [removed] = listToReorder.splice(source.index, 1);
    listToReorder.splice(destination.index, 0, removed);

    // Update sortOrder
    listToReorder.forEach((item, index) => {
        item.sortOrder = index;
    });

    // 3. Optimistic Update
    queryClient.setQueryData(queryKeys.categories.all.queryKey, newCategories);

    // 4. API Calls
    try {
        const updatePromises = listToReorder.map(item => 
             categoryApi.update(item.id, {
                 name: item.name,
                 slug: item.slug,
                 sortOrder: item.sortOrder
             })
        );
        
        await Promise.all(updatePromises);
    } catch (error) {
        // 5. Revert on Error
        console.error("Failed to reorder:", error);
        notifications.show({ title: 'Error', message: 'Failed to save order. Reverting changes.', color: 'red' });
        queryClient.setQueryData(queryKeys.categories.all.queryKey, previousCategories);
    }
  };

  return (
    <Container size="md" py={80}>
      <Group justify="space-between" mb="lg">
        <Title order={2} c="white">카테고리 관리</Title>
        <Button 
            leftSection={<IconPlus size={16} />} 
            onClick={openCreate}
            color="violet"
            radius="md"
        >
            카테고리 추가
        </Button>
      </Group>

      <Paper radius="lg" p="md" bg="#1A1B1E" className="relative min-h-[200px] border border-white/5">
        <LoadingOverlay visible={isLoading} overlayProps={{ blur: 2, bg: 'rgba(0,0,0,0.5)' }} />
        
        <DragDropContext onDragEnd={onDragEnd}>
            <Droppable droppableId="root" type="GROUP">
                {(provided) => (
                    <div {...provided.droppableProps} ref={provided.innerRef}>
                         <Stack gap="xs">
                            {categories?.sort((a,b) => a.sortOrder - b.sortOrder).map((category, index) => (
                                <Draggable key={category.id} draggableId={category.id.toString()} index={index}>
                                    {(providedDrag) => (
                                        <div ref={providedDrag.innerRef} {...providedDrag.draggableProps}>
                                            <Paper radius="md" bg="rgba(255,255,255,0.03)" className="border border-white/5 mb-2 overflow-hidden hover:border-violet-500/30 transition-colors">
                                                <div className="p-3 flex items-center justify-between group">
                                                    <Group wrap="nowrap" gap="xs">
                                                        <div {...providedDrag.dragHandleProps} className="cursor-grab text-gray-500 flex items-center hover:text-white transition-colors">
                                                            <IconGripVertical size={18} />
                                                        </div>
                                                        
                                                        <Text fw={600} c="white" style={{ whiteSpace: 'nowrap' }}>{category.name}</Text>
                                                        {category.slug && <Badge variant="filled" color="gray" size="sm" className="hidden sm:inline-flex">{category.slug}</Badge>}
                                                    </Group>
                                                    
                                                    <Group gap={4} className="opacity-0 group-hover:opacity-100 transition-opacity" wrap="nowrap">
                                                        <ActionIcon variant="subtle" color="violet" onClick={() => openEdit(category)}>
                                                            <IconEdit size={16} />
                                                        </ActionIcon>
                                                        <ActionIcon variant="subtle" color="red" onClick={() => handleDelete(category)}>
                                                            <IconTrash size={16} />
                                                        </ActionIcon>
                                                    </Group>
                                                </div>
                                            </Paper>
                                        </div>
                                    )}
                                </Draggable>
                            ))}
                            {provided.placeholder}
                         </Stack>
                    </div>
                )}
            </Droppable>
        </DragDropContext>

        {categories?.length === 0 && <Text c="dimmed" ta="center" py="xl">카테고리가 없습니다.</Text>}
      </Paper>

      {/* Delete Confirmation Modal */}
      <Modal 
        opened={!!deletingCategory} 
        onClose={() => setDeletingCategory(null)} 
        title="카테고리 삭제" 
        centered
        size="sm"
        radius="lg"
        styles={{ content: { backgroundColor: '#1A1B1E', border: '1px solid rgba(255,255,255,0.1)' } }}
      >
        <Text size="sm" mb="lg" c="gray.3">
            정말로 <span className="font-bold text-white">{deletingCategory?.name}</span> 카테고리를 삭제하시겠습니까? 
            이 작업은 되돌릴 수 없습니다.
        </Text>
        <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeletingCategory(null)} radius="md">취소</Button>
            <Button color="red" onClick={confirmDelete} loading={deleteMutation.isPending} radius="md">삭제</Button>
        </Group>
      </Modal>

      {/* Create/Edit Modal */}
      <Modal 
        opened={opened} 
        onClose={close} 
        title={editingCategory ? "카테고리 수정" : "새 카테고리"} 
        centered
        radius="lg"
        styles={{ content: { backgroundColor: '#1A1B1E', border: '1px solid rgba(255,255,255,0.1)' } }}
      >
        <form onSubmit={form.onSubmit(handleSubmit)}>
           <Stack>
              <TextInput 
                label="이름" 
                placeholder="카테고리 이름을 입력하세요"
                required 
                {...form.getInputProps('name')} 
                classNames={{ input: 'bg-white/5 border-white/10' }}
              />
              <TextInput 
                label="슬러그" 
                placeholder="URL용 이름을 입력하세요 (선택)"
                {...form.getInputProps('slug')} 
                classNames={{ input: 'bg-white/5 border-white/10' }}
              />
              <Button 
                type="submit" 
                color="violet"
                radius="md"
                mt="md"
                loading={createMutation.isPending || updateMutation.isPending}
              >
                 {editingCategory ? '수정하기' : '생성하기'}
              </Button>
           </Stack>
        </form>
      </Modal>
    </Container>
  );
}
