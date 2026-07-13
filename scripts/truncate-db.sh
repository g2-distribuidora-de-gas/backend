#!/usr/bin/env bash
# =============================================================
# truncate-db.sh
# =============================================================
# Vacía los datos de las tablas (TRUNCATE) sin eliminar su estructura.
# Carga .env automaticamente si existe.
#
# Uso:
#   ./scripts/truncate-db.sh
#   ./scripts/truncate-db.sh -y   # sin pedir confirmacion
# =============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
SQL_FILE="$PROJECT_ROOT/src/main/resources/db/admin/truncate_schema.sql"

AUTO_YES=0
for arg in "$@"; do
    if [ "$arg" = "-y" ] || [ "$arg" = "--yes" ]; then
        AUTO_YES=1
    fi
done

echo ""
echo "================================================="
echo "  Truncate de esquema Supabase (Vaciar datos)"
echo "================================================="
echo ""

# Cargar .env si existe
if [ -f "$ENV_FILE" ]; then
    echo "[INFO] Cargando variables desde $ENV_FILE"
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    echo "[WARN] No se encontro .env en $ENV_FILE"
fi

# Validar variable requerida
if [ -z "${SUPABASE_DB_URL:-}" ]; then
    echo "[ERROR] SUPABASE_DB_URL no esta definida"
    echo "        Define la variable o crea un archivo .env"
    exit 1
fi

if [ ! -f "$SQL_FILE" ]; then
    echo "[ERROR] No se encontro el archivo SQL: $SQL_FILE"
    exit 1
fi

# Mostrar resumen
echo "Host destino: $SUPABASE_DB_URL"
echo "SQL a ejecutar: $SQL_FILE"
echo ""
echo -e "\033[33mEsto VACIARA (truncate) todos los datos de las tablas de la aplicacion,\033[0m"
echo -e "\033[33mpero MANTENDRA la estructura de las tablas intacta.\033[0m"
echo ""

# Confirmar
if [ "$AUTO_YES" -eq 0 ]; then
    read -p "Continuar? (s/N): " CONFIRM
    case "$CONFIRM" in
        s|S|y|Y) ;;
        *) echo "Operacion cancelada por el usuario."; exit 0 ;;
    esac
fi

# Verificar psql
if ! command -v psql >/dev/null 2>&1; then
    echo ""
    echo "[ERROR] psql no esta instalado o no esta en el PATH."
    echo "        Instala PostgreSQL client con tu package manager preferido."
    echo "        O ejecuta el script manualmente desde Supabase SQL Editor:"
    echo "        $SQL_FILE"
    exit 1
fi

echo "[INFO] Ejecutando truncate..."
PSQL_URL="${SUPABASE_DB_URL#jdbc:}"
PSQL_URL="${PSQL_URL%&*}"
psql "$PSQL_URL" -f "$SQL_FILE"

echo ""
echo "[OK] Truncate completado. Los datos han sido borrados."
