# ⚡ MythicAbilities - Advanced Abilities Plugin for Minecraft 1.21.11

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft"/>
  <img src="https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-red?style=for-the-badge"/>
</p>

<p align="center">
  <b>✨ 10 Unique Abilities | 🎮 Visual Spin System | ⚔️ Combo-Based Triggers | 🎯 Dynamic Particles ✨</b>
</p>

---

## 📋 Table of Contents
- [✨ Features](#-features)
- [🎯 Abilities List](#-abilities-list)
- [⚙️ Installation](#️-installation)
- [🎮 How to Play](#-how-to-play)
- [👑 Admin Commands](#-admin-commands)
- [🎨 Visual Spin System](#-visual-spin-system)
- [⚡ Combo System](#-combo-system)
- [📊 Cooldown Display](#-cooldown-display)
- [🛠️ Configuration](#️-configuration)
- [🔧 Building from Source](#-building-from-source)
- [📝 Permissions](#-permissions)
- [🤝 Contributing](#-contributing)
- [📜 License](#-license)

---

## ✨ Features

<table>
  <tr>
    <td width="33%" align="center">
      <h3>🎯 10 Unique Abilities</h3>
      <p>Each with custom mechanics, particles, and sound effects</p>
    </td>
    <td width="33%" align="center">
      <h3>⚡ Combo System</h3>
      <p>3 hits = Auto-trigger ability</p>
    </td>
    <td width="33%" align="center">
      <h3>🎨 Visual Spin</h3>
      <p>Armor stand animation for first-join abilities</p>
    </td>
  </tr>
  <tr>
    <td align="center">
      <h3>📊 Dynamic Cooldown</h3>
      <p>Real-time progress bar with symbols</p>
    </td>
    <td align="center">
      <h3>👑 Admin Panel</h3>
      <p>Full control with GUI and commands</p>
    </td>
    <td align="center">
      <h3>🌍 World Border</h3>
      <p>20x20 → 20,000x20,000 expansion</p>
    </td>
  </tr>
</table>

---

## 🎯 Abilities List

### 🔥 1. Inferno Touch
> *Domain Expansion - Fire Realm*

| Property | Value |
|----------|-------|
| **Trigger** | 3 Combos |
| **Cooldown** | 30 seconds |
| **Duration** | 10 seconds |
| **Particles** | FLAME, SOUL_FIRE_FLAME, LAVA |

graph LR
    A[Join Server First Time] --> B[Welcome Title]
    B --> C[2 Second Delay]
    C --> D[✨ Visual Spin Starts]
    D --> E[Armor Stand Animation]
    E --> F[Random Ability Unlocked]
    F --> G[Congratulations!]
