/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1989fa',
        success: '#07c160',
        warning: '#ff976a',
        danger: '#ee0a24',
      }
    },
  },
  plugins: [],
}
