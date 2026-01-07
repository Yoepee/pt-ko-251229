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
        <Title order={2}>Category Management</Title>
        <Button leftSection={<IconPlus size={16} />} onClick={openCreate}>Add Category</Button>
      </Group>

      <Paper withBorder p="md" radius="md" className="relative min-h-[200px]">
        <LoadingOverlay visible={isLoading} />
        
        <DragDropContext onDragEnd={onDragEnd}>
            <Droppable droppableId="root" type="GROUP">
                {(provided) => (
                    <div {...provided.droppableProps} ref={provided.innerRef}>
                         <Stack gap="xs">
                            {categories?.sort((a,b) => a.sortOrder - b.sortOrder).map((category, index) => (
                                <Draggable key={category.id} draggableId={category.id.toString()} index={index}>
                                    {(providedDrag) => (
                                        <div ref={providedDrag.innerRef} {...providedDrag.draggableProps}>
                                            <Paper withBorder radius="sm" className="bg-gray-50 mb-2 overflow-hidden">
                                                <div className="p-2 sm:p-3 flex items-center justify-between group bg-gray-50 hover:bg-gray-100 transition-colors">
                                                    <Group wrap="nowrap" gap="xs">
                                                        <div {...providedDrag.dragHandleProps} className="cursor-grab text-gray-400 flex items-center">
                                                            <IconGripVertical size={18} />
                                                        </div>
                                                        
                                                        <Text fw={600} style={{ whiteSpace: 'nowrap' }}>{category.name}</Text>
                                                        {category.slug && <Badge variant="light" color="gray" size="sm" className="hidden sm:inline-flex">{category.slug}</Badge>}
                                                    </Group>
                                                    
                                                    <Group gap={4} className="opacity-0 group-hover:opacity-100 transition-opacity" wrap="nowrap">
                                                        <ActionIcon variant="subtle" color="blue" onClick={() => openEdit(category)}>
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

        {categories?.length === 0 && <Text c="dimmed" ta="center">No categories found.</Text>}
      </Paper>

      {/* Delete Confirmation Modal */}
      <Modal 
        opened={!!deletingCategory} 
        onClose={() => setDeletingCategory(null)} 
        title="Delete Category" 
        centered
        size="sm"
      >
        <Text size="sm" mb="lg">
            Are you sure you want to delete <span className="font-semibold">{deletingCategory?.name}</span>? 
            This action cannot be undone.
        </Text>
        <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeletingCategory(null)}>Cancel</Button>
            <Button color="red" onClick={confirmDelete} loading={deleteMutation.isPending}>Delete</Button>
        </Group>
      </Modal>

      {/* Create/Edit Modal */}
      <Modal opened={opened} onClose={close} title={editingCategory ? "Edit Category" : "New Category"} centered>
        <form onSubmit={form.onSubmit(handleSubmit)}>
           <Stack>
              <TextInput label="Name" required {...form.getInputProps('name')} />
              <TextInput label="Slug" {...form.getInputProps('slug')} />
              <Button type="submit" loading={createMutation.isPending || updateMutation.isPending}>
                 {editingCategory ? 'Update' : 'Create'}
              </Button>
           </Stack>
        </form>
      </Modal>
    </Container>
  );
}
