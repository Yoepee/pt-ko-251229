'use client';

import { Container, Group, Paper, Text, ThemeIcon, Title } from '@mantine/core';
import { IconChevronRight, IconCode, IconCoffee, IconDeviceLaptop, IconPalette } from '@tabler/icons-react';

const CATEGORIES = [
  { icon: IconPalette, color: 'pink', label: 'Design', count: 12 },
  { icon: IconCode, color: 'blue', label: 'Development', count: 24 },
  { icon: IconDeviceLaptop, color: 'gray', label: 'Technology', count: 8 },
  { icon: IconCoffee, color: 'orange', label: 'Lifestyle', count: 5 },
];

export default function Categories() {
  return (
    <Container size="sm" py={80}>
      <Title order={1} mb="xl">Categories</Title>
      
      <div className="space-y-4">
        {CATEGORIES.map((cat) => (
          <Paper 
            key={cat.label} 
            radius="lg" 
            p="md" 
            className="cursor-pointer hover:bg-white bg-gray-50/50 transition-colors border border-transparent hover:border-gray-200"
          >
            <Group>
              <ThemeIcon color={cat.color} variant="light" size="lg" radius="md">
                <cat.icon size={20} />
              </ThemeIcon>
              
              <div className="flex-1">
                <Text fw={600} size="lg">{cat.label}</Text>
                <Text size="xs" c="dimmed">{cat.count} posts</Text>
              </div>

              <IconChevronRight size={18} className="text-gray-400" />
            </Group>
          </Paper>
        ))}
      </div>
    </Container>
  );
}
