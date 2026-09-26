import Phaser from "phaser";
import {
  createWorkspaceContextMenuState,
  type WorkspaceContextMenuState,
  type WorkspaceSceneEventBus,
  type WorkspaceSceneNode,
  type WorkspaceViewport,
} from "@/lib/workspace-2d";

export type Workspace2DGameOptions = {
  parent: HTMLElement;
  viewport: WorkspaceViewport;
  nodes: WorkspaceSceneNode[];
  selectedNodeId: string | null;
  eventBus: WorkspaceSceneEventBus;
  onSelectNode: (node: WorkspaceSceneNode) => void;
  onContextMenu: (menu: WorkspaceContextMenuState) => void;
};

type PhaserPointer = {
  x: number;
  y: number;
  rightButtonDown(): boolean;
};

export function createWorkspace2DGame(options: Workspace2DGameOptions): Phaser.Game {
  class Workspace2DScene extends Phaser.Scene {
    constructor() {
      super({ key: "Workspace2DScene" });
    }

    create() {
      this.cameras.main.setBounds(0, 0, options.viewport.worldWidth, options.viewport.worldHeight);
      this.cameras.main.setZoom(options.viewport.zoom);
      this.cameras.main.scrollX = options.viewport.x;
      this.cameras.main.scrollY = options.viewport.y;

      drawGrid(this, options.viewport);
      options.nodes.forEach((node) => this.drawNode(node));
    }

    private drawNode(node: WorkspaceSceneNode) {
      const fillColor = node.kind === "agent" ? 0x5c6bc0 : 0x4d5cb5;
      const strokeColor = node.id === options.selectedNodeId ? 0xf8f9fa : 0x343a40;
      const avatar = this.add.circle(node.x, node.y, 28, fillColor, 0.92);
      avatar.setStrokeStyle(3, strokeColor, 0.95);
      avatar.setInteractive({ useHandCursor: true });
      avatar.on("pointerdown", (pointer: PhaserPointer) => {
        if (pointer.rightButtonDown()) {
          options.onContextMenu(
            createWorkspaceContextMenuState({
              nodeId: node.id,
              kind: node.kind,
              screenX: pointer.x,
              screenY: pointer.y,
            }),
          );
          return;
        }

        options.onSelectNode(node);
        if (node.kind === "agent") {
          options.eventBus.emit({
            type: "avatar.select",
            agentId: Number(node.id.replace("agent-", "")),
          });
        }
      });

      this.add
        .text(node.x, node.y - 5, node.kind === "agent" ? "A" : "M", {
          color: "#18191D",
          fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
          fontSize: "18px",
          fontStyle: "700",
        })
        .setOrigin(0.5);
      this.add
        .text(node.x, node.y + 40, node.label, {
          color: "#f8f9fa",
          fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
          fontSize: "13px",
          fontStyle: "700",
        })
        .setOrigin(0.5, 0);
    }
  }

  return new Phaser.Game({
    type: Phaser.AUTO,
    parent: options.parent,
    width: options.viewport.width,
    height: options.viewport.height,
    backgroundColor: "#18191D",
    scene: Workspace2DScene,
  });
}

function drawGrid(scene: Phaser.Scene, viewport: WorkspaceViewport) {
  const grid = scene.add.graphics();
  grid.lineStyle(1, 0x5c6bc0, 0.16);
  for (let x = 0; x <= viewport.worldWidth; x += 80) {
    grid.moveTo(x, 0);
    grid.lineTo(x, viewport.worldHeight);
  }
  for (let y = 0; y <= viewport.worldHeight; y += 80) {
    grid.moveTo(0, y);
    grid.lineTo(viewport.worldWidth, y);
  }
  grid.strokePath();
  grid.lineStyle(2, 0x5c6bc0, 0.34);
  grid.strokeRect(24, 24, viewport.worldWidth - 48, viewport.worldHeight - 48);
}
