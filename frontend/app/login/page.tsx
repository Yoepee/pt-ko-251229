'use client';

import { useAuth } from '@/hooks/useAuth';
import { Anchor, Button, Container, Paper, PasswordInput, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';

export default function Login() {
  const { login, isLoginPending } = useAuth();
  
  const form = useForm({
    initialValues: {
      username: '',
      password: '',
    },
    validate: {
      username: (value) => (value.length < 2 ? 'Username is too short' : null),
      password: (value) => (value.length < 6 ? 'Password must be at least 6 characters' : null),
    },
  });

  const handleSubmit = (values: typeof form.values) => {
    login(values);
  };

  return (
    <Container size="xs" py={120}>
      <div className="text-center mb-8">
         <Title order={1} className="text-3xl font-bold mb-2">Welcome Back</Title>
         <Text c="dimmed">Sign in to your account to continue</Text>
      </div>

      <Paper radius="xl" p="xl" withBorder className="bg-white/80 backdrop-blur-md">
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <TextInput
            label="Username"
            placeholder="Enter your username"
            required
            radius="md"
            mb="md"
            {...form.getInputProps('username')}
          />
          <PasswordInput
            label="Password"
            placeholder="Your password"
            required
            radius="md"
            mb="xl"
            {...form.getInputProps('password')}
          />
          <Button 
            fullWidth 
            mt="xl" 
            size="lg" 
            radius="xl" 
            type="submit"
            loading={isLoginPending}
          >
            Sign in
          </Button>
        </form>
      </Paper>

      <Text align="center" mt="md" size="sm" c="dimmed">
        Don&apos;t have an account?{' '}
        <Anchor href="/register" size="sm">
          Register
        </Anchor>
      </Text>
    </Container>
  );
}
