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
         <Title order={1} className="text-3xl font-black text-white mb-2 tracking-tight">다시 오신 것을 환영합니다</Title>
         <Text c="gray.4">계정에 로그인하여 투표에 참여하세요</Text>
      </div>

      <Paper radius="lg" p="xl" bg="#1A1B1E" className="border border-white/5 shadow-2xl">
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <TextInput
            label="사용자 이름"
            placeholder="아이디를 입력하세요"
            required
            mb="md"
            {...form.getInputProps('username')}
            classNames={{ input: 'bg-white/5 border-white/10' }}
          />
          <PasswordInput
            label="비밀번호"
            placeholder="비밀번호를 입력하세요"
            required
            mb="xl"
            {...form.getInputProps('password')}
            classNames={{ input: 'bg-white/5 border-white/10' }}
          />
          <Button 
            fullWidth 
            mt="xl" 
            size="lg" 
            radius="md" 
            type="submit"
            color="violet"
            loading={isLoginPending}
            className="shadow-lg shadow-violet-900/20"
          >
            로그인하기
          </Button>
        </form>
      </Paper>

      <Text ta="center" mt="xl" size="sm" c="dimmed">
        아직 계정이 없으신가요?{' '}
        <Anchor href="/register" size="sm" c="violet.4" fw={600}>
          회원가입
        </Anchor>
      </Text>
    </Container>
  );
}
