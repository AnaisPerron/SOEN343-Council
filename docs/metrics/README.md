# Phase III metrics

The files in [`phase3/`](phase3/) contain a Lizard baseline for the backend Java source and SonarQube exports for files across the project. They are saved snapshots, not measurements of the current working tree.

## Lizard baseline

- Recorded revision: commit `dd8b2793335cfead5380f8fb66e3739d9cb7d46f` (identified as `phase3` when the baseline was captured).
- Tool: Lizard 1.26.1, using its default options.
- Command, run from the repository root: `lizard tender-app/backend/src/main/java`
- Output: [`phase3/lizard.txt`](phase3/lizard.txt). It contains per-function and per-file metrics, warnings, and totals.

The current `phase3` tag does not resolve to the recorded commit. Use the commit hash above to reproduce this baseline rather than relying on the tag name.

## SonarQube exports

| File | Contents |
| --- | --- |
| [`phase3/sonarqube-measures.json`](phase3/sonarqube-measures.json) | Per-file SonarQube measures, including complexity, non-comment lines of code (`ncloc`), cognitive complexity, and code smells. |
| [`phase3/sonarqube-metrics.csv`](phase3/sonarqube-metrics.csv) | Tabular view of those per-file measures, with columns for file, NLOC, cyclomatic complexity, cognitive complexity, and code smells. |
| [`phase3/sonarqube-code-smells.json`](phase3/sonarqube-code-smells.json) | Exported code-smell issues. |
| [`phase3/sonarqube-all-issues.json`](phase3/sonarqube-all-issues.json) | Exported issues across all types, including bugs, vulnerabilities, and code smells. |

The exports contain 402 file measures, 5 code-smell issues, and 20 issues in total. They do not record an analysis commit or export timestamp, so their provenance should not be inferred from the Lizard baseline.
