'use client';

import { Badge, Card, Group, Image, Text } from '@mantine/core';
import Link from 'next/link';

interface PostCardProps {
  id: string;
  title: string;
  category: string;
  image: string;
  date: string;
}

export function PostCard({ id, title, category, image, date }: PostCardProps) {
  return (
    <Link href={`/blog/${id}`} style={{ textDecoration: 'none' }}>
      <Card padding="lg" radius="xl" className="h-full border-gray-200 transition-all duration-300 hover:scale-[1.02] hover:shadow-xl">
        <Card.Section>
          <Image
            src={image}
            height={240}
            alt={title}
            className="object-cover"
          />
        </Card.Section>

        <Group justify="space-between" mt="md" mb="xs">
          <Badge color="gray" variant="light" size="sm">
            {category}
          </Badge>
          <Text size="xs" c="dimmed">
            {date}
          </Text>
        </Group>

        <Text fw={700} size="xl" mt="xs" className="leading-tight text-gray-900">
          {title}
        </Text>

        <Text size="sm" c="dimmed" mt="sm" lineClamp={2}>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </Text>
      </Card>
    </Link>
  );
}
