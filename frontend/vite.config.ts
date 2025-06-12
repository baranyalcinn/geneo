import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig(() => {
  return {
    plugins: [
      react(),
    ],
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
    },
    build: {
      // Terser minification
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
      },
      // Output dizini
      outDir: 'build',
      chunkSizeWarningLimit: 800, // KB olarak
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['react', 'react-dom', 'react-router-dom'],
            materialui: ['@mui/material', '@mui/icons-material'],
            utilities: ['axios', 'd3'],
          },
        },
      },
    },
    resolve: {
      alias: {
        '@': resolve(import.meta.dirname, 'src'),
      },
    },
    optimizeDeps: {
      include: ['react', 'react-dom', 'react-router-dom'],
    },
    server: {
      port: 3000,
      open: true,
      cors: true,
      fs: {
        // Proje kök dizinine (bir üst klasör) erişime izin ver
        allow: ['..'],
      },
    },
  };
}); 