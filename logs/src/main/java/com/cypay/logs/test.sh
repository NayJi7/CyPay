#!/bin/bash

PORT=8081
BASE_URL="http://localhost:$PORT"

echo "=========================================="
echo "🧪 Tests du Log Service (Full Actor)"
echo "=========================================="
echo ""

# Fonction pour afficher les résultats
test_endpoint() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📋 Test: $name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if [ "$method" = "DELETE" ]; then
        response=$(curl -s -X DELETE "$url")
    else
        response=$(curl -s "$url")
    fi

    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo ""
    echo ""
}

# Attendre que le service soit prêt
echo "⏳ Vérification que le service est démarré..."
for i in {1..10}; do
    if curl -s "$BASE_URL/logs/stats" > /dev/null 2>&1; then
        echo "✅ Service prêt !"
        echo ""
        break
    fi
    if [ $i -eq 10 ]; then
        echo "❌ Service non accessible après 10 secondes"
        echo "   Assurez-vous que le service est démarré:"
        echo "   java -jar target/cypay-logs-service.jar"
        exit 1
    fi
    sleep 1
done

# Test 1: Récupérer tous les logs
test_endpoint \
    "Récupérer tous les logs (limité à 5)" \
    "$BASE_URL/logs?limit=5"

# Test 2: Logs d'un acteur spécifique
test_endpoint \
    "Logs du PaymentProcessor" \
    "$BASE_URL/logs/actor/PaymentProcessor"

# Test 3: Logs par niveau ERROR
test_endpoint \
    "Logs d'erreur uniquement" \
    "$BASE_URL/logs/level/ERROR"

# Test 4: Logs par niveau INFO
test_endpoint \
    "Logs INFO avec limite" \
    "$BASE_URL/logs/level/INFO?limit=3"

# Test 5: Statistiques
test_endpoint \
    "Statistiques globales" \
    "$BASE_URL/logs/stats"

# Test 6: Tous les logs sans limite (100 par défaut)
test_endpoint \
    "Tous les logs (limite par défaut)" \
    "$BASE_URL/logs"

# Test 7: Route inexistante (404)
test_endpoint \
    "Route inexistante (doit retourner 404)" \
    "$BASE_URL/logs/unknown"

echo "=========================================="
echo "✅ Tests terminés !"
echo "=========================================="
echo ""
echo "⚠️  Pour tester la suppression (ATTENTION: supprime tous les logs):"
echo "   curl -X DELETE $BASE_URL/logs | jq '.'"
echo ""
echo "📊 Architecture testée:"
echo "   [cURL] → [HttpReceiver] → [LogHttpActeur]"
echo "                          → [DatabaseActeur] → [PostgreSQL]"
echo "                          → [StatsActeur] → [PostgreSQL]"