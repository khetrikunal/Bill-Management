/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: { navy: { 900: '#0f1b33', 800: '#16264a', 700: '#1f3566' } },
    },
  },
  plugins: [],
}
