import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./app/**/*.{js,ts,jsx,tsx,mdx}', './components/**/*.{js,ts,jsx,tsx,mdx}', './pages/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        background: '#0B1020',
        card: '#111827',
        primary: '#7C5CFC',
        accent: '#22D3EE',
        success: '#34D399',
        warning: '#FBBF24',
        error: '#F87171',
        text: '#F8FAFC',
        secondary: '#94A3B8'
      },
      boxShadow: {
        glow: '0 0 0 1px rgba(124, 92, 252, 0.2), 0 18px 45px rgba(124, 92, 252, 0.18)',
        soft: '0 8px 30px rgba(15, 23, 42, 0.35)'
      },
      backgroundImage: {
        grid: 'radial-gradient(circle at 1px 1px, rgba(148,163,184,0.12) 1px, transparent 0)'
      }
    }
  },
  plugins: []
};

export default config;
