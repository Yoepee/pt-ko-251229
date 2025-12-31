'use client';

import { Anchor, Button, Container, Paper, PasswordInput, Text, TextInput, Title } from '@mantine/core';

export default function Login() {
  return (
    <Container size="xs" py={120}>
      <div className="text-center mb-8">
         <Title order={1} className="text-3xl font-bold mb-2">Welcome Back</Title>
         <Text c="dimmed">Sign in to your account to continue</Text>
      </div>

      <Paper radius="xl" p="xl" withBorder className="bg-white/80 backdrop-blur-md">
        <form onSubmit={(e) => e.preventDefault()}>
          <TextInput
            label="Email"
            placeholder="you@example.com"
            required
            radius="md"
            mb="md"
          />
          <PasswordInput
            label="Password"
            placeholder="Your password"
            required
            radius="md"
            mb="xl"
          />
          <Button fullWidth mt="xl" size="lg" radius="xl">
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
