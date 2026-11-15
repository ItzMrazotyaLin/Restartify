# Справочник часовых поясов (IANA ZoneId)

[Read in English](TIMEZONES.md)

Список популярных и основных идентификаторов часовых поясов `java.time.ZoneId` для параметра `scheduled-restart.timezone` в файле `config.yml`.

> **Пример настройки в `config.yml`:**
> ```yaml
> scheduled-restart:
>   enabled: true
>   timezone: "Europe/Moscow"
>   times:
>     - "04:00"
>     - "16:00"
> ```
> При наличии опечатки в названии часового пояса плагин выведет предупреждение в консоль и автоматически использует системный часовой пояс сервера.  
> Для регионов с переходом на летнее время указаны оба смещения (зима / лето `DST`).

---

## Страны СНГ

### Россия (от Калининграда до Камчатки)

| Идентификатор (`ZoneId`) | Смещение UTC | Город / регион |
|---|---|---|
| `Europe/Kaliningrad` | UTC+2 | Калининград |
| `Europe/Moscow` | UTC+3 | Москва, Санкт-Петербург, центральная Россия |
| `Europe/Kirov` | UTC+3 | Киров |
| `Europe/Volgograd` | UTC+3 | Волгоград |
| `Europe/Samara` | UTC+4 | Самара |
| `Europe/Saratov` | UTC+4 | Саратов |
| `Europe/Ulyanovsk` | UTC+4 | Ульяновск |
| `Europe/Astrakhan` | UTC+4 | Астрахань |
| `Asia/Yekaterinburg` | UTC+5 | Екатеринбург, Пермь, Уфа, Челябинск, Тюмень |
| `Asia/Omsk` | UTC+6 | Омск |
| `Asia/Novosibirsk` | UTC+7 | Новосибирск |
| `Asia/Barnaul` | UTC+7 | Барнаул |
| `Asia/Tomsk` | UTC+7 | Томск |
| `Asia/Novokuznetsk` | UTC+7 | Новокузнецк, Кемерово |
| `Asia/Krasnoyarsk` | UTC+7 | Красноярск |
| `Asia/Irkutsk` | UTC+8 | Иркутск, Улан-Удэ |
| `Asia/Chita` | UTC+9 | Чита |
| `Asia/Yakutsk` | UTC+9 | Якутск |
| `Asia/Khandyga` | UTC+9 | Хандыга |
| `Asia/Vladivostok` | UTC+10 | Владивосток, Хабаровск |
| `Asia/Ust-Nera` | UTC+10 | Усть-Нера |
| `Asia/Magadan` | UTC+11 | Магадан |
| `Asia/Sakhalin` | UTC+11 | Южно-Сахалинск |
| `Asia/Srednekolymsk` | UTC+11 | Среднеколымск |
| `Asia/Kamchatka` | UTC+12 | Петропавловск-Камчатский |
| `Asia/Anadyr` | UTC+12 | Анадырь |

### Беларусь, Казахстан, Украина и другие страны СНГ и Кавказа

| Идентификатор (`ZoneId`) | Смещение UTC | Страна / город |
|---|---|---|
| `Europe/Minsk` | UTC+3 | Беларусь (Минск) |
| `Asia/Almaty` | UTC+5 | Казахстан (Алматы, Астана) |
| `Asia/Aqtobe` | UTC+5 | Казахстан (Актобе) |
| `Asia/Aqtau` | UTC+5 | Казахстан (Актау) |
| `Asia/Atyrau` | UTC+5 | Казахстан (Атырау) |
| `Asia/Oral` | UTC+5 | Казахстан (Уральск) |
| `Asia/Qostanay` | UTC+5 | Казахстан (Костанай) |
| `Asia/Qyzylorda` | UTC+5 | Казахстан (Кызылорда) |
| `Europe/Kyiv` | UTC+2 / UTC+3 (DST) | Украина (Киев) |
| `Asia/Tashkent` | UTC+5 | Узбекистан (Ташкент) |
| `Asia/Samarkand` | UTC+5 | Узбекистан (Самарканд) |
| `Asia/Yerevan` | UTC+4 | Армения (Ереван) |
| `Asia/Tbilisi` | UTC+4 | Грузия (Тбилиси) |
| `Asia/Baku` | UTC+4 | Азербайджан (Баку) |
| `Asia/Bishkek` | UTC+6 | Кыргызстан (Бишкек) |
| `Asia/Dushanbe` | UTC+5 | Таджикистан (Душанбе) |
| `Asia/Ashgabat` | UTC+5 | Туркменистан (Ашхабад) |
| `Europe/Chisinau` | UTC+2 / UTC+3 (DST) | Молдова (Кишинёв) |

---

## Европа

| Идентификатор (`ZoneId`) | Смещение UTC | Город / страна |
|---|---|---|
| `Europe/London` | UTC+0 / UTC+1 (DST) | Великобритания (Лондон) |
| `Europe/Dublin` | UTC+0 / UTC+1 (DST) | Ирландия (Дублин) |
| `Europe/Lisbon` | UTC+0 / UTC+1 (DST) | Португалия (Лиссабон) |
| `Atlantic/Reykjavik` | UTC+0 | Исландия (Рейкьявик) |
| `Europe/Madrid` | UTC+1 / UTC+2 (DST) | Испания (Мадрид) |
| `Europe/Paris` | UTC+1 / UTC+2 (DST) | Франция (Париж) |
| `Europe/Brussels` | UTC+1 / UTC+2 (DST) | Бельгия (Брюссель) |
| `Europe/Amsterdam` | UTC+1 / UTC+2 (DST) | Нидерланды (Амстердам) |
| `Europe/Berlin` | UTC+1 / UTC+2 (DST) | Германия (Берлин) |
| `Europe/Zurich` | UTC+1 / UTC+2 (DST) | Швейцария (Цюрих) |
| `Europe/Vienna` | UTC+1 / UTC+2 (DST) | Австрия (Вена) |
| `Europe/Rome` | UTC+1 / UTC+2 (DST) | Италия (Рим) |
| `Europe/Prague` | UTC+1 / UTC+2 (DST) | Чехия (Прага) |
| `Europe/Warsaw` | UTC+1 / UTC+2 (DST) | Польша (Варшава) |
| `Europe/Budapest` | UTC+1 / UTC+2 (DST) | Венгрия (Будапешт) |
| `Europe/Belgrade` | UTC+1 / UTC+2 (DST) | Сербия (Белград) |
| `Europe/Copenhagen` | UTC+1 / UTC+2 (DST) | Дания (Копенгаген) |
| `Europe/Oslo` | UTC+1 / UTC+2 (DST) | Норвегия (Осло) |
| `Europe/Stockholm` | UTC+1 / UTC+2 (DST) | Швеция (Стокгольм) |
| `Europe/Helsinki` | UTC+2 / UTC+3 (DST) | Финляндия (Хельсинки) |
| `Europe/Tallinn` | UTC+2 / UTC+3 (DST) | Эстония (Таллин) |
| `Europe/Riga` | UTC+2 / UTC+3 (DST) | Латвия (Рига) |
| `Europe/Vilnius` | UTC+2 / UTC+3 (DST) | Литва (Вильнюс) |
| `Europe/Bucharest` | UTC+2 / UTC+3 (DST) | Румыния (Бухарест) |
| `Europe/Sofia` | UTC+2 / UTC+3 (DST) | Болгария (София) |
| `Europe/Athens` | UTC+2 / UTC+3 (DST) | Греция (Афины) |
| `Europe/Istanbul` | UTC+3 | Турция (Стамбул) |

---

## Азия и Ближний Восток

| Идентификатор (`ZoneId`) | Смещение UTC | Город / страна |
|---|---|---|
| `Asia/Jerusalem` | UTC+2 / UTC+3 (DST) | Израиль (Иерусалим) |
| `Asia/Riyadh` | UTC+3 | Саудовская Аравия (Эр-Рияд) |
| `Asia/Tehran` | UTC+3:30 | Иран (Тегеран) |
| `Asia/Dubai` | UTC+4 | ОАЭ (Дубай) |
| `Asia/Karachi` | UTC+5 | Пакистан (Карачи) |
| `Asia/Kolkata` | UTC+5:30 | Индия (Дели / Калькутта) |
| `Asia/Dhaka` | UTC+6 | Бангладеш (Дакка) |
| `Asia/Bangkok` | UTC+7 | Таиланд (Бангкок) |
| `Asia/Ho_Chi_Minh` | UTC+7 | Вьетнам (Хошимин) |
| `Asia/Jakarta` | UTC+7 | Индонезия (Джакарта) |
| `Asia/Singapore` | UTC+8 | Сингапур |
| `Asia/Kuala_Lumpur` | UTC+8 | Малайзия (Куала-Лумпур) |
| `Asia/Manila` | UTC+8 | Филиппины (Манила) |
| `Asia/Shanghai` | UTC+8 | Китай (Шанхай / Пекин) |
| `Asia/Hong_Kong` | UTC+8 | Гонконг |
| `Asia/Taipei` | UTC+8 | Тайвань (Тайбэй) |
| `Asia/Seoul` | UTC+9 | Южная Корея (Сеул) |
| `Asia/Tokyo` | UTC+9 | Япония (Токио) |

---

## Америка

| Идентификатор (`ZoneId`) | Смещение UTC | Город / регион |
|---|---|---|
| `Pacific/Honolulu` | UTC-10 | США — Гавайи (Гонолулу) |
| `America/Anchorage` | UTC-9 / UTC-8 (DST) | США — Аляска (Анкоридж) |
| `America/Los_Angeles` | UTC-8 / UTC-7 (DST) | США — Тихоокеанское время (Лос-Анджелес) |
| `America/Vancouver` | UTC-8 / UTC-7 (DST) | Канада (Ванкувер) |
| `America/Denver` | UTC-7 / UTC-6 (DST) | США — Горное время (Денвер) |
| `America/Phoenix` | UTC-7 | США — Аризона (Финикс, без DST) |
| `America/Chicago` | UTC-6 / UTC-5 (DST) | США — Центральное время (Чикаго) |
| `America/Mexico_City` | UTC-6 | Мексика (Мехико) |
| `America/New_York` | UTC-5 / UTC-4 (DST) | США — Восточное время (Нью-Йорк) |
| `America/Toronto` | UTC-5 / UTC-4 (DST) | Канада (Торонто) |
| `America/Bogota` | UTC-5 | Колумбия (Богота) |
| `America/Lima` | UTC-5 | Перу (Лима) |
| `America/Santiago` | UTC-4 / UTC-3 (DST) | Чили (Сантьяго) |
| `America/Sao_Paulo` | UTC-3 | Бразилия (Сан-Паулу) |
| `America/Argentina/Buenos_Aires` | UTC-3 | Аргентина (Буэнос-Айрес) |

---

## Океания и UTC / GMT

| Идентификатор (`ZoneId`) | Смещение UTC | Описание / регион |
|---|---|---|
| `UTC` | UTC+0 | Всемирное координированное время |
| `Etc/UTC` | UTC+0 | Канонический идентификатор UTC |
| `GMT` | UTC+0 | Время по Гринвичу |
| `Australia/Perth` | UTC+8 | Австралия — Перт |
| `Australia/Sydney` | UTC+10 / UTC+11 (DST) | Австралия — Сидней |
| `Australia/Melbourne` | UTC+10 / UTC+11 (DST) | Австралия — Мельбурн |
| `Pacific/Auckland` | UTC+12 / UTC+13 (DST) | Новая Зеландия — Окленд |
