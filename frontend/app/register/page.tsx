'use client';

import { Anchor, Button, Checkbox, Container, Paper, PasswordInput, Text, TextInput, Title } from '@mantine/core';

export default function Register() {
  return (
    <Container size="xs" py={120}>
      <div className="text-center mb-8">
         <Title order={1} className="text-3xl font-bold mb-2">Create Account</Title>
         <Text c="dimmed">Join our community today</Text>
      </div>

      <Paper radius="xl" p="xl" withBorder className="bg-white/80 backdrop-blur-md">
        <form onSubmit={(e) => e.preventDefault()}>
          <TextInput
            label="Full Name"
            placeholder="John Apple"
            required
            radius="md"
            mb="md"
          />
          <TextInput
            label="Email"
            placeholder="you@example.com"
            required
            radius="md"
            mb="md"
          />
          <PasswordInput
            label="Password"
            placeholder="Create a password"
            required
            radius="md"
            mb="md"
          />
          <Checkbox 
            label="I agree to the terms and conditions"
            mb="xl"
          />
          <Button fullWidth size="lg" radius="xl">
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
