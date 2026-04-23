#!/usr/bin/env bash
# build_npc_data.sh
#
# Builds npc_max_hits.json from the OSRS Wiki's Infobox Monster data.
# Each monster page maps NPC IDs to their correct per-variant max hit.
# This is far more accurate than OSRSBox, which uses a flat computed value.
#
# Usage: bash build_npc_data.sh
# Output: src/main/resources/com/aggrotag/npc_max_hits.json

set -e

WIKI_API="https://oldschool.runescape.wiki/api.php"
UA="AggroTagPlugin-DataBuilder/1.0 (github.com/yourusername/aggro-tag)"
OUTPUT="src/main/resources/com/aggrotag/npc_max_hits.json"
TMP_DIR="/tmp/aggrotag_wiki"
TITLES_FILE="$TMP_DIR/titles.txt"
RESULT_FILE="$TMP_DIR/result.json"

mkdir -p "$TMP_DIR"

echo "[1/4] Fetching all monster page titles from the wiki..."

> "$TITLES_FILE"
cmcontinue=""
page=0
while true; do
    if [ -z "$cmcontinue" ]; then
        URL="${WIKI_API}?action=query&list=categorymembers&cmtitle=Category:Monsters&cmlimit=500&format=json"
    else
        URL="${WIKI_API}?action=query&list=categorymembers&cmtitle=Category:Monsters&cmlimit=500&cmcontinue=${cmcontinue}&format=json"
    fi
    RESPONSE=$(curl -s -A "$UA" "$URL")
    echo "$RESPONSE" | jq -r '.query.categorymembers[].title' >> "$TITLES_FILE"
    count=$(echo "$RESPONSE" | jq '.query.categorymembers | length')
    page=$((page + 1))
    echo "  Page $page: got $count titles"
    cmcontinue=$(echo "$RESPONSE" | jq -r '.continue.cmcontinue // empty')
    if [ -z "$cmcontinue" ]; then break; fi
done

TOTAL=$(wc -l < "$TITLES_FILE")
echo "  Total monster pages: $TOTAL"

echo "[2/4] Fetching wikitext for all pages in batches of 50..."
echo "{}" > "$RESULT_FILE"

batch=()
batchnum=0
processed=0

while IFS= read -r title; do
    batch+=("$title")
    if [ "${#batch[@]}" -eq 50 ]; then
        batchnum=$((batchnum + 1))
        processed=$((processed + 50))
        # URL-encode the pipe-separated titles
        TITLES_PARAM=$(IFS='|'; echo "${batch[*]}" | sed 's/ /_/g')
        RESPONSE=$(curl -s -A "$UA" \
            --data-urlencode "titles=$TITLES_PARAM" \
            -d "action=query&prop=revisions&rvprop=content&rvslots=main&format=json" \
            "$WIKI_API")
        echo "$RESPONSE" >> "$TMP_DIR/batch_${batchnum}.json"
        echo "  Batch $batchnum: fetched ${#batch[@]} pages ($processed/$TOTAL)"
        batch=()
        sleep 0.5  # be polite to the wiki
    fi
done < "$TITLES_FILE"

# Handle the remaining batch
if [ "${#batch[@]}" -gt 0 ]; then
    batchnum=$((batchnum + 1))
    processed=$((processed + ${#batch[@]}))
    TITLES_PARAM=$(IFS='|'; echo "${batch[*]}" | sed 's/ /_/g')
    RESPONSE=$(curl -s -A "$UA" \
        --data-urlencode "titles=$TITLES_PARAM" \
        -d "action=query&prop=revisions&rvprop=content&rvslots=main&format=json" \
        "$WIKI_API")
    echo "$RESPONSE" >> "$TMP_DIR/batch_${batchnum}.json"
    echo "  Batch $batchnum: fetched ${#batch[@]} pages ($processed/$TOTAL)"
fi

echo "[3/4] Parsing wikitext to extract id→max_hit mappings..."

# Use jq + awk to parse all the Infobox Monster templates
# For each page, find all |idN= and |max hitN= pairs, then emit id:maxhit for each
cat "$TMP_DIR/batch_"*.json | jq -r '
  .query.pages | to_entries[] |
  .value.revisions[0].slots.main["*"] // ""
' | awk '
BEGIN { RS = ""; FS = "\n"; }
/\{\{Infobox Monster/ {
    # Process each Infobox Monster block
    block = $0
    # Extract all |idN= values and |max hitN= values
    n = split(block, lines, "\n")
    for (i = 1; i <= n; i++) {
        line = lines[i]
        # Match |id1 = 512,5086,5087  or |id = 512
        if (match(line, /^\|id([0-9]*) *= *([0-9, ]+)/, arr)) {
            suffix = arr[1]
            ids = arr[2]
            id_map[suffix] = ids
        }
        # Match |max hit1=2  or |max hit = 2
        if (match(line, /^\|max hit([0-9]*) *= *([0-9]+)/, arr)) {
            suffix = arr[1]
            maxhit_map[suffix] = arr[2]
        }
    }
    # Now emit id:maxhit pairs
    for (suffix in id_map) {
        mh_key = suffix
        if (!(mh_key in maxhit_map)) mh_key = ""
        if (mh_key in maxhit_map && maxhit_map[mh_key] > 0) {
            n_ids = split(id_map[suffix], id_arr, /[, ]+/)
            for (j = 1; j <= n_ids; j++) {
                id = id_arr[j]
                gsub(/[^0-9]/, "", id)
                if (id != "" && id > 0) {
                    print id ":" maxhit_map[mh_key]
                }
            }
        }
    }
    delete id_map
    delete maxhit_map
}
' | sort -t: -k1,1n | awk -F: '
BEGIN { print "{"; first=1 }
!seen[$1]++ {
    if (!first) printf ",\n"
    printf "\"" $1 "\":" $2
    first=0
}
END { print "\n}" }
' > "$OUTPUT"

COUNT=$(jq 'length' "$OUTPUT")
echo "[4/4] Done! $COUNT NPC IDs written to $OUTPUT"
echo ""
echo "Spot-checking Dark Wizard variants:"
echo "  ID 512  (Lvl 7,  expected 2): $(jq '."512"' "$OUTPUT")"
echo "  ID 2058 (Lvl 11, expected 5): $(jq '."2058"' "$OUTPUT")"
echo "  ID 510  (Lvl 20, expected 6): $(jq '."510"' "$OUTPUT")"
echo "  ID 2057 (Lvl 22, expected 5): $(jq '."2057"' "$OUTPUT")"
echo "  ID 3033 (Goblin Lvl 2, expected 1): $(jq '."3033"' "$OUTPUT")"
