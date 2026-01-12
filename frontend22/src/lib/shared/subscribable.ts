/**
 * Interface for objects that can be subscribed to.
 * Designed to work with React's useSyncExternalStore.
 */
export interface ISubscribable<T> {
  /**
   * Subscribes a listener to updates.
   * @param listener The callback to be invoked when the state changes.
   * @returns A function to unsubscribe the listener.
   */
  subscribe(listener: (state: T) => void): () => void;

  /**
   * Returns the current state.
   */
  getState(): T;
}

/**
 * Base class for subscribable models.
 * Implements state caching to ensure referential stability for useSyncExternalStore.
 */
export abstract class Subscribable<T> implements ISubscribable<T> {
  protected listeners: Set<(state: T) => void> = new Set();

  // Cache the current state to ensure referential equality between renders if no change occurred.
  // We initialize it lazily or allow subclasses to set it.
  protected _state: T | null = null;

  subscribe(listener: (state: T) => void): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  protected emit(state: T): void {
    this._state = state;
    for (const listener of this.listeners) {
      listener(state);
    }
  }

  /**
   * Returns the current state.
   * Subclasses should rely on emit() updating _state, or implement logic to ensure stability.
   */
  getState(): T {
    if (this._state === null) {
      // Fallback for initial state before first emit, if subclass relies on this.
      // Ideally subclasses initialize _state in constructor.
      throw new Error(
        "State not initialized. Subclasses should initialize _state or override getState.",
      );
    }
    return this._state;
  }
}
