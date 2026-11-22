# Third-Party Open-Source Notices

Aquila Office applies the [Apache License 2.0](LICENSE) and incorporates algorithms or UI interaction patterns adapted from the following open-source projects, alongside the runtime dependencies listed below.

---

## 1. Ported / Referenced Open-Source Components

### 1.1 PathFinding.js (`qiao/PathFinding.js`)

- **Purpose**: 2D grid A* pathfinding and line-of-sight path smoothing reference
- **Repository**: <https://github.com/qiao/PathFinding.js>
- **License**: MIT License
- **Copyright**: Copyright (c) 2011-2012 Xueqiao Xu <xueqiaoxu@gmail.com>

```text
MIT License

Copyright (c) 2011-2012 Xueqiao Xu <xueqiaoxu@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

---

### 1.2 kdbush & flatbush (`mourner/kdbush`, `mourner/flatbush`)

- **Purpose**: Flat `TypedArray`-backed static 2D KD-tree / spatial index reference
- **Repositories**: <https://github.com/mourner/kdbush>, <https://github.com/mourner/flatbush>
- **License**: ISC License
- **Copyright**: Copyright (c) 2018-2023 Vladimir Agafonkin

```text
ISC License

Copyright (c) 2018-2023 Vladimir Agafonkin

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY CLAIM, DAMAGES OR ANY
SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE
USE OR PERFORMANCE OF THIS SOFTWARE.
```

---

### 1.3 Fuse.js (`krisk/Fuse`)

- **Purpose**: 32-bit word-parallel Bitap bitmask fuzzy search reference
- **Repository**: <https://github.com/krisk/Fuse>
- **License**: Apache License 2.0
- **Copyright**: Copyright (c) 2012-2024 Kirollos Risk

```text
Apache License, Version 2.0

Copyright (c) 2012-2024 Kirollos Risk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

The full text of the Apache License, Version 2.0 is provided in [LICENSE](LICENSE).

---

### 1.4 cmdk (`pacocoursey/cmdk`)

- **Purpose**: WAI-ARIA `combobox`/`listbox` command palette keyboard interaction structure reference
- **Repository**: <https://github.com/pacocoursey/cmdk>
- **License**: MIT License
- **Copyright**: Copyright (c) 2022 Paco Coursey

```text
MIT License

Copyright (c) 2022 Paco Coursey

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

---

### 1.5 dunce (`kornelski/dunce`)

- **Purpose**: Windows verbatim UNC (`\\?\`) path simplification while preserving standard `260`-char safety bounds
- **Repository**: <https://github.com/kornelski/dunce>
- **License**: `Apache-2.0 OR MIT-0` (reused under Apache-2.0)

---

## 2. Runtime Dependency License Compatibility Summary

| Component | Layer | Version | License | Apache-2.0 Compatibility |
| --- | --- | --- | --- | --- |
| Spring Boot | Backend | `4.0.5` | `Apache-2.0` | Native match; permissive |
| Next.js & React | Frontend | `16.4.6` / `19.2.4` | `MIT` | Permissive; notice retained |
| Lucide (`lucide-react`) | Frontend (Icons) | `^1.14.0` | `ISC` | Permissive; notice retained |
| Tauri (`tauri`) | Desktop Shell | `2` | `Apache-2.0 OR MIT` | Dual permissive; compatible |
| `git2` / `libgit2` | Desktop Shell (Git) | `0.20` | `git2`: `MIT OR Apache-2.0`; `libgit2`: `GPL-2.0-only WITH GCC-exception-2.0` Linking Exception | Compatible via linking exception for compiled binaries |
| Logback | Backend (Logging) | Managed by Spring Boot `4.0.5` | `EPL-2.0 OR LGPL-2.1` dual license | Used unmodified under EPL-2.0; compatible |
| OkHttp (`okhttp3`) | Backend (Outbound HTTP/WS) | `4.12.0` | `Apache-2.0` | Native match; DNS-pinned transport |

---

## Pixel Character, Wardrobe & 2D Office Tilemap Compositor
- Source: `pixel-agents-hq/pixel-agents` (Copyright (c) 2026 Pablo De Lucca)
- License: MIT License (`https://github.com/pixel-agents-hq/pixel-agents`)
- Scope in Aquila Office:
  - Layered sprite recoloring and luminance tinting helpers (`front/lib/pixel-office/colorize.ts`)
  - Scaled offscreen sprite canvas cache and 1px outline synthesizer (`front/lib/pixel-office/sprite-cache.ts`)
  - Synchronously composed `16x32` character renderer and RLE packed wardrobe atlas (`front/lib/pixel-office/character-renderer.ts`, `front/lib/pixel-office/wardrobe-data.ts`)
  - 4-bit wall auto-tiler, marching-squares carpet border resolver, and bubble sprite definitions (`front/lib/pixel-office/office-autotile.ts`, `front/lib/pixel-office/office-pixel-catalog.ts`)
- Character Artwork Credit:
  - *MetroCity - Free Top Down Character Pack* by **JIK-A-4** (`https://jik-a-4.itch.io/metrocity-free-topdown-character-pack`), bundled under the pack's free permissive license alongside `pixel-agents`, paired with original headgear/equipment overlays authored for Aquila Office (`visor`, `headphones`, `droid`, `beret`, `headset`, `crown`).

## Studio Control-Plane UI Patterns
- Source: `paperclipai/paperclip` (Copyright (c) 2025 Paperclip AI)
- License: MIT License (`https://github.com/paperclipai/paperclip`)
- Scope in Aquila Office:
  - OKLab non-wrapping status pill system, semantic Kanban column rail tones (`chrome-kanban-col-*`), 2-pane studio switcher layout, and structured approval card patterns (`front/styles/studio-ui.css`, `front/app/workspaces/page.tsx`, `front/components/workspace/views/WorkspaceTaskBoardView.tsx`, `front/components/workspace/views/WorkspaceApprovalInboxView.tsx`).

## 2D Office Skyline, @4x Furniture Atlas & Atmosphere Themes
- Source: `pixtuoid/pixtuoid` (Copyright (c) 2026 Ivan Wang)
- License: MIT License (`https://github.com/pixtuoid/pixtuoid`)
- Scope in Aquila Office:
  - RLE-packed `@4x` pixel sprite atlas (panoramic skyline buildings, desks, chairs, standing desk, whiteboard, meeting screen, acoustic booths, kitchen/pantry, 4-frame aquarium, office cat & dog sprites, and status glyphs) and 5 developer atmosphere palettes (`tokyo-night`, `catppuccin`, `dracula`, `gruvbox`, `daylight`) in `front/lib/pixel-office/pixtuoid-sprites.ts` and `front/components/workspace/OfficeTilemapCanvas.tsx`.

## Procedural Zone Composition & Live Collaboration Arcs
- Source: `GreenSheep01201/claw-empire`
- License: Apache License 2.0 (`https://github.com/GreenSheep01201/claw-empire`)
- Scope in Aquila Office:
  - Multi-zone parquet atrium layout conventions and live coordinator-to-agent quadratic Bezier collaboration links rendered in `front/components/workspace/OfficeTilemapCanvas.tsx`.

## Cubic-Bezier DAG Edge Path Geometry
- Source: `xyflow/xyflow` (Copyright (c) 2019-2026 webkid GmbH)
- License: MIT License (`https://github.com/xyflow/xyflow`)
- Scope in Aquila Office:
  - Horizontal cubic-Bezier control offset calculation (`calculateControlOffset`, `getHorizontalBezierPath`) adapted from `packages/system/src/utils/edges/bezier-edge.ts` in `front/lib/pixel-office/dag-bezier-edges.ts` and `front/components/workspace/views/WorkspaceOrchestrationView.tsx`.

## Floating Canvas Island Toolbar & Stage Controls
- Source: `excalidraw/excalidraw` (Copyright (c) 2020 Excalidraw)
- License: MIT License (`https://github.com/excalidraw/excalidraw`)
- Scope in Aquila Office:
  - Floating canvas island toolbar grouping and collapsible minimap surface patterns in `front/components/workspace/stage/StageFloatingControls.tsx`, `front/components/workspace/Workspace2DCanvas.tsx`, and `front/styles/studio-ui.css`.

## Assistant Composer & Quick-Start Prompt Chips
- Source: `assistant-ui/assistant-ui` (Copyright (c) 2024 assistant-ui)
- License: MIT License (`https://github.com/assistant-ui/assistant-ui`)
- Scope in Aquila Office:
  - Empty-thread starter prompt chips (`buildAgentQuickPrompts`) and IME-safe `Enter` / `Shift+Enter` composer ergonomics in `front/lib/workspace-formatters.ts` and `front/components/workspace/inspector/WorkspaceContextInspector.tsx`.

## Agent Task Board & Execution Telemetry Layout
- Source: `quentintou/agent-board` (Copyright (c) 2025 Quentin Tou)
- License: MIT License (`https://github.com/quentintou/agent-board`)
- Scope in Aquila Office:
  - Two-column telemetry summary (`ao-telemetry-grid`), task status distribution bar + swatch legend, agent assignee filter, and drag-and-drop column transitions in `front/components/workspace/views/WorkspaceTaskBoardView.tsx` and `front/styles/studio-ui.css`.
