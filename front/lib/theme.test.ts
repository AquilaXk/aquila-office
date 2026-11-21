import assert from "node:assert/strict";
import test from "node:test";
import {
  THEME_STORAGE_KEY,
  applyTheme,
  normalizeThemeMode,
  readStoredTheme,
  toggleTheme,
} from "./theme.ts";

class MemoryStorage {
  private readonly values = new Map<string, string>();
  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }
  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}

test("theme helpers normalize, read, apply, and toggle dark/light modes", () => {
  assert.equal(normalizeThemeMode("light"), "light");
  assert.equal(normalizeThemeMode("dark"), "dark");
  assert.equal(normalizeThemeMode("invalid"), "dark");

  const storage = new MemoryStorage();
  assert.equal(readStoredTheme(storage), "dark");
  storage.setItem(THEME_STORAGE_KEY, "light");
  assert.equal(readStoredTheme(storage), "light");

  assert.equal(applyTheme("light", false), "light");
  assert.equal(toggleTheme("light"), "dark");
  assert.equal(toggleTheme("dark"), "light");
});
