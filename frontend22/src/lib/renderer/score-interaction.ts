export function getClickedElementId(
  target: EventTarget | null,
  containerElement: HTMLElement | null,
): string | undefined {
  if (!target || !containerElement) return undefined;

  const targetEl = target as Element;
  let targetId: string | undefined;

  if (targetEl.classList.contains("hitbox")) {
    const corresp = targetEl.getAttribute("corresp");
    if (corresp) {
      targetId = corresp.startsWith("#") ? corresp.substring(1) : corresp;
    }
  } else {
    let current: Element | null = targetEl;
    while (current && current !== containerElement) {
      if (current.id) {
        targetId = current.id;
        break;
      }
      if (current.parentElement) {
        current = current.parentElement;
      } else {
        break;
      }
    }
  }
  return targetId;
}
