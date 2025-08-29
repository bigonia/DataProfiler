// vite.config.ts
import { defineConfig } from "file:///D:/src/DBCrawlerV3/frontend/node_modules/vite/dist/node/index.js";
import vue from "file:///D:/src/DBCrawlerV3/frontend/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { resolve } from "path";
var __vite_injected_original_dirname = "D:\\src\\DBCrawlerV3\\frontend";
var vite_config_default = defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": resolve(__vite_injected_original_dirname, "src")
    }
  },
  server: {
    port: 5173,
    host: true,
    open: true,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
        // rewrite: (path) => path.replace(/^/api/, '')
      }
    }
  },
  build: {
    outDir: "dist",
    sourcemap: true,
    // Enable sourcemap for debugging
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["vue", "vue-router", "pinia"],
          elementPlus: ["element-plus", "@element-plus/icons-vue"],
          charts: ["echarts", "vue-echarts"]
        }
      }
    }
  },
  // Enable debug mode for development
  define: {
    __VUE_OPTIONS_API__: true,
    __VUE_PROD_DEVTOOLS__: false
  },
  // Optimize dependencies for better debugging
  optimizeDeps: {
    include: ["vue", "vue-router", "pinia", "element-plus"]
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxzcmNcXFxcREJDcmF3bGVyVjNcXFxcZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXHNyY1xcXFxEQkNyYXdsZXJWM1xcXFxmcm9udGVuZFxcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovc3JjL0RCQ3Jhd2xlclYzL2Zyb250ZW5kL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IHsgcmVzb2x2ZSB9IGZyb20gJ3BhdGgnXG5cbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xuICBwbHVnaW5zOiBbdnVlKCldLFxuICByZXNvbHZlOiB7XG4gICAgYWxpYXM6IHtcbiAgICAgICdAJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMnKVxuICAgIH1cbiAgfSxcbiAgc2VydmVyOiB7XG4gICAgcG9ydDogNTE3MyxcbiAgICBob3N0OiB0cnVlLFxuICAgIG9wZW46IHRydWUsXG4gICAgcHJveHk6IHtcbiAgICAgICcvYXBpJzoge1xuICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODAnLFxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXG4gICAgICAgIC8vIHJld3JpdGU6IChwYXRoKSA9PiBwYXRoLnJlcGxhY2UoL14vYXBpLywgJycpXG4gICAgICB9XG4gICAgfVxuICB9LFxuICBidWlsZDoge1xuICAgIG91dERpcjogJ2Rpc3QnLFxuICAgIHNvdXJjZW1hcDogdHJ1ZSwgLy8gRW5hYmxlIHNvdXJjZW1hcCBmb3IgZGVidWdnaW5nXG4gICAgcm9sbHVwT3B0aW9uczoge1xuICAgICAgb3V0cHV0OiB7XG4gICAgICAgIG1hbnVhbENodW5rczoge1xuICAgICAgICAgIHZlbmRvcjogWyd2dWUnLCAndnVlLXJvdXRlcicsICdwaW5pYSddLFxuICAgICAgICAgIGVsZW1lbnRQbHVzOiBbJ2VsZW1lbnQtcGx1cycsICdAZWxlbWVudC1wbHVzL2ljb25zLXZ1ZSddLFxuICAgICAgICAgIGNoYXJ0czogWydlY2hhcnRzJywgJ3Z1ZS1lY2hhcnRzJ11cbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cbiAgfSxcbiAgLy8gRW5hYmxlIGRlYnVnIG1vZGUgZm9yIGRldmVsb3BtZW50XG4gIGRlZmluZToge1xuICAgIF9fVlVFX09QVElPTlNfQVBJX186IHRydWUsXG4gICAgX19WVUVfUFJPRF9ERVZUT09MU19fOiBmYWxzZVxuICB9LFxuICAvLyBPcHRpbWl6ZSBkZXBlbmRlbmNpZXMgZm9yIGJldHRlciBkZWJ1Z2dpbmdcbiAgb3B0aW1pemVEZXBzOiB7XG4gICAgaW5jbHVkZTogWyd2dWUnLCAndnVlLXJvdXRlcicsICdwaW5pYScsICdlbGVtZW50LXBsdXMnXVxuICB9XG59KSJdLAogICJtYXBwaW5ncyI6ICI7QUFBMlEsU0FBUyxvQkFBb0I7QUFDeFMsT0FBTyxTQUFTO0FBQ2hCLFNBQVMsZUFBZTtBQUZ4QixJQUFNLG1DQUFtQztBQUt6QyxJQUFPLHNCQUFRLGFBQWE7QUFBQSxFQUMxQixTQUFTLENBQUMsSUFBSSxDQUFDO0FBQUEsRUFDZixTQUFTO0FBQUEsSUFDUCxPQUFPO0FBQUEsTUFDTCxLQUFLLFFBQVEsa0NBQVcsS0FBSztBQUFBLElBQy9CO0FBQUEsRUFDRjtBQUFBLEVBQ0EsUUFBUTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBLElBQ04sT0FBTztBQUFBLE1BQ0wsUUFBUTtBQUFBLFFBQ04sUUFBUTtBQUFBLFFBQ1IsY0FBYztBQUFBO0FBQUEsTUFFaEI7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUFBLEVBQ0EsT0FBTztBQUFBLElBQ0wsUUFBUTtBQUFBLElBQ1IsV0FBVztBQUFBO0FBQUEsSUFDWCxlQUFlO0FBQUEsTUFDYixRQUFRO0FBQUEsUUFDTixjQUFjO0FBQUEsVUFDWixRQUFRLENBQUMsT0FBTyxjQUFjLE9BQU87QUFBQSxVQUNyQyxhQUFhLENBQUMsZ0JBQWdCLHlCQUF5QjtBQUFBLFVBQ3ZELFFBQVEsQ0FBQyxXQUFXLGFBQWE7QUFBQSxRQUNuQztBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUFBO0FBQUEsRUFFQSxRQUFRO0FBQUEsSUFDTixxQkFBcUI7QUFBQSxJQUNyQix1QkFBdUI7QUFBQSxFQUN6QjtBQUFBO0FBQUEsRUFFQSxjQUFjO0FBQUEsSUFDWixTQUFTLENBQUMsT0FBTyxjQUFjLFNBQVMsY0FBYztBQUFBLEVBQ3hEO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFtdCn0K
