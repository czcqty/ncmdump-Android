import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  // Build to a relative path for Android WebView assets
  base: './',
  build: {
    outDir: '../app/src/main/assets/frontend',
    emptyOutDir: true,
  }
})
