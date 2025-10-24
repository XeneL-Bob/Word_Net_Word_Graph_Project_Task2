# Word_Net_Word_Graph_Project_Task2
Repo for Task 2 of CSC201 by A Innes (Xenel_BoB)

A small language-analysis engine that ingests a cleaned book (one sentence per line, lower-case, no punctuation), builds a **directed, weighted word graph** from ordered bigrams, and supports four tasks:

1) **Word frequency** (CSV + top-k tiers)  
2) **Ordered co-occurrence** (CSV + rank tiers)  
3) **Word graph queries** (shortest path, nodes at exact hops)  
4) **Deterministic auto-sentence generation**

---

## Quick start

### Requirements
- Java **21+** (tested with Eclipse Adoptium / VS Code Java).
- OS: Windows/macOS/Linux.

### Folder layout (key files)
```
Resources/
  book.txt                 # input corpus (one sentence per line)
OutputFiles/               # CSV outputs are written here
src/Main/Java/Util/
  Corpus.java
  Tokenizer.java
  Pair.java
  Graph.java
src/Main/Java/wordnetcomp/
  Main.java                # master driver (CLI)
  WordFrequency.java       # Task 1
  CoOccurrence.java        # Task 2
  WordConnection.java      # Task 3
  Generator.java           # Task 4
```

### Build & run (plain Java)
```bash
# from repo root
javac -encoding UTF-8 -d bin src/Main/Java/Util/*.java src/Main/Java/wordnetcomp/*.java

# run all tasks on default inputs
java -cp bin wordnetcomp.Main

# run the built-in self-check
java -cp bin wordnetcomp.Main --check
```

> Using VS Code? You can “Run” the `Main` class directly. To add flags, create a launch config or run from the built-in terminal with `java -cp … wordnetcomp.Main --check`.

---

## CLI usage

```
Usage: java wordnetcomp.Main [options]
  -i, --input <file>     Input text file (default Resources/book.txt)
  -o, --out <dir>        Output directory (default OutputFiles)
  -t, --task <name>      all | wf | co | graph | gen (default all)
  -k, --tier <n>         Print top <n> frequency tiers (default 3)
  --start <word>         Start word for graph & generator (default 'paul')
  --target <word>        Target word for graph path (default 'arrakis')
  --hops <n>             Hop distance for neighbors (default 1)
  --len <n>              Generated sentence length (default 6)
  --check                Run a quick self-check of counts/graph
  -h, --help             Show this help
```

### Examples via tasks "single"
```bash
# Task 1 only: top-5 tiers
java -cp bin wordnetcomp.Main --task wf -k 5

# Task 2 only: top-3 bigram tiers
java -cp bin wordnetcomp.Main --task co -k 3

# Task 3 only: path & neighbors
java -cp bin wordnetcomp.Main --task graph --start paul --target arrakis --hops 2

# Task 4 only: generator
java -cp bin wordnetcomp.Main --task gen --start paul --len 6
```

---

## Input format (assumptions)

- Each line is **one sentence**.  
- Lower case only; **no punctuation**; words are base forms.  
- Tokens are separated by **single spaces**.  
- Out-of-vocabulary guard: anything non `[a-z]` is treated as whitespace (the provided `book.txt` is already clean).

---

## Outputs

### Task 1 – Word Frequency
- `OutputFiles/word_frequency.csv`  
- `OutputFiles/sorted_words.csv`  
Format (no header): `word,count`

Console top-k rank template:
```
Rank %d: %d word(s) with %d occurrence(s).
Word(s) include: [a, b, c]
```

### Task 2 – Ordered Co-occurrence
- `OutputFiles/bigram_frequency.csv` (no header): `from,to,count`

Console rank template:
```
Bigram Rank %d: %d pair(s) with %d occurrence(s).
Pairs include: [from1 to1, from2 to2, ...]
```

### Task 3 – Word Graph
- Nodes: unique words.  
- Edge `u→v` exists iff bigram `(u,v)` was seen.  
- **Weight:** `cost(u,v) = 100.0 / count(u,v)` (higher co-occurrence ⇒ lower cost).

Templates:
```
Shortest Path between '%s' and '%s' has total cost %.6f, with %d hops.
Path: [u, ..., v]

Total number of nodes with %d hop(s) from '%s': %d
Words: [ ... ]
```

### Task 4 – Generator
- Greedy, deterministic next-word choice by **max bigram count**, tie-break by alpha:

```
Complete sentence with %d words is: [ ... ]
# or
Incomplete sentence with %d words is: [ ... ]
```

---

## Determinism and tie-breaking

To make every run reproducible:

- **Task 1:** sort by `(freq desc, word asc)`. Within a frequency tier, words are **alphabetical**.  
- **Task 2:** bigram rank tiers sorted by `(from asc, to asc)`.  
- **Task 3:** adjacency lists sorted by `to`; Dijkstra’s PQ comparator uses `(distance asc, node asc)` for **lexicographic tie-breaks**.  
- **Task 4:** neighbor lists are pre-sorted by `(count desc, to asc)`; we always pick index `0`.

---

## Design and Complexity

### Corpus loading
- One pass to split sentences into tokens.  
- **Time:** `O(T)` where `T` = total tokens. **Space:** `O(T)` for sentences (or streaming if adapted).

### Task 1 – WordFrequency
- Count with `HashMap<String,Integer>`.  
- Build rank buckets `Map<Integer,List<String>>` and a sorted list of distinct freqs.  
- **Time:** counting `O(T)`, sorting tiers `O(U log U)` where `U` = unique words.  
- **Space:** `O(U)`.

### Task 2 – CoOccurrence
- Count ordered bigrams using `HashMap<Pair,Integer>`.  
- Rank buckets `Map<Integer,List<Pair>>` sorted by `(from,to)`.  
- **Time:** `O(B)` to build (`B` = total bigrams), bucket sort `O(E log E)` with `E` = unique bigrams.  
- **Space:** `O(E)`.

### Task 3 – WordConnection / Graph
- Adjacency lists (`Map<String,List<Edge>>`), edges pre-sorted by `to`.  
- **Dijkstra:** `O((V+E) log V)` with lexicographic PQ tie-break.  
- **BFS nodes-at-hops:** `O(V+E)`.  
- **Space:** `O(V+E)`.

### Task 4 – Generator
- Precompute best next choices per word: `Map<String,List<Entry<String,Integer>>>` sorted by `(count desc, to asc)`.  
- Generation is `O(L)` for length `L` (each step is O(1) to pick the first item).  
- **Space:** `O(E)`.

---

## Self-check

Run:

```bash
java -cp bin wordnetcomp.Main --check
```

What it does:

- Confirms that `"paul"`, `"arrakis"` exist in the corpus.  
- Verifies bigram presence: `"paul atreides"`, `"desert power"`, `"gom jabbar"`.  
- Builds a graph and checks a **sanity path** from `paul → arrakis` (4 hops, total cost `≈ 400.0` given the provided `book.txt`).  
- Prints `[OK]` or `[WARN]` for each assertion and a summary.

---

## Example console (abridged)

```
================ Task 1 – Word Frequency ================
Build word counts finished in 10 ms
Rank 1: 1 word(s) with 17 occurrence(s).
Word(s) include: [paul]
...

================ Task 2 – Co-occurrence ================
Build bigrams finished in 3 ms
Bigram Rank 3: 14 pair(s) with 2 occurrence(s).
Pairs include: [bene gesserit, desert power, ...]
...

================ Task 3 – Word Graph ================
Shortest Path between 'paul' and 'arrakis' has total cost 400.000000, with 4 hops.
Path: [paul, atreides, travel, to, arrakis]
...

================ Task 4 – Generator ================
Complete sentence with 6 words is: [paul, and, chani, guide, atreides, depart]
```

---

## Testing & marking checklist

- Word frequency CSV + sorted CSV (no header, `word,count`)  
- Top-k tiers printed using provided templates  
- Bigram CSV (no header, `from,to,count`)  
- Bigram rank tiers with ties and lexical sort  
- Shortest path printed with **cost** and **hops**, lexicographic tie-breaks  
- Nodes at exact hop distance printed with **count** and **sorted list**  
- Deterministic generator with tie-break rules  
- Master `Main` can run all tasks or any single task  
- Optional `--check` validates a few invariants on the provided `book.txt`

---

## Troubleshooting

- **`Input file not found`** – ensure `Resources/book.txt` exists (or pass `--input <path>`).  
- **Classpath errors** – recompile with `javac -d bin …` and run with `java -cp bin wordnetcomp.Main`.  
- **Different shortest path** – multiple shortest paths can exist; we enforce determinism via lexical tie-break, but costs must still be correct.

---

## Notes on academic integrity

ALL algorithms are implemented with standard Java collections and course-level techniques using standard Java. Do not post private repositories publicly =) 

---

### Maintainer

- **Author:** <Xene-BoB _ For UniSC T2 CSC201  
- **Email:** abi002@student.usc.edua.u
