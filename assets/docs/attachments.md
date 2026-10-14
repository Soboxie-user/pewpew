# Attachments (`type: ATTACHMENT`)

Plus the [common fields](common-fields.md). An attachment fits a gun slot listed in the gun's `allowedAttachmentSlots`.
Players fit them in the attachment bench GUI (swap-hands on a gun). Stat lines render automatically on the item.

Every attachment needs `attachmentType`, which selects the slot and the available modifier fields.

| Field            | Type | Required | Description                               |
|------------------|------|----------|-------------------------------------------|
| `attachmentType` | enum | yes      | `SCOPE`, `BARREL`, `GRIP`, or `MAGAZINE`. |

Modifiers are **multipliers** unless stated otherwise: `1.0` = no change, `1.25` = +25%, `0.75` = −25%.

## SCOPE

Aim-down-sights optics. ADS is triggered by holding sneak.

| Field                 | Type   | Default | Description                                                                    |
|-----------------------|--------|---------|--------------------------------------------------------------------------------|
| `zoom`                | double | `1.0`   | Zoom strength while aiming (FOV pinch). `1.0` = none.                          |
| `adsSpeedModifier`    | double | `1.0`   | Reserved for aim-in speed tuning.                                              |
| `aimSpreadMultiplier` | double | `0.0`   | Spread multiplier while scoped. `0.0` = perfectly accurate, `1.0` = unchanged. |
| `aimRecoilMultiplier` | double | `0.0`   | Recoil multiplier while scoped. `0.0` = no kick.                               |

## BARREL

| Field            | Type   | Default | Description             |
|------------------|--------|---------|-------------------------|
| `damageModifier` | double | `1.0`   | Multiplies base damage. |
| `rangeModifier`  | double | `1.0`   | Multiplies range.       |

## GRIP

| Field            | Type   | Default | Description                                           |
|------------------|--------|---------|-------------------------------------------------------|
| `recoilModifier` | double | `1.0`   | Multiplies both recoil kick and spread. `0.5` = half. |

## MAGAZINE

| Field            | Type   | Default | Description                                                     |
|------------------|--------|---------|-----------------------------------------------------------------|
| `ammoBonus`      | int    | `0`     | Rounds added to the gun's `maxAmmo`. Can be negative.           |
| `reloadModifier` | double | `1.0`   | Multiplies reload time. `0.6` = 40% faster, `1.4` = 40% slower. |

## Example

```yaml
extended_mag:
  type: ATTACHMENT
  name: "<gray>Extended Magazine"
  itemModel: "minecraft:iron_ingot"
  attachmentType: MAGAZINE
  ammoBonus: 10
  reloadModifier: 1.15
```

## Custom slots

Slot types are not limited to the four above. Every name written under `slots` in `attachment-menu.yml`
becomes a slot type, which you can then use as `attachmentType` on an attachment and in
`allowedAttachmentSlots` / `defaultAttachments` on a gun. Names may use letters, digits, `_`, `.` and `-`.
A gun holds one attachment per slot type.

That file also lays out the menu: `pattern` is a grid of characters (nine per row, `rows` rows), where `-`
is a mask tile, `G` is the gun preview, and every other character is a slot defined under `slots`. Run
`/pewpew reload` after editing it. See the comments in the generated file.

Each slot shows `empty-icon` when the gun has the slot but nothing is fitted, and `unavailable-icon` (one
global entry, optionally overridden per slot) when the gun does not have that slot at all.

## Modifiers

Any attachment, of any slot type, may have a `modifiers` block. Slot types that are not one of the four
above have no fields of their own, so `modifiers` is all they do.

```yaml
tactical_silencer:
  type: ATTACHMENT
  name: "<gray>Tactical Silencer"
  itemModel: "minecraft:iron_ingot"
  attachmentType: MUZZLE
  modifiers:
    range: 0.95
    recoil: 0.85
    max_ammo_bonus: 4
```

Each stat takes a multiplier (`damage: 1.1` or `damage_multiplier: 1.1`) or a flat addition
(`damage_bonus: 2`). Additions are applied first, then multipliers, across every fitted attachment:
`(base + additions) * multipliers`. `max_ammo` is the exception: written bare it is an addition.

| Stat               | Acts on                                                                        |
|--------------------|--------------------------------------------------------------------------------|
| `damage`           | Base damage.                                                                   |
| `headshot`         | Headshot multiplier. Above `1.0` it also enables headshots on a gun without.   |
| `range`            | Range.                                                                         |
| `projectile_speed` | Projectile speed (projectile guns only).                                       |
| `fire_rate`        | Above `1.0` fires faster, below `1.0` slower.                                  |
| `recoil`           | Recoil kick.                                                                   |
| `spread`           | Hip-fire spread.                                                               |
| `aim_recoil`       | Recoil while aiming. Scales the scope's own `aimRecoilMultiplier`.             |
| `aim_spread`       | Spread while aiming. Scales the scope's own `aimSpreadMultiplier`.             |
| `max_ammo`         | Magazine capacity.                                                             |
| `reload_time`      | Reload time.                                                                   |

A grip's `recoilModifier` still scales recoil and spread together; `recoil` and `spread` modifiers
act on one each, on top of it. Aiming needs a scope, and a scope's aim multipliers default to `0.0`
(perfectly accurate), so `aim_spread` and `aim_recoil` only change something on scopes that set their own.

## Effects

An attachment of any slot type may also have an `effects` block, applied to the target whenever a shot
from the gun hits it. The keys are the same ones a gun uses for the same things.

```yaml
frost_chip:
  type: ATTACHMENT
  name: "<aqua>Frost Chip"
  itemModel: "minecraft:heart_of_the_sea"
  attachmentType: CORE_ALPHA
  modifiers:
    damage: 0.9
  effects:
    victimEffects:          # potion effects on whoever is hit, as "TYPE:ticks:amplifier"
      - "SLOWNESS:60:1"
    shooterEffects:         # potion effects on the shooter when a shot hits
      - "SPEED:40:0"
    fireTicks: 40           # sets the target on fire
    freezeTicks: 60         # vanilla powder-snow freezing
    impactParticle: "SNOWFLAKE"
    explosive:              # same keys as a gun's 'explosive'
      blastRadius: 3.0
      explosionDamage: 4.0
```

`chance` (0 to 1) is rolled on every hit, `cooldown` (ticks, per shooter and attachment) limits how often
the whole block may trigger, and `explosive.cooldown` limits the explosion alone. A cooldown starts only
when the effects actually trigger, so a failed `chance` roll does not use it up.

```yaml
  effects:
    chance: 0.25
    cooldown: 40
    explosive:
      blastRadius: 3.0
      cooldown: 100
```

Effects of every fitted attachment apply, on top of the gun's own. For projectile guns they are the
attachments that were fitted when the shot was fired. An `explosive` here goes off at the target on every
entity hit, so give it a `cooldown` before putting it on a fast-firing gun, and leave `damageBlocks` off unless you mean it.
