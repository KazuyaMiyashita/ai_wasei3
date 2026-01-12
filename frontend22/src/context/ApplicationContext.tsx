import React, {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useState,
  useSyncExternalStore,
} from "react";
import { Application, type ApplicationState } from "../lib/application";

const ApplicationContext = createContext<Application | null>(null);

export const ApplicationProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  // Application is a singleton-like object for the session
  const [application] = useState(() => new Application());

  useEffect(() => {
    application.init().catch(console.error);
  }, [application]);

  return (
    <ApplicationContext.Provider value={application}>
      {children}
    </ApplicationContext.Provider>
  );
};

export function useApplication(): Application {
  const context = useContext(ApplicationContext);
  if (!context) {
    throw new Error(
      "useApplication must be used within an ApplicationProvider",
    );
  }
  return context;
}

export function useApplicationState<T>(
  selector: (state: ApplicationState) => T,
): T {
  const application = useApplication();

  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      // Application emits the new state, but useSyncExternalStore just needs a notification signal.
      return application.subscribe(() => onStoreChange());
    },
    [application],
  );

  const getSnapshot = useCallback(() => {
    return selector(application.getState());
  }, [application, selector]);

  return useSyncExternalStore(subscribe, getSnapshot);
}
