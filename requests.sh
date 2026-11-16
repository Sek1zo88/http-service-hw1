#!/usr/bin/env bash

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"

extract_id() {
  sed -n 's/.*"id":"\([^"]*\)".*/\1/p'
}

section() {
  printf '\n\n======================================================================\n'
  printf '=== %s ===\n' "$1"
  printf '======================================================================\n'
}

section "1. Создаем тренера по Кроссфиту (вместимость 1): 201"
TRAINER_A_RESPONSE=$(curl -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван","specialty":"Crossfit","maxCapacity":1}')
printf '%s\n' "$TRAINER_A_RESPONSE"
TRAINER_A_ID=$(printf '%s' "$TRAINER_A_RESPONSE" | extract_id)

section "2. Создаем тренера по Йоге (вместимость 2): 201"
TRAINER_B_RESPONSE=$(curl -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна","specialty":"Yoga","maxCapacity":2}')
printf '%s\n' "$TRAINER_B_RESPONSE"
TRAINER_B_ID=$(printf '%s' "$TRAINER_B_RESPONSE" | extract_id)

section "3. Создаем Клиента с Базовым абонементом: 201"
CLIENT_BASIC_RESPONSE=$(curl -sS -X POST "$BASE_URL/clients" \
  -H "Content-Type: application/json" \
  -d '{"name":"Олег","type":"BASIC"}')
printf '%s\n' "$CLIENT_BASIC_RESPONSE"
CLIENT_BASIC_ID=$(printf '%s' "$CLIENT_BASIC_RESPONSE" | extract_id)

section "4. Создаем Клиента с Премиум абонементом: 201"
CLIENT_PREMIUM_RESPONSE=$(curl -sS -X POST "$BASE_URL/clients" \
  -H "Content-Type: application/json" \
  -d '{"name":"Мария","type":"PREMIUM"}')
printf '%s\n' "$CLIENT_PREMIUM_RESPONSE"
CLIENT_PREMIUM_ID=$(printf '%s' "$CLIENT_PREMIUM_RESPONSE" | extract_id)

section "5. Получение полных списков тренеров и клиентов без фильтрации: 200"
curl -i -sS "$BASE_URL/trainers"
printf '\n\n'
curl -i -sS "$BASE_URL/clients"

section "6. Получение тренера и клиента по ID: 200"
curl -i -sS "$BASE_URL/trainers/$TRAINER_A_ID"
printf '\n\n'
curl -i -sS "$BASE_URL/clients/$CLIENT_BASIC_ID"

section "7. Фильтрация: Получаем тренеров по Кроссфиту без учета регистра: 200"
curl -i -sS "$BASE_URL/trainers?specialty=crossfit"

section "8. Фильтрация: Получаем клиентов по типу абонемента без учета регистра: 200"
curl -i -sS "$BASE_URL/clients?type=basic"

section "9. БИЗНЕС-ОПЕРАЦИЯ: Базовый клиент записывается к Тренеру А: 200"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_A_ID"

section "10. БИЗНЕС-ПРАВИЛО 1: Базовый клиент пытается записаться второй раз: 409"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_B_ID"

section "11. БИЗНЕС-ПРАВИЛО 2: Премиум клиент пытается записаться к Тренеру А, но мест нет: 409"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_A_ID"

section "12. БИЗНЕС-ОПЕРАЦИЯ: Премиум клиент записывается к Тренеру Б: 200"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_B_ID"

section "13. БИЗНЕС-ПРАВИЛО 3: Попытка записаться к одному тренеру дважды: 409"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_B_ID"

section "14. Обновление данных тренера (PUT): 200"
curl -i -sS -X PUT "$BASE_URL/trainers/$TRAINER_B_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна Иванова","specialty":"Yoga","maxCapacity":2}'

section "15. Обновление данных клиента (PUT): 200"
curl -i -sS -X PUT "$BASE_URL/clients/$CLIENT_BASIC_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Олег Петров","type":"BASIC"}'

section "16. Удаление тренера А освобождает лимит Базового клиента: 204"
curl -i -sS -X DELETE "$BASE_URL/trainers/$TRAINER_A_ID"

section "17. БИЗНЕС-ОПЕРАЦИЯ: Базовый клиент записывается к Тренеру Б после удаления Тренера А: 200"
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_B_ID"

section "18. БИЗНЕС-ПРАВИЛО 4: Понижение вместимости тренера ниже текущих записей: 409"
curl -i -sS -X PUT "$BASE_URL/trainers/$TRAINER_B_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна Иванова","specialty":"Yoga","maxCapacity":1}'

section "19. Создаем третьего тренера и проверяем запрет понижения абонемента клиента: 409"
TRAINER_C_RESPONSE=$(curl -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Виктор","specialty":"Boxing","maxCapacity":3}')
printf '%s\n' "$TRAINER_C_RESPONSE"
TRAINER_C_ID=$(printf '%s' "$TRAINER_C_RESPONSE" | extract_id)

curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_C_ID"
printf '\n\n'
curl -i -sS -X PUT "$BASE_URL/clients/$CLIENT_PREMIUM_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Мария","type":"BASIC"}'

section "20. Удаление клиента освобождает место у Тренера Б (409 -> DELETE 204 -> GET 200 -> 200)"
CLIENT_EXTRA_RESPONSE=$(curl -sS -X POST "$BASE_URL/clients" \
  -H "Content-Type: application/json" \
  -d '{"name":"Дмитрий","type":"BASIC"}')
printf '%s\n' "$CLIENT_EXTRA_RESPONSE"
CLIENT_EXTRA_ID=$(printf '%s' "$CLIENT_EXTRA_RESPONSE" | extract_id)

curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_EXTRA_ID/book?trainerId=$TRAINER_B_ID"
printf '\n\n'
curl -i -sS -X DELETE "$BASE_URL/clients/$CLIENT_BASIC_ID"
printf '\n\n'
curl -i -sS "$BASE_URL/trainers/$TRAINER_B_ID"
printf '\n\n'
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_EXTRA_ID/book?trainerId=$TRAINER_B_ID"

section "21. ОШИБКА: Запрос к несуществующим ресурсам: 404"
curl -i -sS "$BASE_URL/trainers/invalid-id-123"
printf '\n\n'
curl -i -sS "$BASE_URL/clients/invalid-id-123"

section "22. ПРОВЕРКА ВАЛИДАЦИИ: Пустое имя и некорректная вместимость (0 и -1): 400"
curl -i -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":" ","specialty":"Yoga","maxCapacity":1}'
printf '\n\n'
curl -i -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ошибочный","specialty":"Crossfit","maxCapacity":0}'
printf '\n\n'
curl -i -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ошибочный","specialty":"Crossfit","maxCapacity":-1}'

section "23. ПРОВЕРКА ВАЛИДАЦИИ: Неизвестный тип абонемента при создании и в фильтре: 400"
curl -i -sS -X POST "$BASE_URL/clients" \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван","type":"UNKNOWN"}'
printf '\n\n'
curl -i -sS "$BASE_URL/clients?type=UNKNOWN"

section "24. ПРОВЕРКА ВАЛИДАЦИИ: Некорректный JSON, null и отсутствие trainerId: 400"
curl -i -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Сломанный JSON","maxCapacity":}'
printf '\n\n'
curl -i -sS -X POST "$BASE_URL/trainers" \
  -H "Content-Type: application/json" \
  -d 'null'
printf '\n\n'
curl -i -sS -X POST "$BASE_URL/clients/$CLIENT_EXTRA_ID/book"