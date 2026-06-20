# TaCZ Killfeed 🎯

**CS2-style killfeed overlay for Minecraft with TaCZ guns**

![GitHub Release](https://img.shields.io/github/v/release/kavarobot3/taczkillfeed)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-blue)
![Forge](https://img.shields.io/badge/Forge-47.4.16-orange)
![TaCZ](https://img.shields.io/badge/TaCZ-1.1.8--hotfix-red)

---

## 🇷🇺 Описание

Мод добавляет в правый верхний угол экрана **киллфид в стиле CS2** для модификации **TaCZ** (Timeless and Classics Guns).

Вместо скучного чата — красивые уведомления об убийствах с иконками оружия, цветами команд и индикаторами хедшотов.

### Возможности

| Фича | Описание |
|------|----------|
| 🔫 **Иконка оружия** | Отрисовка предмета из TaCZ (с обвесами, скинами, стикерами) |
| 🎯 **Headshot индикатор** | Золотой круг с крестом (как в CS2) |
| 🔥 **Киллстрики** | `2x`, `3x` после ника, `ON FIRE` с 5+ убийств |
| 🤝 **Ассисты** | Автоопределение помощи — строка `A: Игрок` под киллом |
| 🗡️ **Нож/Граната** | Разные цвета фона: синий для ножа, рыжий для гранаты |
| 👥 **Цвета команд** | Жёлтый (террористы) / синий (контр-террористы) / серый |
| ✨ **Затухание** | Плавное исчезновение за 1 сек до конца (6 сек показа) |
| 🔴 **Подсветка** | Красная рамка если вы — убийца или жертва |

### Как это выглядит

```
┌────────────────────────┐
│ Killer 3x 🔫 Victim  ⦿ │  ← килл с хедшотом
│ A: Assistant           │  ← ассист (серый, мелкий)
└────────────────────────┘
```

- Голубой фон → убийство из гранаты
- Тёмно-синий фон → убийство ножом
- Красная рамка → вы участвуете

---

## 🇬🇧 Description

A **CS2-style killfeed** overlay for the **TaCZ** (Timeless and Classics Guns) mod in Minecraft.

No more squinting at chat — get clean, real-time kill notifications in the top-right corner with weapon icons, team colors, headshot indicators, streaks, and assists.

### Features

| Feature | Description |
|---------|-------------|
| 🔫 **Weapon icon** | Renders the actual TaCZ gun with attachments, skins, stickers via `renderFakeItem` |
| 🎯 **Headshot indicator** | Gold circle with cross (CS2 style) |
| 🔥 **Kill streaks** | `2x`, `3x` after killer name, `ON FIRE` at 5+ kills |
| 🤝 **Assists** | Auto-detected — shows `A: Player` under the kill line (5 sec damage window) |
| 🗡️ **Knife/Grenade** | Different background colors: blue for knife, orange for grenade |
| 👥 **Team colors** | Yellow (terrorists) / blue (counter-terrorists) / gray |
| ✨ **Fade out** | Smooth 1-second fade after 6 seconds of display |
| 🔴 **Highlight** | Red border if you are the killer or the victim |

### Preview

```
┌────────────────────────┐
│ Killer 3x 🔫 Victim  ⦿ │  ← headshot kill
│ A: Assistant           │  → assist line (gray, small)
└────────────────────────┘
```

- Blue tint background → knife kill
- Orange tint background → grenade kill  
- Red border → you are involved

---

## 📦 Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | 1.20.1 |
| Forge | 47.4.16 |
| TaCZ | 1.1.8-hotfix |

## ⚙️ Installation

1. Install [Minecraft Forge 47.4.16](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
2. Install [TaCZ 1.1.8-hotfix](https://github.com/tacz-plus/TaCZ/releases)
3. Download `tacz_killfeed.jar` from [Releases](https://github.com/kavarobot3/taczkillfeed/releases)
4. Place it in your `mods` folder
5. Launch the game — it works out of the box, no configuration needed

## 🔧 Building from source

```bash
git clone https://github.com/kavarobot3/taczkillfeed.git
cd taczkillfeed
./gradlew build
# Output: build/libs/tacz_killfeed-1.1.0.jar
```

---

## 📁 Project structure

```
taczkillfeed/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/main/
    ├── java/net/example/taczkillfeed/
    │   ├── TaczKillfeed.java          # Mod entry point
    │   ├── client/
    │   │   ├── ClientAccess.java       # DistExecutor bridge
    │   │   ├── ClientModEvents.java    # Registers overlay renderer
    │   │   └── KillfeedOverlay.java    # Rendering & entry lifecycle
    │   ├── event/
    │   │   └── KillHandler.java        # Event listener (streaks, assists, detection)
    │   └── network/
    │       ├── KillfeedPacket.java     # Network packet definition
    │       └── ModMessages.java        # Channel setup
    └── resources/
        ├── META-INF/mods.toml
        ├── pack.mcmeta
        └── assets/tacz_killfeed/textures/hud/default_icon.png
```

---

## 📄 License

MIT — free to use, modify, and distribute.
