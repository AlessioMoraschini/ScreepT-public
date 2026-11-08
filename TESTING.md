# ScreepT - automatic tests

Three tiers, told apart by the class name (JUnit 4, no extra dependency, they work offline):

| Tier | Class name ends with | What it checks | Time | When |
|------|----------------------|----------------|------|------|
| quick | `QuickTest` | pure logic: image engine, presets, viewer associations, Java names in the launcher | seconds | every build (`build_with_quick_tests.cmd`) |
| medium | `MediumTest` | real files in a temp folder, every filter on a 4 MP image, every writable format, batch-like conversion | about half a minute | once a day |
| full | `FullTest` | opens the real windows (Image Studio, Batch, viewer associations); skipped without a display | about a minute | weekly and before every release |

```
run_tests.cmd quick | medium | full      (Windows)
./run_tests.sh quick | medium | full      (Linux / macOS)
```

Each tier includes the previous ones. New tests: put them in `src/test/java/tiers/` of the module, name them after the tier.

## Scheduling (suggested, not installed)
- quick: already part of `build_with_quick_tests.cmd`
- medium, daily at 13:00 (Windows): `schtasks /Create /SC DAILY /ST 13:00 /TN "ScreepT tests medium" /TR "C:\...\ScreepT-public\run_tests.cmd medium"`
- full, weekly on Sunday at 20:00: `schtasks /Create /SC WEEKLY /D SUN /ST 20:00 /TN "ScreepT tests full" /TR "C:\...\ScreepT-public\run_tests.cmd full"`

The release procedure should run `run_tests.cmd full` before building the release.
