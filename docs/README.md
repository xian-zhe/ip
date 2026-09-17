# Oz User Guide

Oz is a task manager operated through short text commands. In addition to todos,
deadlines, and events, Oz supports same-day events that repeat at weekly intervals.

## Adding a recurring event

Use the `recurring` command to add a same-day event that repeats every specified
number of weeks:

```text
recurring <description> /on <date> /start <time> /end <time> /every <interval> week|weeks
```

The interval number is mandatory and must be positive:

```text
recurring project meeting /on 2026-10-02 /start 1400 /end 1500 /every 1 week
```

An optional inclusive `/until` date limits the series:

```text
recurring tutorial /on 2026-10-02 /start 1000 /end 1200 /every 2 weeks /until 2026-12-31
```

The date is supplied once with `/on`; `/start` and `/end` accept times without dates.
Only weekly recurrence is supported. Commands such as `/every week`,
`/every 0 weeks`, and `/every 1 month` are rejected.

## Viewing recurring events

`list` displays each recurring series once together with its recurrence rule:

```text
1. [R] project meeting (from: Oct 02 2026, 2pm to: Oct 02 2026, 3pm; repeats: every 1 week)
```

Use `on <date>` to view the calculated occurrence on a particular date:

```text
on 2026-10-09
```

```text
Here are the tasks occurring on Oct 09 2026:
1. [R][ ] project meeting (from: Oct 09 2026, 2pm to: Oct 09 2026, 3pm)
```

Numbers shown by `on` and `find` are local to those results. Use the task number
shown by `list` when marking, unmarking, or deleting a recurring series.

## Marking a recurring occurrence

Recurring events do not have a single completion state. Specify the start date of
the occurrence to mark or unmark:

```text
mark 1 /on 2026-10-09
unmark 1 /on 2026-10-09
```

Using `mark 1` or `unmark 1` without `/on <date>` is rejected for a recurring
event. The supplied date must be an actual occurrence in the series.

## Deleting a recurring series

Use the normal `delete` command with the number shown by `list`:

```text
delete 1
```

This deletes the entire recurring series, including its saved occurrence
completion states. Deleting or rescheduling only one occurrence is not supported.

## Supported date formats

The `/on` and `/until` arguments accept dates without times, such as `2026-12-31`
or `31/12/2026`. The `/start` and `/end` arguments accept times in formats such as:

```text
1400
14:00
2pm
```
