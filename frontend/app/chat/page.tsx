'use client';

import { ActionIcon, Avatar, Container, Grid, Group, Paper, ScrollArea, Text, TextInput } from '@mantine/core';
import { IconSearch, IconSend } from '@tabler/icons-react';
import { useState } from 'react';

const CONTACTS = [
  { id: 1, name: 'Alice Design', message: 'Hey, did you see the new update?', time: '10:30 AM', avatar: '/images/avatar-alice.jpg' },
  { id: 2, name: 'Bob Tech', message: 'The render quality is amazing.', time: 'Yesterday', avatar: '/images/avatar-bob.jpg' },
  { id: 3, name: 'Charlie', message: 'Meeting at 2 PM?', time: 'Tue', avatar: '/images/avatar-charlie.jpg' },
];

const MESSAGES = [
  { id: 1, sender: 'them', text: 'Hey! How is the blog coming along?' },
  { id: 2, sender: 'me', text: 'It goes well! Just finishing the Chat UI.' },
  { id: 3, sender: 'me', text: 'Trying to make it look like iMessage.' },
  { id: 4, sender: 'them', text: 'Nice! Make sure to add the blur effects.' },
  { id: 5, sender: 'me', text: 'Already done! Mantine makes it easy.' },
];

export default function Chat() {
  const [messages, setMessages] = useState(MESSAGES);
  const [input, setInput] = useState('');

  const handleSend = () => {
    if (!input.trim()) return;
    setMessages([...messages, { id: Date.now(), sender: 'me', text: input }]);
    setInput('');
  };

  return (
    <Container size="lg" py="xl" className="h-[calc(100vh-140px)]">
      <Grid h="100%" gutter="xl">
        {/* Sidebar */}
        <Grid.Col span={{ base: 12, md: 4 }} className="h-full flex flex-col">
          <Paper radius="xl" withBorder h="100%" p="md" className="flex flex-col bg-white/80 backdrop-blur-md">
            <Text size="xl" fw={700} mb="md" ml="xs">Messages</Text>
            <TextInput 
              placeholder="Search" 
              leftSection={<IconSearch size={16} />}
              radius="lg"
              mb="md"
              variant="filled"
            />
            <ScrollArea className="flex-1 -mx-2 px-2">
              <div className="space-y-1">
                {CONTACTS.map((contact) => (
                  <div key={contact.id} className="p-3 hover:bg-gray-100 rounded-xl cursor-pointer transition-colors duration-200">
                    <Group wrap="nowrap">
                      <Avatar src={contact.avatar} radius="xl" alt={contact.name} />
                      <div className="flex-1 min-w-0">
                        <Group justify="space-between" align="baseline">
                           <Text size="sm" fw={600} truncate>{contact.name}</Text>
                           <Text size="xs" c="dimmed">{contact.time}</Text>
                        </Group>
                        <Text size="xs" c="dimmed" truncate>{contact.message}</Text>
                      </div>
                    </Group>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </Paper>
        </Grid.Col>

        {/* Listen for details */}
        <Grid.Col span={{ base: 12, md: 8 }} className="h-full hidden md:block">
           <Paper radius="xl" withBorder h="100%" p="md" className="flex flex-col bg-white/80 backdrop-blur-md relative overflow-hidden">
             {/* Chat Header */}
             <div className="absolute top-0 left-0 right-0 p-4 border-b border-gray-100 bg-white/50 backdrop-blur-sm z-10 text-center">
                <Text size="sm" fw={500} c="dimmed">Alice Design</Text>
             </div>

             {/* Messages */}
             <ScrollArea className="flex-1 pt-16 pb-4 px-4 space-y-4">
               {messages.map((msg) => (
                 <div key={msg.id} className={`flex ${msg.sender === 'me' ? 'justify-end' : 'justify-start'}`}>
                   <div 
                     className={`max-w-[70%] px-4 py-2 rounded-2xl text-sm ${
                       msg.sender === 'me' 
                         ? 'bg-blue-500 text-white rounded-br-sm' 
                         : 'bg-gray-200 text-black rounded-bl-sm'
                     }`}
                   >
                     {msg.text}
                   </div>
                 </div>
               ))}
             </ScrollArea>

             {/* Input */}
             <div className="mt-4 flex gap-2">
               <TextInput 
                 className="flex-1"
                 radius="xl"
                 placeholder="iMessage"
                 value={input}
                 onChange={(e) => setInput(e.target.value)}
                 onKeyDown={(e) => e.key === 'Enter' && handleSend()}
               />
               <ActionIcon 
                 variant="filled" 
                 color="blue" 
                 radius="xl" 
                 size="lg" 
                 onClick={handleSend}
                 disabled={!input.trim()}
               >
                 <IconSend size={18} />
               </ActionIcon>
             </div>
           </Paper>
        </Grid.Col>
      </Grid>
    </Container>
  );
}
