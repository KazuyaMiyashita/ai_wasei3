import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// 開発環境で MusicIcon で利用している音楽フォントが何度も再読み込みされてちらつきが発生するのを防ぐ。
// このちらつきは必ずしもコンポーネントの再描画時に発生するわけではなく、ウィンドウのリサイズによって生じていた。
const fontHeaderPlugin = () => ({
  name: "adjust-font-headers",
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      if (req.url?.includes("bravura.woff2")) {
        res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
      }
      next();
    });
  },
});

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), fontHeaderPlugin()],
  server: {
    proxy: {
      "/score": "http://127.0.0.1:8080",
      "/perform": "http://127.0.0.1:8080",
      "/partwise": "http://127.0.0.1:8080",
    },
  },
});
