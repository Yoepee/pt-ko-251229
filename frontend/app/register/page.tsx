'use client';

import { useAuth } from '@/hooks/useAuth';
import { Anchor, Button, Checkbox, Container, Paper, PasswordInput, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';

export default function Register() {
  const { signup, isSignupPending } = useAuth();

  const form = useForm({
    initialValues: {
      username: '',
      password: '',
      nickname: '',
      terms: false,
    },
    validate: {
      username: (value) => (value.length < 4 ? 'Username must be at least 4 characters' : null),
      password: (value) => (value.length < 6 ? 'Password must be at least 6 characters' : null),
      nickname: (value) => (value.length < 2 ? 'Nickname is too short' : null),
      terms: (value) => (!value ? 'You must agree to terms' : null),
    },
  });

  const handleSubmit = (values: typeof form.values) => {
    const { terms, ...data } = values;
    signup(data);
  };

  return (
    <Container size="xs" py={120}>
      <div className="text-center mb-8">
         <Title order={1} className="text-3xl font-bold mb-2">Create Account</Title>
         <Text c="dimmed">Join our community today</Text>
      </div>

      <Paper radius="xl" p="xl" withBorder className="bg-white/80 backdrop-blur-md">
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <TextInput
            label="Username"
            placeholder="Choose a username"
            required
            radius="md"
            mb="md"
            {...form.getInputProps('username')}
          />
          <TextInput
            label="Nickname"
            placeholder="Your display name"
            required
            radius="md"
            mb="md"
            {...form.getInputProps('nickname')}
          />
          <PasswordInput
            label="Password"
            placeholder="Create a password"
            required
            radius="md"
            mb="md"
            {...form.getInputProps('password')}
          />
          <Checkbox 
            label="I agree to the terms and conditions"
            mb="xl"
            {...form.getInputProps('terms', { type: 'checkbox' })}
          />
          <Button 
            fullWidth 
            size="lg" 
            radius="xl"
            type="submit"
            loading={isSignupPending}
          >
            Create Account
          </Button>
        </form>
      </Paper>

      <Text align="center" mt="md" size="sm" c="dimmed">
        Already have an account?{' '}
        <Anchor href="/login" size="sm">
          Sign in
        </Anchor>
      </Text>
    </Container>
  );
}
