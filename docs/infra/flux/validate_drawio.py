#!/usr/bin/env python3
"""Validate a .drawio flux diagram: box crossings + duplicate connection points.
Usage: python validate_drawio.py diagram.drawio
(Reusable validator from HOWTO-flux-diagrams-mermaid-to-drawio.md section 11.)"""
import sys, re
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else "diagram.drawio"
root = ET.parse(path).getroot()

def sval(style, key):
    m = re.search(rf'{key}=([-0-9.]+)', style or '')
    return float(m.group(1)) if m else None

boxes = {}
for c in root.iter('mxCell'):
    if c.get('vertex') == '1':
        g = c.find('mxGeometry')
        if g is None or g.get('x') is None:
            continue
        boxes[c.get('id')] = dict(x=float(g.get('x')), y=float(g.get('y')),
                                  w=float(g.get('width')), h=float(g.get('height')),
                                  style=c.get('style') or '')

# Treat as an "obstacle" only real component nodes. This guide reserves left space
# for a corner icon (spacingLeft=20), so that's a good heuristic — adjust to taste.
def is_obstacle(b): return 'spacingLeft' in b['style']

def point(bid, fx, fy):
    b = boxes[bid]
    return (b['x'] + (fx if fx is not None else 0.5) * b['w'],
            b['y'] + (fy if fy is not None else 0.5) * b['h'])

dups, crossings = {}, []
for c in root.iter('mxCell'):
    if c.get('edge') != '1':
        continue
    s = c.get('style') or ''
    ex, ey, nx, ny = sval(s,'exitX'), sval(s,'exitY'), sval(s,'entryX'), sval(s,'entryY')
    src, tgt = c.get('source'), c.get('target')
    if ex is not None and src: dups.setdefault((src, ex, ey), []).append(c.get('id'))
    if nx is not None and tgt: dups.setdefault((tgt, nx, ny), []).append(c.get('id'))
    if src not in boxes or tgt not in boxes:
        continue
    pts = [point(src, ex, ey)]
    g = c.find('mxGeometry')
    arr = g.find('Array') if g is not None else None
    if arr is not None:
        pts += [(float(p.get('x')), float(p.get('y'))) for p in arr.findall('mxPoint')]
    pts.append(point(tgt, nx, ny))
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        if abs(ax - bx) < 0.6 or abs(ay - by) < 0.6:          # axis-aligned segment
            x1, x2 = sorted((ax, bx)); y1, y2 = sorted((ay, by))
            for bid, b in boxes.items():
                if bid in (src, tgt) or not is_obstacle(b):
                    continue
                if (x1 <= b['x']+b['w']-2 and x2 >= b['x']+2 and
                    y1 <= b['y']+b['h']-2 and y2 >= b['y']+2):
                    crossings.append(f"{c.get('id')} crosses {bid}")

print(f"crossings: {len(crossings)}")
for x in crossings: print("  ", x)
dupes = {k: v for k, v in dups.items() if len(v) > 1}
print(f"duplicate connection points: {len(dupes)}")
for k, v in dupes.items(): print("  ", k, "->", v)
