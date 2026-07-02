import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    allowedHosts: [
      "valuables-underling-datebook.ngrok-free.dev",
      ".ngrok-free.app",
      ".ngrok-free.dev"
    ]
  }
});
