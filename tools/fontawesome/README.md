# Font Awesome Free 7.0.0 (solid) for habit icons

Same major version as the web app (`@fortawesome/free-solid-svg-icons` 7.0.0), so a
stored icon name (e.g. `book-open`) means the same icon on both sides.

Regenerate:

```bash
python3 -m venv .venv && .venv/bin/pip install fonttools brotli pyyaml
curl -LO https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.0.0/webfonts/fa-solid-900.woff2
curl -LO https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.0.0/metadata/icons.yml
.venv/bin/python -c "from fontTools.ttLib import TTFont; f=TTFont('fa-solid-900.woff2'); f.flavor=None; f.save('fa_solid.ttf')"
.venv/bin/python build_icons.py icons.yml fa_solid.ttf fa_icons.json
cp fa_solid.ttf ../../app/src/main/res/font/ && cp fa_icons.json ../../app/src/main/assets/
```

`fa_icons.json`: `[name, unicodeHex, searchTerms]`, solid icons present in the free font,
with Spanish terms added for common habits. License: icons CC BY 4.0, font SIL OFL 1.1
(see LICENSE-fontawesome-free.txt); credited in the app's recovery sheet.
