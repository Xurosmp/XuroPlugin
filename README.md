# SMPCore

All-in-one SMP-Plugin für **Paper 1.21.4+** (Java 21) mit **Bedrock-Support** (Geyser/Floodgate).

## Features
- **Eigene Economy** (Geld + Kristalle), `/balance`, `/pay`, `/top`, `/eco` (Admin), Kurzformen: `1k`, `2.5m`, `1b`
- **Shop** (Kategorien -> Item -> Kauf/Verkauf 1/16/64/Alles), `/sell hand|all`
- **Auktionshaus** `/ah`, `/ah sell <preis>`, `/ah search <text>`, Sortierung, Ablauf, Steuer, "Meine Items"
- **Kopfgelder** `/bounty`, `/bounty <spieler> <betrag>`, Auszahlung beim Kill
- **Kristalle (Shards)**: Kills, Spielzeit, Daily, Kristall-Shop `/crystalshop`
- **Crates mit Keys**: Vorschau (Linksklick), Öffnen mit Animation (Rechtsklick), Partikel, Broadcast bei seltenen Gewinnen
- **Admin Panel** `/ap`: Spieler (Geld/Kristalle/Keys/TP/Heilen/Kick), Kopfgelder, Crates, Feature-Schalter, Reload
- **Extras**: Scoreboard, Homes, `/tpa`, `/rtp`, Spawn, `/daily` mit Streak, `/stats`, Bestenlisten

## Bedrock
Alle Menüs sind normale Kisten-GUIs und mit **einem Tap** bedienbar (kein Shift-Klick, kein Chat-Input nötig).
Käufe im AH/Kristall-Shop haben ein Bestätigungsmenü. Spielerdaten laufen über UUIDs, Floodgate-Namen (`.Name`)
werden mit und ohne Prefix gefunden (`bedrock-prefix` in der config.yml).

## JAR über GitHub bekommen
1. Neues Repo auf GitHub anlegen und diesen Ordnerinhalt hochladen (Push auf `main`).
2. Tab **Actions** -> der Workflow "Build SMPCore" läuft automatisch.
3. Fertige JAR: **Releases -> "Latest Build"** (wird bei jedem Push neu gebaut) oder unter dem Run als Artifact.
4. Für feste Versionen: Tag pushen, z.B. `git tag v1.0.0 && git push --tags` -> eigenes Release.

## Installation
JAR in `plugins/`, Server starten, Configs unter `plugins/SMPCore/` anpassen (`config.yml`, `shop.yml`, `crates.yml`, `crystalshop.yml`).
Danach: `/setspawn`, Crate-Block anschauen und `/crate set common`, Keys geben mit `/crate give <spieler> common 5`.

## Rechte
`smpcore.admin` (OP), `smpcore.tp.bypass` (kein Teleport-Delay). Alles andere ist für alle erlaubt.

## Eigene Plugins anbinden
`SMPCore.get().economy().add(uuid, 100)` usw. (Vault-Bridge ist nicht enthalten).
