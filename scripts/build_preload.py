#!/usr/bin/env python3
"""
Build app/src/main/assets/preload.json from raw NDJSON word lists.

Source: github.com/kajweb/dict (extracted under scripts/raw/).
- PEPGaoZhong_1..11.json: senior-high school words (3500-tier).
- KaoYan_1..3.json: postgrad entrance exam words (5500-tier).

Output schema (kept minimal — drop anything we won't show in v0.1):
[
  {
    "lemma": "abandon",
    "phonetic": "əˈbændən",
    "partOfSpeech": "v.",
    "definitionZh": "放弃；抛弃",
    "exampleEn": "She abandoned the plan.",
    "exampleZh": "她放弃了这个计划。",
    "level": "gaozhong" | "kaoyan" | "both"
  },
  ...
]

When a lemma appears in both sources, the kaoyan entry wins (richer content)
but level is marked "both".
"""
from __future__ import annotations

import glob
import json
import os
import re
import sys
from pathlib import Path

RAW_DIR = Path(__file__).parent / "raw"
OUTPUT = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "preload.json"

# Reject entries that are phrases / multi-token expressions rather than lemmas.
# The source mixes a small number of these in. We only want single words.
# Also require >= 2 letters (drops "BC", "PE" etc. — abbreviations not needed).
LEMMA_OK = re.compile(r"^[A-Za-z][a-z'\-]{1,}$")

# Strip leading pos tag like "v. ", "n. ", "adj. " from a Chinese gloss so it
# doesn't duplicate the partOfSpeech field. Source format is consistent: short
# pos token + period + space.
POS_PREFIX = re.compile(r"^(?:n|v|vt|vi|adj|adv|prep|conj|pron|num|art|int)\.\s*", re.IGNORECASE)
POS_DISPLAY = {
    "n": "n.",
    "v": "v.",
    "vt": "v.",
    "vi": "v.",
    "adj": "adj.",
    "adv": "adv.",
    "prep": "prep.",
    "conj": "conj.",
    "pron": "pron.",
    "num": "num.",
    "art": "art.",
    "int": "int.",
}


def extract_one(raw_entry: dict) -> dict | None:
    """Pull the fields we care about from one source NDJSON line.

    Returns None if the entry is unusable (no headWord, looks like a phrase,
    or no Chinese gloss)."""
    head = raw_entry.get("headWord", "").strip()
    if not head or not LEMMA_OK.match(head):
        return None

    content = raw_entry.get("content", {}).get("word", {}).get("content", {})

    # Phonetic: prefer UK (more standard for Chinese learners), fall back to US
    phonetic = content.get("ukphone") or content.get("usphone") or None
    if phonetic:
        phonetic = phonetic.strip()

    # Translations: dict has list of {pos, tranCn}
    trans_list = content.get("trans", []) or []
    if not trans_list:
        return None

    # Combine all pos+tranCn into a compact definition string.
    # Source often gives multiple senses; we keep the first 2 to limit clutter.
    # primary_pos goes into its own field, so don't prepend it to the gloss
    # itself — only mark pos for *additional* senses with a different pos.
    parts: list[str] = []
    primary_pos: str | None = None
    for t in trans_list[:2]:
        cn = (t.get("tranCn") or "").strip()
        if not cn:
            continue
        cn = POS_PREFIX.sub("", cn)
        pos = (t.get("pos") or "").strip().lower()
        pos_display = POS_DISPLAY.get(pos, "")
        if primary_pos is None:
            primary_pos = pos_display or None
            parts.append(cn)
        elif pos_display and pos_display != primary_pos:
            parts.append(f"{pos_display} {cn}")
        else:
            parts.append(cn)

    definition_zh = "；".join(p.strip() for p in parts if p.strip())
    if not definition_zh:
        return None

    # First example sentence (if any)
    example_en: str | None = None
    example_zh: str | None = None
    sentences = (
        content.get("sentence", {}).get("sentences", []) or []
    )
    if sentences:
        first = sentences[0]
        example_en = (first.get("sContent") or "").strip() or None
        example_zh = (first.get("sCn") or "").strip() or None

    return {
        "lemma": head,
        "phonetic": phonetic,
        "partOfSpeech": primary_pos,
        "definitionZh": definition_zh,
        "exampleEn": example_en,
        "exampleZh": example_zh,
    }


def load_source(pattern: str, level: str) -> dict[str, dict]:
    """Read every matching NDJSON file, return {lemma_lower: entry_with_level}."""
    out: dict[str, dict] = {}
    for path in sorted(glob.glob(str(RAW_DIR / pattern))):
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    raw = json.loads(line)
                except json.JSONDecodeError:
                    continue
                entry = extract_one(raw)
                if not entry:
                    continue
                key = entry["lemma"].lower()
                if key in out:
                    continue
                entry["level"] = level
                out[key] = entry
    return out


def main() -> int:
    if not RAW_DIR.is_dir():
        print(f"Missing {RAW_DIR}; download word list zips first.", file=sys.stderr)
        return 1

    gaozhong = load_source("PEPGaoZhong_*.json", "gaozhong")
    kaoyan = load_source("KaoYan_*.json", "kaoyan")

    merged: dict[str, dict] = {}
    for k, v in gaozhong.items():
        merged[k] = v
    for k, v in kaoyan.items():
        if k in merged:
            # Prefer the kaoyan entry but mark as both.
            v = {**v, "level": "both"}
            merged[k] = v
        else:
            merged[k] = v

    # Sort alphabetically for deterministic output (and so future diffs are readable).
    out_list = [merged[k] for k in sorted(merged.keys())]

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(out_list, f, ensure_ascii=False, separators=(",", ":"))

    size_kb = OUTPUT.stat().st_size / 1024
    levels = {"gaozhong": 0, "kaoyan": 0, "both": 0}
    for e in out_list:
        levels[e["level"]] += 1
    print(f"Wrote {len(out_list)} entries to {OUTPUT} ({size_kb:.0f} KB)")
    print(f"  gaozhong-only: {levels['gaozhong']}")
    print(f"  kaoyan-only:   {levels['kaoyan']}")
    print(f"  both:          {levels['both']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
