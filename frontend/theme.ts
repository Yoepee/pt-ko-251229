import { createTheme } from '@mantine/core';

export const theme = createTheme({
  primaryColor: 'violet',
  colors: {
    // Custom dark violet scale for background/UI
    dark: [
      '#C1C2C5',
      '#A6A7AB',
      '#909296',
      '#5C5F66',
      '#373A40',
      '#2C2E33',
      '#25262B', // 6: Main background
      '#1A1B1E', // 7: Card background
      '#141517',
      '#101113',
    ],
    // Vibrant Violet for accents
    violet: [
      '#F3F0FF',
      '#E5DBFF',
      '#D0BFFF',
      '#B197FC',
      '#9775FA',
      '#845EF7',
      '#7950F2',
      '#7048E8',
      '#6741D9',
      '#5F3DC4',
    ],
  },
  defaultRadius: 'md',
  fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif',
  headings: {
    fontFamily: 'Outfit, sans-serif',
    fontWeight: '700',
  },
  components: {
    Button: {
      defaultProps: {
        radius: 'md',
        fw: 600,
      },
      styles: {
        root: {
            transition: 'transform 0.1s ease',
        },
      }
    },
    Card: {
      defaultProps: {
        radius: 'lg',
        withBorder: true,
      },
      styles: {
        root: {
          backgroundColor: 'rgba(26, 27, 30, 0.6)', // Glass-like dark
          backdropFilter: 'blur(12px)',
          borderColor: 'rgba(255, 255, 255, 0.1)',
        },
      },
    },
    Paper: {
        defaultProps: {
            radius: 'lg',
        },
        styles: {
            root: {
                backgroundColor: 'rgba(26, 27, 30, 0.6)',
                borderColor: 'rgba(255, 255, 255, 0.1)',
            }
        }
    },
    Modal: {
        styles: {
            content: {
                backgroundColor: '#1A1B1E',
                border: '1px solid rgba(255,255,255,0.1)',
            },
            header: {
                backgroundColor: '#1A1B1E',
            }
        }
    }
  },
});
