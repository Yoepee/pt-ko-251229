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
    signup(values);
  };

  return (
    <Container size="xs" py={120}>
      <div className="text-center mb-8">
         <Title order={1} className="text-3xl font-black text-white mb-2 tracking-tight">계정 만들기</Title>
         <Text c="gray.4">지금 가입하고 투표에 참여하세요</Text>
      </div>

      <Paper radius="lg" p="xl" bg="#1A1B1E" className="border border-white/5 shadow-2xl">
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <TextInput
            label="사용자 이름"
            placeholder="사용하실 아이디를 입력하세요"
            required
            mb="md"
            {...form.getInputProps('username')}
            classNames={{ input: 'bg-white/5 border-white/10' }}
          />
          <TextInput
            label="닉네임"
            placeholder="표시될 이름을 입력하세요"
            required
            mb="md"
            {...form.getInputProps('nickname')}
            classNames={{ input: 'bg-white/5 border-white/10' }}
          />
          <PasswordInput
            label="비밀번호"
            placeholder="비밀번호를 입력하세요"
            required
            mb="md"
            {...form.getInputProps('password')}
            classNames={{ input: 'bg-white/5 border-white/10' }}
          />
          <Checkbox 
            label="이용 약관에 동의합니다"
            mb="xl"
            color="violet"
            {...form.getInputProps('terms', { type: 'checkbox' })}
          />
          <Button 
            fullWidth 
            size="lg" 
            radius="md"
            type="submit"
            color="violet"
            loading={isSignupPending}
            className="shadow-lg shadow-violet-900/20"
          >
            가입하기
          </Button>
        </form>
      </Paper>

      <Text ta="center" mt="xl" size="sm" c="dimmed">
        이미 계정이 있으신가요?{' '}
        <Anchor href="/login" size="sm" c="violet.4" fw={600}>
          로그인
        </Anchor>
      </Text>
    </Container>
  );
}
