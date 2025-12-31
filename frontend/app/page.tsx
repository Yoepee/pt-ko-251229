import { PostCard } from '@/components/PostCard';
import { Container, SimpleGrid, Text, Title } from '@mantine/core';

const MOCK_POSTS = [
  {
    id: '1',
    title: 'Designing for the Future',
    category: 'Design',
    image: '/images/post-design.jpg',
    date: 'Dec 31, 2025',
  },
  {
    id: '2',
    title: 'The Art of Minimalism',
    category: 'Lifestyle',
    image: '/images/post-minimal.jpg',
    date: 'Dec 30, 2025',
  },
  {
    id: '3',
    title: 'Next.js 15 Features',
    category: 'Tech',
    image: '/images/post-tech.jpg',
    date: 'Dec 29, 2025',
  },
  {
    id: '4',
    title: 'Apple Event Highlights',
    category: 'Tech',
    image: '/images/post-apple.jpg',
    date: 'Dec 28, 2025',
  },
];

export default function Home() {
  return (
    <Container size="lg" py="xl">
      <div className="text-center py-20 pb-24">
        <Title order={1} className="text-5xl md:text-7xl font-bold tracking-tight mb-4 bg-clip-text text-transparent bg-gradient-to-r from-gray-900 to-gray-500">
          Stories & Ideas
        </Title>
        <Text size="xl" c="dimmed" className="max-w-2xl mx-auto">
          Thoughts on design, technology, and the future of the web.
        </Text>
      </div>

      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xl" verticalSpacing="xl">
        {MOCK_POSTS.map((post) => (
          <PostCard key={post.id} {...post} />
        ))}
      </SimpleGrid>
    </Container>
  );
}
