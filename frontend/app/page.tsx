'use client';

import { EditorialCard } from '@/components/EditorialCard';
import { Box, Container, Text } from '@mantine/core';

const MOCK_POSTS = [
  {
    id: '1',
    title: 'Designing for the Future of Interfaces',
    category: 'Design',
    image: '/images/post-design.jpg', // Glass/Abstract
    date: 'Dec 31',
  },
  {
    id: '2',
    title: 'The Art of Digital Minimalism',
    category: 'Lifestyle',
    image: '/images/post-minimal.jpg', // Desk/Laptop
    date: 'Dec 30',
  },
  {
    id: '3',
    title: 'Next.js 15: A New Era',
    category: 'Tech',
    image: '/images/post-tech.jpg', // Neon
    date: 'Dec 29',
  },
  {
    id: '4',
    title: 'Apple Event: Metal & Glass',
    category: 'Highlights',
    image: '/images/post-apple.jpg', // Metal
    date: 'Dec 28',
  },
];

export default function Home() {
  return (
    <Box className="bg-[#FAF9F6] min-h-screen text-black overflow-hidden">
      
      {/* Hero: Spline 3D Scene - Full Width/Cinematic */}
      {/* increased height for impact, removed top text */}
      <div className="w-full h-[85vh] relative mb-20">
         <iframe 
            src='https://my.spline.design/r4xbot-I6h8m8Rh1kuPFOBEjfXIeSon/' 
            frameBorder='0' 
            width='100%' 
            height='100%'
            className="w-full h-full pointer-events-auto"
            title="Spline 3D Scene"
         />
         {/* Overlay gradient to blend bottom */}
         {/* <div className="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-[#FAF9F6] to-transparent pointer-events-none" /> */}
      </div>

      {/* Editorial Grid Section */}
      <Container size="xl" pb={120}>
        <div className="flex flex-col md:flex-row justify-between items-end mb-12 border-b border-black/10 pb-6">
            <h2 className="text-4xl font-light tracking-tight">Latest Stories</h2>
            <Text c="dimmed" size="sm" className="mb-1">Curated for the curious mind</Text>
        </div>

        {/* Custom asymmetrical grid layout */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-y-16 gap-x-8">
            
            {/* Featured Item (Design) - Takes 8 columns */}
            <div className="md:col-span-8">
                <EditorialCard 
                    {...MOCK_POSTS[0]} 
                    aspectRatio="aspect-[16/9]" 
                />
            </div>

            {/* Side Item (Minimalism) - Takes 4 columns, vertical */}
            <div className="md:col-span-4 flex flex-col justify-end">
                 <EditorialCard 
                    {...MOCK_POSTS[1]} 
                    aspectRatio="aspect-[3/4]" 
                />
            </div>

            {/* Row 2 */}
            {/* Tech - Takes 6 columns */}
            <div className="md:col-span-6">
                <EditorialCard 
                    {...MOCK_POSTS[2]} 
                    aspectRatio="aspect-[4/3]" 
                />
            </div>

            {/* Apple - Takes 6 columns */}
            <div className="md:col-span-6">
                 <EditorialCard 
                    {...MOCK_POSTS[3]} 
                    aspectRatio="aspect-[4/3]" 
                />
            </div>

        </div>
      </Container>
    </Box>
  );
}
