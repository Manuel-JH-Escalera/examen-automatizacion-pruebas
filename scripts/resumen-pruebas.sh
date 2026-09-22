#!/usr/bin/env bash
# Genera un resumen en Markdown de los reportes XML de Surefire/Failsafe
# para mostrarlo en el "Summary" de la ejecución en GitHub Actions.
# Uso: scripts/resumen-pruebas.sh "Título" target/surefire-reports
set -euo pipefail

titulo="$1"
dir="$2"

echo "## $titulo"
echo
if ! ls "$dir"/TEST-*.xml >/dev/null 2>&1; then
  echo "_No se encontraron reportes en ${dir}_"
  exit 0
fi

echo "| Clase de prueba | Ejecutadas | Fallidas | Errores | Omitidas |"
echo "|---|---|---|---|---|"
python3 - "$dir" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
total = [0, 0, 0, 0]
for f in sorted(glob.glob(sys.argv[1] + "/TEST-*.xml")):
    r = ET.parse(f).getroot()
    vals = [int(r.get(k, 0)) for k in ("tests", "failures", "errors", "skipped")]
    total = [a + b for a, b in zip(total, vals)]
    print(f"| `{r.get('name')}` | {vals[0]} | {vals[1]} | {vals[2]} | {vals[3]} |")
print(f"| **Total** | **{total[0]}** | **{total[1]}** | **{total[2]}** | **{total[3]}** |")
PY
