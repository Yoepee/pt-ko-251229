'use client';

import { Group, Image, Text } from '@mantine/core';
import Link from 'next/link';

interface EditorialCardProps {
  id: string;
  title: string;
  category: string;
  image: string;
  date: string;
  aspectRatio?: string; // Allow custom aspect ratios for grid variety
}

export function EditorialCard({ id, title, category, image, date, aspectRatio = 'aspect-[4/3]' }: EditorialCardProps) {
  return (
    <Link href={`/blog/${id}`} className="group no-underline block h-full">
      <div className="flex flex-col h-full gap-4">
        <div className={`relative overflow-hidden w-full ${aspectRatio} bg-gray-100`}>
          <Image
            src={image}
            alt={title}
            className="object-cover w-full h-full transition-transform duration-700 group-hover:scale-105"
            style={{ width: '100%', height: '100%' }}
          />
        </div>

        <div className="flex flex-col gap-1">
          <Group justify="space-between" align="center" className="mb-1">
             <Text size="xs" fw={600} tt="uppercase" opacity={0.5} className="tracking-wider">
               {category}
             </Text>
             <Text size="xs" c="dimmed">{date}</Text>
          </Group>
          <Text 
            fw={500} 
            size="xl" 
            className="font-sans leading-tight text-gray-900 group-hover:text-gray-600 transition-colors"
          >
            {title}
          </Text>
        </div>
      </div>
    </Link>
  );
}
