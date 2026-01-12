import { useEffect, useState } from "react";
import { useApplication } from "../context/ApplicationContext";
import Center from "./center/Center";
import Footer from "./footer/Footer";
import Header from "./header/Header";
import { Snackbar } from "./ui/Snackbar";

function Layout() {
  const application = useApplication();
  const [snackbarState, setSnackbarState] = useState<{
    message: string;
    isOpen: boolean;
  }>({ message: "", isOpen: false });

  useEffect(() => {
    const unsubscribe = application.logger.subscribe((state) => {
      if (state.lastLog && state.lastLog.level === "error") {
        setSnackbarState({
          message: `[${state.lastLog.sender}] ${state.lastLog.message}`,
          isOpen: true,
        });
      }
    });
    return unsubscribe;
  }, [application]);

  return (
    <div className="bg-ui-bg text-ui-text-main flex h-screen w-screen flex-col overflow-hidden">
      <Header />
      <Center />
      <Footer />
      <Snackbar
        message={snackbarState.message}
        isOpen={snackbarState.isOpen}
        onClose={() => setSnackbarState((prev) => ({ ...prev, isOpen: false }))}
      />
    </div>
  );
}

export default Layout;
