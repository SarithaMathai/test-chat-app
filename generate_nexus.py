import json

with open('nexus_items_export.json', 'r') as f:
    items = json.load(f)

lines = []
lines.append('package com.testmonochat.tools.constant')
lines.append('')
lines.append('import com.testmonochat.tools.model.NexusItem')
lines.append('')
lines.append('/**')
lines.append(' * Static Nexus AI attribute data.')
lines.append(' *')
lines.append(' * Contains all known attribute items grouped by type:')
lines.append(' * Color Family (17), Color Name (305), Color Tone (11),')
lines.append(' * Pattern (298), Pattern Group (16), Product Size (1000).')
lines.append(' */')
lines.append('object NexusData {')
lines.append('')
lines.append('    val NEXUS_ITEMS: List<NexusItem> = listOf(')

for i, item in enumerate(items):
    name_escaped = item['name'].replace('\\', '\\\\').replace('"', '\\"')
    comma = ',' if i < len(items) - 1 else ''
    lines.append(f'        NexusItem(id = "{item["id"]}", name = "{name_escaped}", type = "{item["type"]}"){comma}')

lines.append('    )')
lines.append('}')

output_path = 'test-mono-chst-tools/src/main/kotlin/com/testmonochat/tools/constant/NexusData.kt'
with open(output_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print(f'Generated {output_path} with {len(items)} items')
