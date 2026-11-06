# Validation

- Compiled all production sources and development launcher against RuneLite 1.13.6, Java 11 API and bytecode.
- Ran JUnit 4.13.2: **49 tests, all passed**.
- Covered personal/team reset rules, solo-equivalent behavior, first observed KC without backfilling, duplicate completions, late same-raid reward correction, Undo, Gson persistence, invalid-state validation, old-raid reward guards, tagged/comma-formatted completion messages, exact item and recipient matching, false-positive announcement rejection, scene-only team detection and purple item IDs.
- Checked package ZIP/JAR integrity and classfile target version.
- Checked the Windows launcher script's PowerShell syntax.
- Version 1.0.1 adds seven regression tests for `Playername found something special: Item name`, including local/teammate recipients, color tags, non-breaking spaces, optional final period, exact player names and invalid-item rejection.

- Version 1.0.2 removes personal ownership inference from default chest varbits. Scene observations reset team only; a personal announcement or reward item confirms the personal reset. NPC/item IDs use named gameval constants.

Not tested: a live raid or actual Jagex login. The announcement parser is tested against the format supplied by the user, rather than a captured live RuneLite event. See README for detection details.
