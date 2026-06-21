import urllib.request
import urllib.parse
import json
import re
import time
import os

WIKI_API = "https://oldschool.runescape.wiki/api.php"
UA = "AggroTagPlugin-DataBuilder/1.0 (github.com/yourusername/aggro-tag)"
OUTPUT = "src/main/resources/com/aggrotag/npc_data.json"

faction_map = {}
for i in [690,695,2209,2210,2211,2212,2213,2214]: faction_map[i] = "saradomin"
for i in [2233,2234,2235,2236,2237,2238,2239,2240,2241,2242,2243,2244,2245,2246,2247,2248,2249]: faction_map[i] = "bandos"
for i in [3133,3135,3136,3137,3139,3140,3141,3159,3160,3161,8997]: faction_map[i] = "zamorak"
for i in [3166,3167,3168,3169,3170,3171,3172,3173,3174,3175,3176,3177,3178,3179,3180,3181,3182,3183]: faction_map[i] = "armadyl"
for i in [11290,11291,11293]: faction_map[i] = "zaros"

def fetch_json(url, data=None):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    if data:
        data = urllib.parse.urlencode(data).encode('utf-8')
    with urllib.request.urlopen(req, data=data) as response:
        return json.loads(response.read().decode('utf-8'))

print("[1/4] Fetching all monster and NPC page titles from the wiki...")
titles = set()
for template in ["Template:Infobox_Monster", "Template:Infobox_NPC"]:
    eicontinue = ""
    page = 0
    while True:
        url = f"{WIKI_API}?action=query&list=embeddedin&eititle={template}&eilimit=500&format=json"
        if eicontinue:
            url += f"&eicontinue={eicontinue}"
        data = fetch_json(url)
        members = data.get("query", {}).get("embeddedin", [])
        titles.update([m["title"] for m in members])
        page += 1
        print(f"  {template} Page {page}: got {len(members)} titles")
        eicontinue = data.get("continue", {}).get("eicontinue")
        if not eicontinue:
            break

titles = list(titles)
total = len(titles)
print(f"  Total pages to scan: {total}")

print("[2/4] Fetching wikitext and parsing in batches of 50...")
final_data = {}
processed = 0

for i in range(0, total, 50):
    batch = titles[i:i+50]
    processed += len(batch)
    print(f"  Batch {i//50 + 1}: fetched {len(batch)} pages ({processed}/{total})")
    titles_param = "|".join(batch).replace(" ", "_")
    
    post_data = {
        "action": "query",
        "prop": "revisions",
        "rvprop": "content",
        "rvslots": "main",
        "format": "json",
        "titles": titles_param
    }
    
    resp = fetch_json(WIKI_API, post_data)
    pages = resp.get("query", {}).get("pages", {})
    
    for page_id, page_data in pages.items():
        if "revisions" not in page_data: continue
        wikitext = page_data["revisions"][0]["slots"]["main"].get("*", "")
        
        # Split by Infobox Monster or Infobox NPC
        boxes = re.split(r"\{\{Infobox (?:Monster|NPC)", wikitext)
        for b in boxes[1:]:
            lines = b.split("\n")
            
            id_map = {}
            maxhit_map = {}
            aggro_map = {}
            style_map = {}
            slayer_map = {}
            
            for line in lines:
                # |id1 = 512,5086,5087
                m = re.match(r"^\|id([0-9]*)\s*=\s*([0-9,\s]+)", line)
                if m:
                    id_map[m.group(1)] = m.group(2)
                
                m = re.match(r"^\|max hit([0-9]*)\s*=\s*([0-9]+)", line)
                if m:
                    maxhit_map[m.group(1)] = int(m.group(2))
                    
                m = re.match(r"^\|aggressive([0-9]*)\s*=\s*(.*)", line)
                if m:
                    suffix = m.group(1)
                    val = m.group(2).strip().lower()
                    if val == "yes": aggro_map[suffix] = 1
                    elif val == "no": aggro_map[suffix] = 0
                    
                m = re.match(r"^\|attack style([0-9]*)\s*=\s*(.*)", line)
                if m:
                    suffix = m.group(1)
                    val = m.group(2).lower()
                    mask = 0
                    if re.search(r"melee|crush|slash|stab", val): mask += 1
                    if "ranged" in val: mask += 2
                    if re.search(r"magic|dragonfire", val): mask += 4
                    if mask > 0: style_map[suffix] = mask

                m = re.match(r"^\|slayer[ _]?level([0-9]*)\s*=\s*([0-9]+)", line, re.IGNORECASE)
                if m:
                    slayer_map[m.group(1)] = int(m.group(2))
            
            for suffix, ids_str in id_map.items():
                mh_key = suffix if suffix in maxhit_map else ("" if "" in maxhit_map else None)
                ag_key = suffix if suffix in aggro_map else ("" if "" in aggro_map else None)
                st_key = suffix if suffix in style_map else ("" if "" in style_map else None)
                sl_key = suffix if suffix in slayer_map else ("" if "" in slayer_map else None)
                
                for npc_id_str in re.split(r"[, \n]+", ids_str):
                    npc_id_str = re.sub(r"[^0-9]", "", npc_id_str)
                    if not npc_id_str: continue
                    npc_id = int(npc_id_str)
                    if npc_id <= 0: continue
                    
                    obj = {}
                    if mh_key is not None and maxhit_map[mh_key] >= 0:
                        obj["m"] = maxhit_map[mh_key]
                    if ag_key is not None:
                        obj["a"] = aggro_map[ag_key]
                    if st_key is not None:
                        obj["s"] = style_map[st_key]
                    if sl_key is not None and slayer_map[sl_key] > 0:
                        obj["l"] = slayer_map[sl_key]
                    if npc_id in faction_map:
                        obj["f"] = faction_map[npc_id]
                        
                    if obj:
                        final_data[str(npc_id)] = obj

print(f"[3/4] Writing output to {OUTPUT}...")
# Sort by ID (numeric)
sorted_data = {k: final_data[k] for k in sorted(final_data.keys(), key=int)}
with open(OUTPUT, "w", encoding="utf-8") as f:
    f.write(json.dumps(sorted_data, separators=(',', ':')))

print(f"[4/4] Done! {len(sorted_data)} NPC IDs written to {OUTPUT}")

print("\nSpot-checking Dark Wizard variants:")
def check(nid):
    print(f"  ID {nid}: {json.dumps(sorted_data.get(str(nid)))}")

check(512)
check(2058)
check(510)
check(2057)
check(3033)
