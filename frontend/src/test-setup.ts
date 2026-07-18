// Vitest runs on Node, whose built-in `localStorage` global (Node >= 22)
// shadows jsdom's and is non-functional without a --localstorage-file flag.
// Install a spec-compliant in-memory Storage so app code and tests can use
// the standard API.
class MemoryStorage implements Storage {
  private readonly entries = new Map<string, string>();

  get length(): number {
    return this.entries.size;
  }

  clear(): void {
    this.entries.clear();
  }

  getItem(key: string): string | null {
    return this.entries.get(key) ?? null;
  }

  key(index: number): string | null {
    return [...this.entries.keys()][index] ?? null;
  }

  removeItem(key: string): void {
    this.entries.delete(key);
  }

  setItem(key: string, value: string): void {
    this.entries.set(key, String(value));
  }
}

const memoryStorage = new MemoryStorage();

for (const target of [globalThis, window]) {
  Object.defineProperty(target, 'localStorage', {
    value: memoryStorage,
    writable: true,
    configurable: true,
  });
}
