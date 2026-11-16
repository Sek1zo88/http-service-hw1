#!/usr/bin/env bash

# Очищаем консоль
clear

echo "======================================================================"
echo "=== 1. Создаем тренера по Кроссфиту (вместимость 1) ==="
echo "======================================================================"
TRAINER_A_ID=$(curl -s -X POST http://localhost:8080/trainers \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван","specialty":"Crossfit","maxCapacity":1}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Trainer A ID: $TRAINER_A_ID"
echo ""

echo "======================================================================"
echo "=== 2. Создаем тренера по Йоге (вместимость 2) ==="
echo "======================================================================"
TRAINER_B_ID=$(curl -s -X POST http://localhost:8080/trainers \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна","specialty":"Yoga","maxCapacity":2}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Trainer B ID: $TRAINER_B_ID"
echo ""

echo "======================================================================"
echo "=== 3. Создаем Клиента с Базовым абонементом ==="
echo "======================================================================"
CLIENT_BASIC_ID=$(curl -s -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Олег","type":"BASIC"}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Basic Client ID: $CLIENT_BASIC_ID"
echo ""

echo "======================================================================"
echo "=== 4. Создаем Клиента с Премиум абонементом ==="
echo "======================================================================"
CLIENT_PREMIUM_ID=$(curl -s -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Мария","type":"PREMIUM"}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Premium Client ID: $CLIENT_PREMIUM_ID"
echo ""

echo "======================================================================"
echo "=== 5. Получение полных списков тренеров и клиентов без фильтрации (Успех 200) ==="
echo "======================================================================"
curl -i -w "\n\n" "http://localhost:8080/trainers"
curl -i -w "\n\n" "http://localhost:8080/clients"

echo "======================================================================"
echo "=== 6. Получение тренера и клиента по ID (Успех 200) ==="
echo "======================================================================"
curl -i -w "\n\n" "http://localhost:8080/trainers/$TRAINER_A_ID"
curl -i -w "\n\n" "http://localhost:8080/clients/$CLIENT_BASIC_ID"

echo "======================================================================"
echo "=== 7. Фильтрация: Получаем тренеров по Кроссфиту без учета регистра ==="
echo "======================================================================"
curl -i -w "\n\n" "http://localhost:8080/trainers?specialty=crossfit"

echo "======================================================================"
echo "=== 8. Фильтрация: Получаем клиентов по типу абонемента без учета регистра ==="
echo "======================================================================"
curl -i -w "\n\n" "http://localhost:8080/clients?type=basic"

echo "======================================================================"
echo "=== 9. БИЗНЕС-ОПЕРАЦИЯ: Базовый клиент записывается к Тренеру А (Успех) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_A_ID"

echo "======================================================================"
echo "=== 10. БИЗНЕС-ПРАВИЛО 1: Базовый клиент пытается записаться второй раз (ОШИБКА 409) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_B_ID"

echo "======================================================================"
echo "=== 11. БИЗНЕС-ПРАВИЛО 2: Премиум клиент пытается записаться к Тренеру А, но мест нет (ОШИБКА 409) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_A_ID"

echo "======================================================================"
echo "=== 12. БИЗНЕС-ОПЕРАЦИЯ: Премиум клиент записывается к Тренеру Б (Успех) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_B_ID"

echo "======================================================================"
echo "=== 13. БИЗНЕС-ПРАВИЛО 3: Попытка записаться к одному тренеру дважды (ОШИБКА 409) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_B_ID"

echo "======================================================================"
echo "=== 14. Обновление данных тренера (Успех 200) ==="
echo "======================================================================"
curl -i -w "\n\n" -X PUT "http://localhost:8080/trainers/$TRAINER_B_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна Иванова","specialty":"Yoga","maxCapacity":2}'

echo "======================================================================"
echo "=== 15. Обновление данных клиента (Успех 200) ==="
echo "======================================================================"
curl -i -w "\n\n" -X PUT "http://localhost:8080/clients/$CLIENT_BASIC_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Олег Петров","type":"BASIC"}'

echo "======================================================================"
echo "=== 16. Удаление тренера А освобождает лимит Базового клиента (Успех 204) ==="
echo "======================================================================"
curl -i -w "\n\n" -X DELETE "http://localhost:8080/trainers/$TRAINER_A_ID"

echo "======================================================================"
echo "=== 17. БИЗНЕС-ОПЕРАЦИЯ: Базовый клиент записывается к Тренеру Б после удаления Тренера А (Успех) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_BASIC_ID/book?trainerId=$TRAINER_B_ID"

echo "======================================================================"
echo "=== 18. БИЗНЕС-ПРАВИЛО 4: Понижение вместимости тренера ниже текущих записей (ОШИБКА 409) ==="
echo "======================================================================"
curl -i -w "\n\n" -X PUT "http://localhost:8080/trainers/$TRAINER_B_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Анна Иванова","specialty":"Yoga","maxCapacity":1}'

echo "======================================================================"
echo "=== 19. Создаем третьего тренера и проверяем запрет понижения абонемента клиента (ОШИБКА 409) ==="
echo "======================================================================"
TRAINER_C_ID=$(curl -s -X POST http://localhost:8080/trainers \
  -H "Content-Type: application/json" \
  -d '{"name":"Виктор","specialty":"Boxing","maxCapacity":3}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_PREMIUM_ID/book?trainerId=$TRAINER_C_ID"
curl -i -w "\n\n" -X PUT "http://localhost:8080/clients/$CLIENT_PREMIUM_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Мария","type":"BASIC"}'

echo "======================================================================"
echo "=== 20. Удаление клиента освобождает место у Тренера Б (409 -> DELETE 204 -> GET 200 -> 200) ==="
echo "======================================================================"
CLIENT_EXTRA_ID=$(curl -s -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Дмитрий","type":"BASIC"}' \
  | grep -o '"id":"[^"]*' | cut -d'"' -f4)
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_EXTRA_ID/book?trainerId=$TRAINER_B_ID"
curl -i -w "\n\n" -X DELETE "http://localhost:8080/clients/$CLIENT_BASIC_ID"
curl -i -w "\n\n" "http://localhost:8080/trainers/$TRAINER_B_ID"
curl -i -w "\n\n" -X POST "http://localhost:8080/clients/$CLIENT_EXTRA_ID/book?trainerId=$TRAINER_B_ID"

echo "======================================================================"
echo "=== 21. ОШИБКА: Запрос к несуществующему ресурсу (ОШИБКА 404) ==="
echo "======================================================================"
curl -i -w "\n\n" "http://localhost:8080/clients/invalid-id-123"

echo "======================================================================"
echo "=== 22. ПРОВЕРКА ВАЛИДАЦИИ: Создание тренера с отрицательной вместимостью (ОШИБКА 400) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST http://localhost:8080/trainers \
  -H "Content-Type: application/json" \
  -d '{"name":"Ошибочный","specialty":"Crossfit","maxCapacity":-1}'

echo "======================================================================"
echo "=== 23. ПРОВЕРКА ВАЛИДАЦИИ: Отправка некорректного JSON (ОШИБКА 400) ==="
echo "======================================================================"
curl -i -w "\n\n" -X POST http://localhost:8080/trainers \
  -H "Content-Type: application/json" \
  -d '{"name": "Сломанный JSON", "maxCapacity": }'