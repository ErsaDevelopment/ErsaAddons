# ErsaHistory

Addon for **ErsaAC** that saves player flag history to a database.

## Why use it?
ErsaAC flags players in real-time, but the default history isn't saved anywhere. This addon writes flags to a database (SQLite or PostgreSQL) so you can check a player's history later, even if they are offline.

## Requirements
* ErsaAC (required)
* Java 17+

## Installation
1. Put `ErsaHistory.jar` into your `plugins` folder.
2. Start the server to generate `config.yml`.
3. Set up your database (SQLite is used by default).
4. Add the `[history]` command marker to your ErsaAC config (see below).
5. Use `/ersa history reload` or restart the server.

## Configuring ErsaAC (checks.yml)
ErsaHistory doesn't track flags automatically. You need to tell ErsaAC when to trigger it by adding the `[history]` marker to your punishment commands in `checks.yml`.

**Example:**
```yaml
  Hitbox:
    remove-vl: 120
    checks:
      - "Reach"
      - "Hitbox"
    commands:
      - "2:2 [alert]"
      - "2:2 [history]" # <--- Add this line