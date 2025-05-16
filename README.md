# Explore With Me (Исследуй со мной)

Приложение-афиша, позволяющее пользователям делиться информацией об интересных событиях и находить компанию для участия в них. Проект разрабатывается в рамках обучения в Яндекс Практикуме.

## Оглавление

- [Технологии](#технологии)
- [Структура проекта](#структура-проекта)
- [Начало работы](#начало-работы)
    - [Предварительные требования](#предварительные-требования)
    - [Сборка проекта](#сборка-проекта)
    - [Запуск с использованием Docker Compose](#запуск-с-использованием-docker-compose)
    - [Локальный запуск для разработки (IntelliJ IDEA)](#локальный-запуск-для-разработки-intellij-idea)
        - [Локальный запуск Stats Service](#локальный-запуск-stats-service)
        - [Локальный запуск Main Service](#локальный-запуск-main-service)
- [Тестирование](#тестирование)
- [Дополнительная функциональность](#дополнительная-функциональность)
- [Планы по использованию OpenAPI Generator](#планы-по-использованию-openapi-generator)
- [Команда](#команда)

## Технологии

- Java 21
- Spring Boot 3.4.5 # Убедитесь, что версия актуальна (в вашем корневом pom.xml указана эта версия)
- Spring Data JPA
- Spring MVC
- PostgreSQL 16.1 # Можно уточнить версию PostgreSQL
- Maven
- Docker / Docker Compose
- Lombok
- JUnit 5, Mockito
- Testcontainers
- Checkstyle, Spotbugs, Jacoco (для контроля качества кода)

## Структура проекта

Проект является многомодульным Maven-проектом и состоит из следующих основных частей:

- `explore-with-me` (корневой POM)
    - `ewm-common`: Общий модуль, содержащий классы, используемые как основным сервисом, так и сервисом статистики (например, `ApiError.java`).
    - `main-service`: Основной сервис приложения. Отвечает за бизнес-логику, управление пользователями, событиями, категориями, подборками и запросами на участие.
        - `Dockerfile`
    - `stats-service` (родительский POM для модулей статистики)
        - `stats-dto`: Data Transfer Objects (DTO) для сервиса статистики.
        - `stats-client`: HTTP-клиент для взаимодействия с сервисом статистики (используется `main-service`).
        - `stats-server`: Сервис статистики (сбор и предоставление данных о запросах к эндпоинтам).
            - `Dockerfile`

## Начало работы

### Предварительные требования

Для работы с проектом вам понадобятся:

- JDK 21 (или выше, совместимая с Java 21)
- Apache Maven 3.6+
- Docker и Docker Compose
- IntelliJ IDEA (рекомендуется) или другая IDE с поддержкой Maven и Spring Boot.

### Сборка проекта

Для сборки всех модулей проекта выполните следующую команду в корневой директории:

```bash
mvn clean install
```
Эта команда также запустит статические анализаторы кода (Checkstyle, Spotbugs) и юнит-тесты.

### Запуск с использованием Docker Compose

Наиболее предпочтительный способ запуска всего приложения – использование Docker Compose. Это обеспечит запуск всех сервисов (`main-service`, `stats-server`) и их соответствующих баз данных PostgreSQL в изолированных контейнерах.

1.  **Соберите проект (если не делали ранее):**
    ```bash
    mvn clean install
    ```
2.  **Запустите сервисы:**
    В корневой директории проекта выполните:
    ```bash
    docker-compose up --build -d
    ```
    Ключ `-d` запускает контейнеры в фоновом режиме.
    Эта команда соберет Docker-образы для `stats-server` и `main-service` и запустит их вместе с необходимыми базами данных PostgreSQL.
    - Сервис статистики (`stats-server`) будет доступен по адресу: `http://localhost:9090`
    - Основной сервис (`main-service`) будет доступен по адресу: `http://localhost:8080`

3.  **Просмотр логов (при запуске с `-d`):**
    ```bash
    docker-compose logs -f main-service
    docker-compose logs -f stats-server
    # или docker-compose logs -f для всех сервисов
    ```

4.  **Остановка сервисов:**
    ```bash
    docker-compose down
    ```
    Для удаления volumes (данных БД):
    ```bash
    docker-compose down -v
    ```

### Локальный запуск для разработки (IntelliJ IDEA)

Для удобства разработки и отладки можно запускать сервисы локально из IntelliJ IDEA.

#### Локальный запуск Stats Service

Предусмотрен профиль запуска `stat-local` в IntelliJ IDEA для `stats-server`.

1.  **Настройка базы данных для `stats-server`:**
    Убедитесь, что у вас локально запущен экземпляр PostgreSQL, доступный по адресу, указанному в `stats-service/stats-server/src/main/resources/application-local.yml`.
    Примерные параметры для `application-local.yml`:
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:6543/ewm_stats_db # Убедитесь, что порт и имя БД соответствуют вашей локальной PG для stats-db
        username: stats_user # Ваш пользователь
        password: stats_password # Ваш пароль
      jpa:
        hibernate:
          ddl-auto: update # или create-drop для локальной разработки
    # ... другие настройки, если нужны ...
    ```
    *Примечание: Вам может потребоваться создать базу данных `ewm_stats_db` и пользователя `stats_user` вручную, если они еще не существуют.*

2.  **Запуск `StatsServerApplication`:**
    - Откройте проект в IntelliJ IDEA.
    - Найдите класс `StatsServerApplication.java` в модуле `stats-server`.
    - В репозитории должна быть предустановленная Run Configuration "stat-local" (проверьте `.idea/runConfigurations/`). Если нет, создайте новую конфигурацию Spring Boot:
        - **Main class:** `ru.practicum.explorewithme.stats.server.StatsServerApplication`
        - **VM options:** `-Dspring.profiles.active=local` (активирует `application-local.yml`)
        - **Working directory:** Корневая директория модуля `stats-server`.
    - Запустите эту конфигурацию.

#### Локальный запуск Main Service

Аналогично можно настроить локальный запуск для `main-service`.

1.  **Настройка базы данных для `main-service`:**
    Убедитесь, что у вас локально запущен экземпляр PostgreSQL, доступный по адресу, указанному в `main-service/src/main/resources/application-local.yml`.
    Создайте файл `application-local.yml` в `main-service/src/main/resources/` (если его еще нет) с примерным содержанием:
    ```yaml
    # URL сервиса статистики для локального запуска main-service,
    # если stats-server тоже запущен локально на порту 9090
    stats-server:
      url: http://localhost:9090

    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/ewm_main_db # Убедитесь, что порт и имя БД соответствуют вашей локальной PG для ewm-db
        username: ewm_user # Ваш пользователь
        password: ewm_password # Ваш пароль
      jpa:
        hibernate:
          ddl-auto: update # или create-drop для локальной разработки
    # ... другие настройки, если нужны ...
    ```
    *Примечание: Вам может потребоваться создать базу данных `ewm_main_db` и пользователя `ewm_user` вручную, если они еще не существуют.*
    *Также убедитесь, что сервис статистики (`stats-server`) запущен (локально или в Docker), если `main-service` будет к нему обращаться.*

2.  **Запуск `MainServiceApplication`:**
    - Найдите класс `MainServiceApplication.java` в модуле `main-service`.
    - В репозитории должна быть предустановленная Run Configuration "main-local" (проверьте `.idea/runConfigurations/`). Если нет, создайте новую конфигурацию Spring Boot:
        - **Main class:** `ru.practicum.explorewithme.main.MainServiceApplication`
        - **VM options:** `-Dspring.profiles.active=local` (активирует `application-local.yml`)
        - **Working directory:** Корневая директория модуля `main-service`. 
    - Запустите эту конфигурацию.

## Тестирование

Для запуска всех тестов в проекте выполните:

```bash
mvn test
```
Проект использует JUnit 5, Mockito и Testcontainers для различных уровней тестирования (юнит-тесты, интеграционные тесты с БД). Отчеты о покрытии кода (Jacoco) генерируются в `target/site/jacoco/`.

## Дополнительная функциональность

В рамках проекта командой была выбрана следующая дополнительная функциональность для реализации после основной части:

- **Основной выбор:** "Администрирование. Первый вариант"
    - Возможность для администратора добавлять конкретные локации — города, театры, концертные залы и другие в виде координат (широта, долгота, радиус).
    - Получение списка этих локаций.
    - Возможность поиска событий в конкретной локации.

- **Резервный вариант:** "Комментарии"
    - Возможность оставлять комментарии к событиям и модерировать их.

## Планы по использованию OpenAPI Generator

Команда планировала исследовать `openapi-generator-maven-plugin` для автоматической генерации DTO и, возможно, интерфейсов контроллеров на основе OpenAPI спецификаций. По результатам исследования ([ссылка на документ Леры или краткое резюме, если есть]) было принято решение на текущем этапе **отказаться от автоматической генерации DTO** в пользу ручного создания. Это связано с лучшим контролем над кодом, интеграцией с Lombok и Jackson, а также более точной настройкой валидации, что на данном этапе более эффективно для команды. Вопрос может быть пересмотрен в будущем при значительном увеличении количества DTO или частоты изменения API.

## Команда

- Иван Петровский (Team Lead) - [@impatient0](https://github.com/impatient0)
- Андрей Гагарский - [@Gagarskiy-Andrey](https://github.com/Gagarskiy-Andrey)
- Валерия Бутько - [@progingir](https://github.com/progingir)
- Сергей Филипповских - [@SergikF](https://github.com/SergikF)