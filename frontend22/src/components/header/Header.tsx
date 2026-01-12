import AudioControlBar from "./AudioControlBar";
import Logo from "./Logo";
import Menubar from "./Menubar";
import ScoreControlBar from "./ScoreControlBar";

// --- メインコンポーネント ---

const Header = () => {
  return (
    <header className="ui-header-base flex h-12 items-center justify-between border-b">
      <Logo />
      <Menubar />
      <div className="flex flex-1 items-center justify-between px-2">
        <div /> {/* spacing */}
        <ScoreControlBar />
        <AudioControlBar />
        <div /> {/* spacing */}
      </div>
    </header>
  );
};

export default Header;
