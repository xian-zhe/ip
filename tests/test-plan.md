# Recurring Events Test Plan

## Preconditions

1. Run Oz with Java 25.
2. Start with an empty `data/oz.txt`, or use a separate temporary storage file.
3. Enter commands exactly as shown unless a test states otherwise.

## Automated regression test

Run:

```text
./gradlew test checkstyleMain checkstyleTest
```

Expected: the build succeeds and all existing and recurring-event tests pass.

## Add and list an indefinite series

1. Enter:

   ```text
   recurring project meeting /on 2026-10-02 /start 1400 /end 1500 /every 1 week
   ```

2. Enter `list`.

Expected: one `[R]` task is shown with the first start and end times and
`repeats: every 1 week`.

## Add a finite series

Enter:

```text
recurring tutorial /on 2026-10-02 /start 1000 /end 1200 /every 2 weeks /until 2026-10-30
```

Expected: the task is added and its display ends with
`repeats: every 2 weeks until Oct 30 2026`.

## Query occurrence dates

Using the two-week series above:

1. Enter `on 2026-10-16`.
   Expected: the tutorial occurrence from 10am to 12pm is shown.
2. Enter `on 2026-10-23`.
   Expected: no tutorial occurrence is shown.
3. Enter `on 2026-10-30`.
   Expected: the final tutorial occurrence is shown because `/until` is inclusive.
4. Enter `on 2026-11-13`.
   Expected: no tutorial occurrence is shown because the series has ended.

## Mark and unmark one occurrence

Use the recurring task number reported by `list`.

1. Enter `mark <number>`.
   Expected: Oz requests `/on <date>` and makes no change.
2. Enter `mark <number> /on 2026-10-16`.
   Expected: the October 16 occurrence is displayed with `[X]`.
3. Enter `on 2026-10-16`.
   Expected: the occurrence remains `[X]`.
4. Enter the same mark command again.
   Expected: Oz reports that the occurrence is already marked.
5. Enter `unmark <number> /on 2026-10-16`.
   Expected: the occurrence is displayed with `[ ]`.
6. Enter the same unmark command again.
   Expected: Oz reports that the occurrence is not marked.

## Reject a non-occurrence date

Enter `mark <number> /on 2026-10-23` for the two-week series.

Expected: Oz reports that the recurring task has no occurrence on October 23 and
does not modify any occurrence.

## Validate recurring syntax

Each command below must return an error and must not add a task:

```text
recurring meeting /on 2026-10-02 /start 1400 /end 1500 /every week
recurring meeting /on 2026-10-02 /start 1400 /end 1500 /every 0 weeks
recurring meeting /on 2026-10-02 /start 1500 /end 1400 /every 1 week
recurring meeting /on 2026-10-02 /start 1400 /end 1500 /every 1 month
recurring meeting /on 2026-10-02 /start 1400 /end 1500 /every 1 week /until 2026-10-01
```

## Preserve ordinary task behavior

1. Add ordinary todos, deadlines, and events using their existing commands.
2. Mark and unmark them without `/on`.
3. List, find, query, and delete them.

Expected: all existing commands and output behavior remain unchanged. Supplying
`/on <date>` when marking an ordinary task produces an error.

## Persistence and compatibility

1. Add a recurring series and mark one occurrence.
2. Exit and restart Oz.
3. Run `list`, query the marked date, and query another occurrence date.

Expected: the series reloads, only the selected occurrence is marked, and existing
`T`, `D`, and `E` records still load normally. A malformed `R` record is reported
and skipped without preventing later valid records from loading.

## Delete a series

1. Run `delete <number>` using the recurring series number from `list`.
2. Restart Oz and run `list`.

Expected: the entire recurring series and its occurrence completion data are gone.
