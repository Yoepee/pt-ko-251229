'use client';

import { createTheme } from '@mantine/core';

export const theme = createTheme({
  primaryColor: 'gray',
  defaultRadius: 'lg',
  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji"',
  headings: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji"',
    sizes: {
      h1: { fontSize: '2.5rem', lineHeight: '1.2' },
      h2: { fontSize: '2rem', lineHeight: '1.25' },
    },
  },
  components: {
    Card: {
      defaultProps: {
        shadow: 'sm',
        withBorder: true,
      },
      styles: (theme) => ({
        root: {
          backgroundColor: 'rgba(255, 255, 255, 0.8)', // Glass-ish
          backdropFilter: 'blur(10px)',
          transition: 'transform 0.2s ease, box-shadow 0.2s ease',
          '&:hover': {
            transform: 'scale(1.01)',
            boxShadow: theme.shadows.md,
          },
        },
      }),
    },
    Button: {
      defaultProps: {
        radius: 'xl',
      },
    },
  },
});
