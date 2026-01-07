'use client';

import { useAuth } from '@/hooks/useAuth';
import { Avatar, Burger, Button, Container, Group, Image, Menu, Text, UnstyledButton } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconLogout, IconUser } from '@tabler/icons-react';
import Link from 'next/link';

const links = [
  { link: '/', label: 'Home' },
  { link: '/category', label: 'Categories' },
  { link: '/chat', label: 'Chat' },
];

export function Header() {
  const [opened, { toggle }] = useDisclosure(false);
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b border-gray-200 bg-white/70 backdrop-blur-xl">
      <Container size="lg" className="h-14 flex items-center justify-between">
        <Group>
           <Link href="/" className="no-underline text-black flex items-center gap-2">
             <Image src="/images/logo.jpg" w={32} h={32} radius="md" alt="Logo" />
             <Text fw={600} size="lg">Blog</Text>
           </Link>
        </Group>

        <Group gap={20} visibleFrom="xs">
          {links.map((link) => (
            <Link 
              key={link.label} 
              href={link.link}
              className="text-sm font-medium text-gray-600 hover:text-black transition-colors"
            >
              {link.label}
            </Link>
          ))}
          
          {user ? (
            <Menu shadow="md" width={200} position="bottom-end">
              <Menu.Target>
                <UnstyledButton>
                  <Group gap={8}>
                    <Avatar src={null} alt={user.nickname} radius="xl" size="sm" color="blue">
                      {user.nickname?.substring(0, 1)}
                    </Avatar>
                    <Text size="sm" fw={500}>{user.nickname}</Text>
                  </Group>
                </UnstyledButton>
              </Menu.Target>

              <Menu.Dropdown>
                <Menu.Label>My Account</Menu.Label>
                <Menu.Item 
                  leftSection={<IconUser size={14} />}
                  component={Link}
                  href="/profile"
                >
                  Profile
                </Menu.Item>
                <Menu.Item 
                  leftSection={<IconUser size={14} />}
                  component={Link}
                  href="/category/manage"
                >
                  Manage Categories
                </Menu.Item>
                <Menu.Divider />
                <Menu.Item 
                  color="red" 
                  leftSection={<IconLogout size={14} />}
                  onClick={() => logout()}
                >
                  Logout
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          ) : (
             <Link href="/login">
               <Button size="xs" variant="light" radius="xl">Login</Button>
             </Link>
          )}
        </Group>

        <Burger opened={opened} onClick={toggle} hiddenFrom="xs" size="sm" />
      </Container>
    </header>
  );
}
