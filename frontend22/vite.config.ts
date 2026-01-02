import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/score": "http://127.0.0.1:8080",
      "/perform": "http://127.0.0.1:8080",
      "/partwise": "http://127.0.0.1:8080",
    },
  },
});
