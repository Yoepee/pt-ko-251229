'use client';

import { Burger, Container, Group, Image, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import Link from 'next/link';

const links = [
  { link: '/', label: 'Home' },
  { link: '/category', label: 'Categories' },
  { link: '/chat', label: 'Chat' },
  { link: '/login', label: 'Login' },
];

export function Header() {
  const [opened, { toggle }] = useDisclosure(false);

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
        </Group>

        <Burger opened={opened} onClick={toggle} hiddenFrom="xs" size="sm" />
      </Container>
    </header>
  );
}
