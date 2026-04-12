# C4-Pro


## 📦 模组介绍

### 1. 🎯 模组定位  
本模组由 **qisumei** 的 C4 模组移植、改造并深度优化而来。在保留原版玩法精髓的基础上，针对其机制与体验上的不足进行了大量重构与增强，旨在提供更稳定、更灵活、更适合多人游戏环境的 C4 机制。

---

### 2. 💣 玩法说明  
- **安装 C4**：手持 C4，对准允许放置的方块，**持续按住右键 4 秒**即可完成安装。  
- **拆除 C4**：使用 **剪刀** 对准已安装的 C4，**持续按住右键 4 秒**即可拆除。  
- **爆炸倒计时**：C4 安装后 **40 秒** 自动爆炸，期间会通过游戏内提示进行预警。

---

### 3. ⚙️ 核心改动  
- **实体化重构**：将 C4 从方块改为实体，解决了原版中因游戏模式限制导致的拆装问题，同时便于服务器进行实体清理与管理。  
- **合法方块配置**：支持自定义可安装 C4 的方块类型，只有在配置名单内的方块上才允许安装，提升玩法可控性。  
- **专用拆除工具**：C4 仅能使用剪刀拆除，避免误操作或其他工具干扰。  
- **记分板状态反馈**：C4 的安装、爆炸、拆除状态会实时同步至记分板，便于地图或服务器进行逻辑联动（详见“记分板联动”部分）。

---

### 4. 📊 记分板联动  
C4 的状态将记录在玩家 `#C4` 的记分板项 `c4_condition` 中，状态值含义如下：

| 状态值 | 含义 |
|--------|------|
| 1 | 已安装 C4 |
| 2 | C4 已爆炸 |
| 3 | C4 已拆除 |

> 📌 **注意**：每局游戏开始时，需手动将该值重置为 `0`，以确保状态判断准确。

---

### 5. 🐛 问题反馈  
如在使用过程中遇到任何问题，或希望提出建议，欢迎加入开发群进行反馈与交流。

---

## 📦 Mod Introduction

### 1. 🎯 Positioning  
This mod is a **fork, enhancement, and optimization** of the original C4 mod created by **qisumei**. While retaining the core mechanics, it addresses many limitations of the original version through extensive refactoring and improvements, aiming to deliver a more stable, flexible, and multiplayer-friendly C4 experience.

---

### 2. 💣 Gameplay  
- **Planting C4**: Hold the C4 in hand, aim at a valid block, and **hold the right button for 4 seconds** to plant it.  
- **Defusing C4**: Use **shears** on a planted C4 and **hold the right button for 4 seconds** to defuse it.  
- **Detonation Timer**: The C4 will explode **40 seconds after being planted**, with in-game alerts throughout the countdown.

---

### 3. ⚙️ Key Changes  
- **Entity-Based Design**: The C4 is now an entity instead of a block, ensuring proper planting and defusing mechanics regardless of game mode, and allowing easier server-side cleanup.  
- **Configurable Valid Blocks**: Only blocks specified in the configuration can have C4 planted on them, offering better control over gameplay scenarios.  
- **Dedicated Defusal Tool**: C4 can only be defused using shears, preventing accidental interactions or interference from other tools.  
- **Scoreboard Integration**: The status of C4 is synchronized to the scoreboard, enabling seamless integration with maps or server logic (see “Scoreboard Integration” for details).

---

### 4. 📊 Scoreboard Integration  
C4 status is stored in the player’s `#C4` scoreboard under the objective `c4_condition`. The possible values are:

| Value | Status |
|-------|--------|
| 1 | C4 Planted |
| 2 | C4 Exploded |
| 3 | C4 Defused |

> 📌 **Note**: At the start of each round, this value should be manually reset to `0` to ensure accurate state tracking.

---

### 5. 🐛 Feedback & Support  
If you encounter any issues or have suggestions, feel free to join our development group for feedback and support.
