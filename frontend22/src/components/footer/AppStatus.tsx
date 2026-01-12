import { useState } from "react";
import { cn } from "../../utils";

const AppStatus = () => {
  const [isConnected, setIsConnected] = useState<boolean>(true);

  const handleClick = () => {
    setIsConnected((isConnected) => {
      return !isConnected;
    });
  };

  return (
    <div className="flex items-center">
      <div className="border-ui-border text-text-muted mr-2 border-r pr-2">
        Version 0.1.0
      </div>
      <button type="button" onClick={handleClick}>
        <div className="flex items-center gap-1">
          <div
            className={cn(
              "h-2 w-2 rounded-full",
              isConnected ? "bg-success" : "bg-error",
            )}
          />
          <span
            className={cn(
              isConnected ? "text-success" : "text-error font-bold",
            )}
          >
            {isConnected ? "Connected" : "Disconnected"}
          </span>
        </div>
      </button>
    </div>
  );
};

export default AppStatus;
