import assert from "node:assert/strict";
import test from "node:test";
import {
  clampInspectorWidth,
  formatCommandShortcut,
  formatViewIndexShortcut,
  isEditableEventTarget,
  matchViewSwitchShortcut,
  resolveShortcutStyle,
} from "./workspace-shortcuts.ts";

test("resolveShortcutStyle distinguishes desktop/browser and macOS/non-macOS", () => {
  assert.equal(resolveShortcutStyle(true, "Mozilla/5.0 (Macintosh; Intel Mac OS X)"), "desktop-mac");
  assert.equal(resolveShortcutStyle(true, "Mozilla/5.0 (Windows NT 10.0)"), "desktop");
  assert.equal(resolveShortcutStyle(false, "Mozilla/5.0 (Macintosh; Intel Mac OS X)"), "browser-mac");
  assert.equal(resolveShortcutStyle(false, "Mozilla/5.0 (X11; Linux x86_64)"), "browser");
});

test("formatCommandShortcut and formatViewIndexShortcut render platform labels", () => {
  assert.equal(formatCommandShortcut("desktop-mac", "K"), "⌘K");
  assert.equal(formatCommandShortcut("browser-mac", "K"), "⌘K");
  assert.equal(formatCommandShortcut("desktop", "B"), "Ctrl+B");
  assert.equal(formatViewIndexShortcut("desktop-mac", 1), "⌘1");
  assert.equal(formatViewIndexShortcut("browser-mac", 2), "⌥2");
  assert.equal(formatViewIndexShortcut("browser", 5), "Alt+5");
});

test("matchViewSwitchShortcut respects modifier contracts and view bounds", () => {
  assert.equal(
    matchViewSwitchShortcut("desktop-mac", { key: "3", metaKey: true, ctrlKey: false, altKey: false, shiftKey: false }, 5),
    2,
  );
  assert.equal(
    matchViewSwitchShortcut("browser-mac", { key: "3", metaKey: true, ctrlKey: false, altKey: false, shiftKey: false }, 5),
    null,
  );
  assert.equal(
    matchViewSwitchShortcut("browser-mac", { key: "3", metaKey: false, ctrlKey: false, altKey: true, shiftKey: false }, 5),
    2,
  );
  assert.equal(
    matchViewSwitchShortcut("desktop", { key: "9", metaKey: false, ctrlKey: true, altKey: false, shiftKey: false }, 5),
    null,
  );
  assert.equal(
    matchViewSwitchShortcut("desktop", { key: "2", metaKey: false, ctrlKey: true, altKey: false, shiftKey: true }, 5),
    null,
  );
});

test("clampInspectorWidth enforces [280, 520] and viewport safety bounds", () => {
  assert.equal(clampInspectorWidth(Number.NaN), 320);
  assert.equal(clampInspectorWidth(120), 280);
  assert.equal(clampInspectorWidth(900), 520);
  assert.equal(clampInspectorWidth(420, 1280), 420);
  assert.equal(clampInspectorWidth(500, 900), 380);
  assert.equal(clampInspectorWidth(500, 720), 280);
});

test("isEditableEventTarget detects text inputs and open dialogs", () => {
  assert.equal(isEditableEventTarget(null), false);
  assert.equal(
    isEditableEventTarget({ tagName: "INPUT", isContentEditable: false, closest: () => null } as unknown as EventTarget),
    true,
  );
  assert.equal(
    isEditableEventTarget({ tagName: "DIV", isContentEditable: true, closest: () => null } as unknown as EventTarget),
    true,
  );
  assert.equal(
    isEditableEventTarget({
      tagName: "BUTTON",
      isContentEditable: false,
      closest: (selector: string) => (selector.includes('[role="dialog"]') ? ({} as Element) : null),
    } as unknown as EventTarget),
    true,
  );
  assert.equal(
    isEditableEventTarget({ tagName: "BUTTON", isContentEditable: false, closest: () => null } as unknown as EventTarget),
    false,
  );
});
