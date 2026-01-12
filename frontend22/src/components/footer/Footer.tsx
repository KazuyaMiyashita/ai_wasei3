import AppStatus from "./AppStatus";
import Notification from "./Notification";

const Footer = () => {
  return (
    <footer className="ui-footer-base flex h-6 shrink-0 items-center justify-between border-t">
      <div className="border-ui-border mr-2 flex max-w-[50%] min-w-0 flex-1 items-center border-r pr-2">
        <Notification />
      </div>
      <AppStatus />
    </footer>
  );
};

export default Footer;
