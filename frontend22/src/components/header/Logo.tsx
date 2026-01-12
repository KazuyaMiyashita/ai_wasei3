const Logo = () => (
  <div className="flex shrink-0 items-center">
    {/* アイコン部分：shrink-0 と aspect-square で正方形を維持 */}
    <div className="flex w-12 justify-center">
      <div className="bg-brand-primary text-brand-text flex aspect-square h-8 w-8 shrink-0 items-center justify-center rounded-md font-bold">
        M
      </div>
    </div>
    {/* 文字部分：hidden sm:block でスマホサイズでは非表示にする */}
    <span className="text-ui-text-main hidden text-sm font-semibold whitespace-nowrap md:block">
      Music Analysis Integration
    </span>
  </div>
);

export default Logo;
