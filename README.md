# logfilter

English | [日本語](./README.ja.md)

[![CI](https://github.com/scenario-test-framework/logfilter/actions/workflows/ci.yaml/badge.svg)](https://github.com/scenario-test-framework/logfilter/actions/workflows/ci.yaml)
[![Release](https://img.shields.io/github/v/release/scenario-test-framework/logfilter)](https://github.com/scenario-test-framework/logfilter/releases/latest)
[![Go version](https://img.shields.io/github/go-mod/go-version/scenario-test-framework/logfilter)](./go.mod)
[![Container](https://img.shields.io/badge/container-ghcr.io-blue?logo=docker)](https://github.com/scenario-test-framework/logfilter/pkgs/container/logfilter)
[![License](https://img.shields.io/github/license/scenario-test-framework/logfilter)](./LICENSE)

![logfilter: extract matching events from large log streams](./docs/assets/social-preview.png)

`logfilter` extracts the log events you need from large files by time range, log level, text, or regular expression. It preserves multiline events such as stack traces and supports common Japanese character encodings in addition to UTF-8.

It is distributed as a single Go binary and maintains command-line compatibility with the original Java implementation.

## Features

- Filter log events by time range, log level, substring, or [RE2 regular expression](https://github.com/google/re2/wiki/Syntax).
- Read timestamp and level fields from JSON Lines or logfmt, including nested JSON fields.
- Keep continuation lines, including stack traces, together with the preceding timestamped event.
- Read and write UTF-8, Shift_JIS (CP932), EUC-JP, ISO-2022-JP, and UTF-16, including common encoding aliases.
- Run as a native binary or a multi-architecture container image.
- Reuse JSON configuration while overriding individual settings from the command line.

## Quick start

Install the latest release with Go:

```sh
go install github.com/scenario-test-framework/logfilter@latest
```

Extract `WARN` and `ERROR` events from a log file:

```sh
logfilter -l 'WARN,ERROR' ./app.log ./filtered.log
```

You can also run the container image without installing the binary:

```sh
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "$PWD:/work" \
  ghcr.io/scenario-test-framework/logfilter:latest \
  -l 'WARN,ERROR' /work/app.log /work/filtered.log
```

## Typical workflow

1. Choose the input format. Text is the default for Apache access logs and timestamped application logs. Select JSON Lines with `-lf json` or logfmt with `-lf logfmt`.
2. Choose one or more filters. For example, combine a time range with `-tf` and `-tt`, select levels with `-l`, or search event content with `-s` or `-r`.
3. Run `logfilter`, specifying the input and output paths. You may omit the output positional argument only when `outputFilePath` is set in a configuration file.
4. Read the filtered file. A successful match exits with code `0`; no matching events exits with code `3`; invalid input or another error exits with code `6`.

For example:

```sh
logfilter \
  -tf '2016-11-06 12:17:53.000' \
  -tt '2016-11-06 12:18:00.000' \
  -l 'WARN,ERROR' \
  ./app.log ./filtered.log
```

## Supported log formats

### Apache access logs

```text
127.0.0.1 - name [10/Oct/2000:13:55:36 +0900] "GET /apache_pb.gif HTTP/1.0" 200 2326
```

### Timestamp, log level, and message

The parser accepts the following timestamp components:

- Date: `yyyy/MM/dd`, `yy/MM/dd`, `yyyy-MM-dd`, `yy-MM-dd`, or `yyyyMMdd`
- Date/time separator: a space or `T`
- Time: omitted, `HH:mm`, `HH:mm:ss`, `HH:mm:ss.SSS`, or `HH:mm:ss,SSS`
- Time zone: ISO timestamps using the `T` separator accept `Z` or a numeric offset such as `+09`, `+0900`, or `+09:00`. Apache access-log timestamps include an offset. Space-separated timestamps are interpreted in the local time zone.

The log level is recognized when it appears between spaces, such as ` ERROR `, or immediately after `[`, such as `[ERROR]`. Level names are not restricted to a fixed set, so values such as `TRACE`, `ERROR`, `FINEST`, and `FATAL` are supported.

```text
2016-11-06 12:17:53.985 DEBUG 15765 [main] Example : Starting validation
2016-11-06 12:17:54.022 ERROR 15765 [main] Example : Validation failed
The second line of this event has no timestamp.
```

### Structured logs (JSON Lines / logfmt)

Select the format explicitly. These two records describe the same event.

```json
{"time":"2024-01-01T00:00:01Z","level":"warn","msg":"retry stopped"}
```

```text
time=2024-01-01T00:00:01Z level=warn msg="retry stopped"
```

Each JSON Lines record is a JSON object on one line. In logfmt, whitespace separates fields. Double-quoted values may contain spaces and the escapes `\"`, `\\`, `\n`, `\r`, and `\t`. An unquoted value must not contain `=` or `"`; such a line is treated as a continuation line. `key=` gives an empty value. A bare `key` without `=` is ignored, so words such as `time` in plain continuation lines are never mistaken for fields.

- **Timestamp field.** The first existing key in the order `time`, `timestamp`, `ts`, `@timestamp` supplies the timestamp. Use `-tk` to select a single key. For example, `-tk log.ts` reads a literal JSON key named `log.ts` first, then looks for `ts` inside `log`. At each depth, the remaining literal key takes priority, followed by object prefixes from shortest to longest. Dots are always literal in logfmt. The same rules apply to `-lk`.
- **Timestamp format.** In `auto` mode, a string that consists only of digits (optionally with a leading `-` and a fraction) is a Unix timestamp, except an eight-digit integer string that is a valid calendar date, such as `"20240101"`, which means January 1, 2024 in local time (`yyyyMMdd`). An eight-digit string that is not a valid date, such as `"20240230"`, is read as Unix seconds. Other strings are parsed as supported dates. JSON numbers are Unix timestamps. The absolute integer part determines the unit. Values below `1e11` use seconds, below `1e14` milliseconds, below `1e17` microseconds, and all larger values nanoseconds. Use `-tfmt unix`, `unixms`, `unixus`, or `unixns` to fix the unit, including for eight-digit strings. All four accept fractions and retain precision to the nanosecond. Unix timestamps outside the years 0001 to 9999 are invalid values.
- **Event boundaries.** A record with a valid timestamp starts an event. A malformed record, a non-object JSON value, or a line missing the selected key continues the preceding event. The first line must start an event. An existing timestamp field with an invalid value stops processing with exit code `6` and reports the line number and value. Output preserves the original line content. Substring and regex filters search the entire event, including continuation lines.
- **Log level.** The first existing key in the order `level`, `severity`, `lvl` on the event's first line supplies the level. Use `-lk` to select a single key. String and numeric levels match an entire `-l` value, ignoring case. If the field is missing or has another type, the filter uses the original case-sensitive search for ` LEVEL ` or `[LEVEL` anywhere in the event. This also works for Docker json-file records whose `log` field contains the level.

## Installation

### GitHub Releases

Download the archive for your platform from [GitHub Releases](https://github.com/scenario-test-framework/logfilter/releases/latest), then extract and install the binary:

```sh
tar xvfz ./logfilter_*.tar.gz
mv ./logfilter_*/logfilter /usr/local/bin/
```

### Go

```sh
go install github.com/scenario-test-framework/logfilter@latest
```

### Docker

```sh
docker pull ghcr.io/scenario-test-framework/logfilter:latest
```

## Usage

```text
logfilter [OPTION] inputFilePath [outputFilePath]
```

Examples:

```sh
# Show help
logfilter -h

# Filter by time range
logfilter -tf '2016-11-06 12:17:53.000' -tt '2016-11-06 12:18:00.000' ./app.log ./filtered.log

# Filter by log level
logfilter -l 'WARN,ERROR' ./app.log ./filtered.log

# Read the default timestamp and level fields from logfmt
logfilter -lf logfmt -l WARN ./app.logfmt ./filtered.log

# Read a nested Unix millisecond timestamp and level from JSON Lines
logfilter -lf json -tk log.ts -tfmt unixms -lk log.level -l ERROR ./app.jsonl ./filtered.jsonl

# Filter by substring or regular expression
logfilter -s 'Exception' ./app.log ./filtered.log
logfilter -r 'ERROR|Exception' ./app.log ./filtered.log

# Load a configuration file and override one value
logfilter -cf ./config.json -tf '2016-11-06' ./app.log ./filtered.log
```

### Options

| Option | Description |
|---|---|
| `-h`, `--help` | Show command usage. |
| `-f`, `--force` | Do not ask for confirmation before overwriting an existing output file. |
| `-cf`, `--configFile` | Read settings from a JSON configuration file. |
| `-ic`, `--inputCharset` | Input encoding. The default is UTF-8. |
| `-oc`, `--outputCharset` | Output encoding. The default is the input encoding. |
| `-lf`, `--logFormat` | Input format. Choose `text` (default), `json` (JSON Lines), or `logfmt`. |
| `-tk`, `--timestampKey` | Structured timestamp field. Defaults to the first existing timestamp candidate. |
| `-tfmt`, `--timestampFormat` | Structured timestamp format. Choose `auto` (default), `unix`, `unixms`, `unixus`, or `unixns`. |
| `-lk`, `--logLevelKey` | Structured level field. Defaults to the first existing level candidate. |
| `-tf`, `--timeFilterFrom` | Include events at or after this time. |
| `-tt`, `--timeFilterTo` | Exclude events at or after this time. |
| `-l`, `--logLevelFilter` | Comma-separated log levels to include. |
| `-s`, `--stringContentFilter` | Substring that the event must contain. |
| `-r`, `--regexFilter` | RE2 regular expression that the event must match. |

### Configuration file

```json
{
  "inputCharset": "UTF-8",
  "outputFilePath": "",
  "outputCharset": "",
  "timeFilterValueFrom": "",
  "timeFilterValueTo": "",
  "logLevelFilterValueList": ["WARN", "ERROR"],
  "stringContentFilterValue": "",
  "regexFilterValue": ""
}
```

The input file is always supplied as a positional command-line argument. When the output positional argument is omitted, `outputFilePath` from the configuration file is used.

For JSON Lines records such as `{"log":{"ts":1704067200123,"level":"error"}}`, use the following configuration. Set `timestampKey`, `timestampFormat`, and `logLevelKey` only with `json` or `logfmt`.

```json
{
  "logFormat": "json",
  "timestampKey": "log.ts",
  "timestampFormat": "unixms",
  "logLevelKey": "log.level",
  "logLevelFilterValueList": ["ERROR"]
}
```

Nonempty command-line options override the corresponding configuration values.

### Exit codes

| Code | Meaning |
|---|---|
| `0` | Completed successfully. |
| `3` | Completed with no matching log events. |
| `6` | Failed because of invalid input or another error. |

## Running with Docker

Mount the directory containing your logs into `/work`. Set `TZ` when timestamps without an explicit offset should be interpreted in a local time zone; otherwise, they are interpreted as UTC.

```sh
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -e TZ=Asia/Tokyo \
  -v "$PWD:/work" \
  ghcr.io/scenario-test-framework/logfilter:latest \
  -l 'WARN,ERROR' /work/app.log /work/filtered.log
```

Using the host user and group with `-u "$(id -u):$(id -g)"` ensures that the output file is owned by the current host user.

With [Docker Compose](./compose.yaml):

```sh
docker compose run --rm logfilter -l 'WARN,ERROR' /work/logs/app.log /work/logs/filtered.log
```

## Development

Go 1.26 or later is required.

```sh
make        # vet, test, and build
make cross  # build release archives in dist/
```

| Workflow | Trigger | Purpose |
|---|---|---|
| [CI](./.github/workflows/ci.yaml) | Push or pull request | Vet, test, compile, and verify the container build. |
| [Release](./.github/workflows/release.yaml) | Push of a `v*` tag | Publish release archives and amd64/arm64 container images. |

Internal development documentation and source-code comments are primarily written in Japanese.

## License

Licensed under the [Apache License 2.0](./LICENSE).
