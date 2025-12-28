# Vacancy API JMeter Plan

Сценарий в `load-tests/vacancy-api.jmx` эмулирует типичные действия:
- чтение списка вакансий (`GET /answer`) с разными фильтрами и сортировками;
- чтение метрик (`GET /stats`);
- запись/парсинг новой вакансии (`POST /parse`) с разными URL из CSV.


## Что покрыто чек-листом
- **Два типа запросов**: GET (`/answer`, `/stats`) и POST (`/parse`).
- **Валидация**: Response Assertion на код 200 и JSR223-проверки JSON (наличие `title` в списке, `totalParsed`, echo `url` после POST).
- **Многопользовательская нагрузка**: Thread Group с `${threads} > 1`, плавный запуск через `${rampUp}`, задержки 500 мс между запросами.
- **Разные входные данные**: CSV Data Set `vacancy-params.csv` (URL, city, source, sortBy).




