import { Container, Group, Text } from '@mantine/core';

export function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-gray-50 mt-12 py-8">
      <Container size="lg">
        <Group justify="space-between">
          <Text size="sm" c="dimmed">© 2025 Antigravity Blog. All rights reserved.</Text>
          <Group gap="xs">
            <Text size="sm" c="dimmed">Privacy</Text>
            <Text size="sm" c="dimmed">Terms</Text>
          </Group>
        </Group>
      </Container>
    </footer>
  );
}
