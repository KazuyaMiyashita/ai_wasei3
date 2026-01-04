import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.tsx";
import { AudioProvider } from "./context/AudioContext.tsx";
import { ConfirmProvider } from "./context/ConfirmContext.tsx";
import { NotificationProvider } from "./context/NotificationContext.tsx";
import { ApiProvider } from "./hooks/api/useApi";
import "./index.css";

const rootElement = document.getElementById("root");
if (rootElement) {
  ReactDOM.createRoot(rootElement).render(
    <React.StrictMode>
      <NotificationProvider>
        <ConfirmProvider>
          <ApiProvider>
            <AudioProvider>
              <App />
            </AudioProvider>
          </ApiProvider>
        </ConfirmProvider>
      </NotificationProvider>
    </React.StrictMode>,
  );
}
