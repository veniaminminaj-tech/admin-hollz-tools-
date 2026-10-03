Admin Crucifix - Forge 1.16.5 (Forge 36.2.39, JDK 8)

EASIEST: BUILD THE JAR ONLINE (no installs)
1. Create a free GitHub repo and upload everything in this folder
   (keep the .github folder).
2. Open the Actions tab -> "Build jar" -> wait ~3 minutes.
3. Download the "admin-crucifix-jar" artifact; the file inside
   (doorscrucifix-1.0.0.jar, not the -sources one) goes in .minecraft/mods.

LOCAL BUILD: install JDK 8 + Gradle 7.4.2, run "gradle build" here, jar is in build/libs/.

USE (operators, permission level 2+)
- /give @s doorscrucifix:admin_crucifix
- Hold right-click ~1.5s: charge animation, then burst FREEZES every mob within 5 blocks, then they shatter (die) 1.5s later.
- Sneak + right-click a player: bans them. Undo with /pardon <name>.

ITEMS (all operator-only)
- /give @s doorscrucifix:admin_crucifix   hold right-click: freeze-then-kill burst (5 blocks); sneak+right-click player: BAN
- /give @s doorscrucifix:knockback_stick  hit anything: launched far (game caps speed ~3.9 blocks/tick)
- /give @s doorscrucifix:admin_gun        right-click: instant beam, 1000 damage, 100 block range
- /give @s doorscrucifix:admin_bible      right-click: menu of online players; click a name to teleport to them
                                          (top button switches to "Bring player to me")

BAN REASON
- /reason <text>   set the reason the Admin Crucifix writes into the ban (shown to the banned player too)
- /reason          show your current reason (default: "Banned with the Admin Crucifix")

JAIL (new dimension "doorscrucifix:jail", a void with one blackstone cell)
- /give @s doorscrucifix:jail_hammer
    right-click a player: jail them; sneak + right-click: release them
- /jail <player>  and  /unjail <player>   same thing from chat (works from anywhere)
- On jailing: water buckets (incl. fish buckets), ender pearls and chorus fruit are removed, and are
  removed again every second. Chorus/pearl teleporting is blocked, non-op jailed players can't break or
  place blocks, and dying sends them back to the cell. Released players return to where they were jailed.

PORTAL GUN (from PortalGunMod.zip, Rick and Morty style)
- /give @s doorscrucifix:portal_gun
- LEFT CLICK: save your current spot + dimension as the destination
- RIGHT CLICK: open a green portal in front of you linked to it (a matching portal opens at the destination)
- Portals last 30 seconds; players, mobs and dropped items can walk through; works across dimensions
