'use client';

import { useAuth } from '@/hooks/useAuth';
import { authApi } from '@/lib/api';
import { queryKeys } from '@/lib/queryKeys';
import { useUserStore } from '@/store/userStore';
import { Badge, Button, Container, Divider, Group, Modal, Paper, PasswordInput, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';

export default function Profile() {
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const router = useRouter();
  const [opened, { open, close }] = useDisclosure(false);

  const nicknameForm = useForm({
    initialValues: { nickname: '' },
    validate: { nickname: (v) => (v.length < 2 ? 'Too short' : null) },
  });

  const passwordForm = useForm({
    initialValues: { 
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
    validate: { 
      currentPassword: (v) => (v.length < 6 ? 'Too short' : null),
      newPassword: (v) => (v.length < 6 ? 'Password must be at least 6 characters' : null),
      confirmPassword: (v, values) => (v !== values.newPassword ? 'Passwords do not match' : null),
    },
  });

  // Mutations
  const updateNickname = useMutation({
    mutationFn: authApi.updateNickname,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.me.queryKey });
      nicknameForm.reset();
      notifications.show({
        title: 'Success',
        message: 'Nickname updated successfully',
        color: 'green',
      });
    },
    onError: () => {
       notifications.show({
        title: 'Error',
        message: 'Failed to update nickname',
        color: 'red',
      });
    }
  });

  const updatePassword = useMutation({
    mutationFn: authApi.updatePassword,
    onSuccess: () => {
      passwordForm.reset();
      notifications.show({
        title: 'Success',
        message: 'Password updated. Please login again.',
        color: 'green',
      });
      // Manual Client Logout since server expired the cookie
      useUserStore.getState().logout();
      queryClient.setQueryData(queryKeys.auth.me.queryKey, null);
      router.push('/login');
    },
    onError: () => {
       notifications.show({
        title: 'Error',
        message: 'Failed to update password',
        color: 'red',
      });
    }
  });

  const withdraw = useMutation({
    mutationFn: authApi.withdraw,
    onSuccess: () => {
      close();
      logout(); 
      notifications.show({
        title: 'Account Deleted',
        message: 'Your account has been successfully deleted',
        color: 'blue',
      });
    },
  });

  if (!user) {
    return (
        <Container size="xs" py={100} className="text-center">
            <Text>Please login to view this page.</Text>
        </Container>
    );
  }

  return (
    <Container size="sm" py={80}>
      <Title order={1} mb="lg">My Profile</Title>

      <Paper radius="md" p="xl" withBorder mb="lg">
        <Title order={3} mb="md">Account Information</Title>
        <Group mb="xs">
            <Text fw={500} w={100}>Username:</Text> 
            <Text>{user.username}</Text>
        </Group>
        <Group mb="xs">
            <Text fw={500} w={100}>Nickname:</Text> 
            <Text>{user.nickname}</Text>
        </Group>
        <Group>
            <Text fw={500} w={100}>Role:</Text> 
            <Badge>{user.role}</Badge>
        </Group>
      </Paper>

      <Paper radius="md" p="xl" withBorder mb="lg">
        <Title order={4} mb="md">Change Nickname</Title>
        <form onSubmit={nicknameForm.onSubmit((values) => updateNickname.mutate(values.nickname))}>
          <Group align="flex-start">
             <TextInput 
                placeholder="New Nickname" 
                {...nicknameForm.getInputProps('nickname')}
                style={{ flex: 1 }}
             />
             <Button type="submit" loading={updateNickname.isPending}>Update</Button>
          </Group>
        </form>
      </Paper>

      <Paper radius="md" p="xl" withBorder mb="lg">
        <Title order={4} mb="md">Change Password</Title>
        <form onSubmit={passwordForm.onSubmit((values) => updatePassword.mutate({
            currentPassword: values.currentPassword,
            newPassword: values.newPassword
        }))}>
          <div className="flex flex-col gap-3">
             <PasswordInput 
                label="Current Password"
                placeholder="Current Password" 
                {...passwordForm.getInputProps('currentPassword')}
                required
             />
             <PasswordInput 
                label="New Password"
                placeholder="New Password" 
                {...passwordForm.getInputProps('newPassword')}
                required
             />
             <PasswordInput 
                label="Confirm New Password"
                placeholder="Confirm New Password" 
                {...passwordForm.getInputProps('confirmPassword')}
                required
             />
             <Button type="submit" mt="sm" loading={updatePassword.isPending}>Update Password</Button>
          </div>
        </form>
      </Paper>

      <Divider my="xl" />

      <Paper radius="md" p="xl" withBorder style={{ borderColor: 'red' }}>
         <Title order={4} c="red" mb="md">Danger Zone</Title>
         <Text size="sm" mb="md">Once you delete your account, there is no going back. Please be certain.</Text>
         <Button 
            color="red" 
            variant="outline" 
            onClick={open}
         >
            Delete Account
         </Button>
      </Paper>

      <Modal opened={opened} onClose={close} title="Confirm Deletion" centered>
        <Text size="sm" mb="lg">
          Are you sure you want to delete your account? This action cannot be undone.
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={close}>Cancel</Button>
          <Button 
            color="red" 
            onClick={() => withdraw.mutate()} 
            loading={withdraw.isPending}
          >
            Delete Account
          </Button>
        </Group>
      </Modal>
    </Container>
  );
}
