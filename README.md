# CCS Pairing System (Spring Boot)

A basic Spring Boot rewrite of the console pairing app, with a MySQL (Laragon) backend
and a plain HTML/JS front end (no framework, so it's easy to read and cheap to run).

## 1. Set your DB password

Your terminal showed `root` now needs a password (`ERROR 1045` on empty, then it worked
after you typed one). Open:

`src/main/resources/application.properties`

and replace `CHANGE_ME` with your actual root password:

```
spring.datasource.password=your_actual_password
```

The database `ccs_pairing` will be created automatically on first run
(`createDatabaseIfNotExist=true`), and tables are created automatically too
(`spring.jpa.hibernate.ddl-auto=update`) — no manual SQL needed.

## 2. Run it

Make sure Laragon's MySQL is running (it already is, per your `netstat` output — port 3306).

From the project folder:

```
mvn spring-boot:run
```

(If you don't have Maven installed, Laragon usually has it under Tools > Quick add,
or just import the folder as a Maven project in IntelliJ/Eclipse and run
`PairingSystemApplication.java` directly.)

Then open: **http://localhost:8080**

## 3. How to use the UI

1. Create a tournament (name + number of rounds).
2. Add players (name + rating).
3. Click **Generate Next Round Pairings** — boards appear below.
4. Click White wins / Draw / Black wins on each board to record results
   (ratings, scores, and standings update automatically, same Elo logic
   as your console version).
5. Click **Generate Next Round Pairings** again for the next round, repeat.
6. Standings table at the bottom always shows current rank.

## What's simplified vs. the console app

To keep this basic (per your request), a few things were trimmed:

- **Pairing algorithm**: uses a straightforward single-pass greedy Swiss pairing
  (sort by score/rating, pair down avoiding repeat opponents where possible)
  instead of the full backtracking + cross-bracket + global 2-opt repair pass.
  For a small club tournament this gives the same practical result almost
  always; it just won't guarantee the mathematically optimal pairing in rare
  edge cases.
- **Tie-breakers** (Buchholz / Sonneborn-Berger) and **save/load to .txt**,
  **undo round** aren't wired up in this version — score/rating/W-L-D and
  color balance are tracked and persisted in MySQL instead, which replaces
  the old file save/load.
- No login/auth — it's a local tool for the club, same as before.

All of this is straightforward to add back if you need it — just say which
piece and I'll extend the corresponding file (`TournamentService.java` mainly).

## Project structure

```
src/main/java/com/ccs/pairing/
  PairingSystemApplication.java   - entry point
  model/                          - Player, Tournament, Match (JPA entities)
  repository/                     - Spring Data JPA repositories
  service/TournamentService.java  - pairing, results, rating logic
  controller/TournamentController.java - REST API (/api/...)
src/main/resources/
  application.properties          - DB connection (edit the password!)
  static/index.html, app.js       - the basic UI
```
