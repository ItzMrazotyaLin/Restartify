# Timezone Reference (IANA ZoneId)

[Прочитать на русском](TIMEZONES_RU.md)

This reference lists valid `java.time.ZoneId` identifiers (IANA format) and their UTC offsets for the `scheduled-restart.timezone` option in `config.yml`.

> **Example in `config.yml`:**
> ```yaml
> scheduled-restart:
>   enabled: true
>   timezone: "Europe/Moscow"
>   times:
>     - "04:00"
>     - "16:00"
> ```
> If an invalid timezone is entered, Restartify logs a warning to the console and automatically falls back to the server's system timezone (`ZoneId.systemDefault()`).  
> Regions observing Daylight Saving Time show both offsets (Standard / DST).

---

## Europe

### Western & Central Europe

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Europe/London` | UTC+0 / UTC+1 (DST) | United Kingdom (London) |
| `Europe/Dublin` | UTC+0 / UTC+1 (DST) | Ireland (Dublin) |
| `Europe/Lisbon` | UTC+0 / UTC+1 (DST) | Portugal (Lisbon) |
| `Atlantic/Reykjavik` | UTC+0 | Iceland (Reykjavik) |
| `Europe/Madrid` | UTC+1 / UTC+2 (DST) | Spain (Madrid) |
| `Europe/Paris` | UTC+1 / UTC+2 (DST) | France (Paris) |
| `Europe/Brussels` | UTC+1 / UTC+2 (DST) | Belgium (Brussels) |
| `Europe/Amsterdam` | UTC+1 / UTC+2 (DST) | Netherlands (Amsterdam) |
| `Europe/Luxembourg` | UTC+1 / UTC+2 (DST) | Luxembourg |
| `Europe/Berlin` | UTC+1 / UTC+2 (DST) | Germany (Berlin, Frankfurt) |
| `Europe/Zurich` | UTC+1 / UTC+2 (DST) | Switzerland (Zurich) |
| `Europe/Vienna` | UTC+1 / UTC+2 (DST) | Austria (Vienna) |
| `Europe/Rome` | UTC+1 / UTC+2 (DST) | Italy (Rome) |
| `Europe/Prague` | UTC+1 / UTC+2 (DST) | Czechia (Prague) |
| `Europe/Bratislava` | UTC+1 / UTC+2 (DST) | Slovakia (Bratislava) |
| `Europe/Warsaw` | UTC+1 / UTC+2 (DST) | Poland (Warsaw) |
| `Europe/Budapest` | UTC+1 / UTC+2 (DST) | Hungary (Budapest) |
| `Europe/Belgrade` | UTC+1 / UTC+2 (DST) | Serbia (Belgrade) |
| `Europe/Zagreb` | UTC+1 / UTC+2 (DST) | Croatia (Zagreb) |
| `Europe/Ljubljana` | UTC+1 / UTC+2 (DST) | Slovenia (Ljubljana) |
| `Europe/Copenhagen` | UTC+1 / UTC+2 (DST) | Denmark (Copenhagen) |
| `Europe/Oslo` | UTC+1 / UTC+2 (DST) | Norway (Oslo) |
| `Europe/Stockholm` | UTC+1 / UTC+2 (DST) | Sweden (Stockholm) |

### Eastern Europe

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Europe/Helsinki` | UTC+2 / UTC+3 (DST) | Finland (Helsinki) |
| `Europe/Tallinn` | UTC+2 / UTC+3 (DST) | Estonia (Tallinn) |
| `Europe/Riga` | UTC+2 / UTC+3 (DST) | Latvia (Riga) |
| `Europe/Vilnius` | UTC+2 / UTC+3 (DST) | Lithuania (Vilnius) |
| `Europe/Kyiv` | UTC+2 / UTC+3 (DST) | Ukraine (Kyiv) |
| `Europe/Chisinau` | UTC+2 / UTC+3 (DST) | Moldova (Chisinau) |
| `Europe/Bucharest` | UTC+2 / UTC+3 (DST) | Romania (Bucharest) |
| `Europe/Sofia` | UTC+2 / UTC+3 (DST) | Bulgaria (Sofia) |
| `Europe/Athens` | UTC+2 / UTC+3 (DST) | Greece (Athens) |
| `Europe/Nicosia` | UTC+2 / UTC+3 (DST) | Cyprus (Nicosia) |
| `Europe/Kaliningrad` | UTC+2 | Russia (Kaliningrad) |
| `Europe/Moscow` | UTC+3 | Russia (Moscow, Saint Petersburg) |
| `Europe/Volgograd` | UTC+3 | Russia (Volgograd) |
| `Europe/Minsk` | UTC+3 | Belarus (Minsk) |
| `Europe/Istanbul` | UTC+3 | Turkey (Istanbul) |
| `Europe/Samara` | UTC+4 | Russia (Samara) |

---

## Asia & Middle East

### East & Southeast Asia

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Asia/Bangkok` | UTC+7 | Thailand (Bangkok) |
| `Asia/Ho_Chi_Minh` | UTC+7 | Vietnam (Ho Chi Minh City) |
| `Asia/Jakarta` | UTC+7 | Indonesia (Jakarta) |
| `Asia/Singapore` | UTC+8 | Singapore |
| `Asia/Kuala_Lumpur` | UTC+8 | Malaysia (Kuala Lumpur) |
| `Asia/Manila` | UTC+8 | Philippines (Manila) |
| `Asia/Shanghai` | UTC+8 | China (Shanghai, Beijing) |
| `Asia/Hong_Kong` | UTC+8 | Hong Kong |
| `Asia/Taipei` | UTC+8 | Taiwan (Taipei) |
| `Asia/Ulaanbaatar` | UTC+8 | Mongolia (Ulaanbaatar) |
| `Asia/Seoul` | UTC+9 | South Korea (Seoul) |
| `Asia/Tokyo` | UTC+9 | Japan (Tokyo) |

### South & Central Asia

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Asia/Yekaterinburg` | UTC+5 | Russia (Yekaterinburg) |
| `Asia/Almaty` | UTC+5 | Kazakhstan (Almaty, Astana) |
| `Asia/Tashkent` | UTC+5 | Uzbekistan (Tashkent) |
| `Asia/Dushanbe` | UTC+5 | Tajikistan (Dushanbe) |
| `Asia/Ashgabat` | UTC+5 | Turkmenistan (Ashgabat) |
| `Asia/Karachi` | UTC+5 | Pakistan (Karachi) |
| `Asia/Kolkata` | UTC+5:30 | India (New Delhi, Mumbai, Kolkata) |
| `Asia/Colombo` | UTC+5:30 | Sri Lanka (Colombo) |
| `Asia/Kathmandu` | UTC+5:45 | Nepal (Kathmandu) |
| `Asia/Omsk` | UTC+6 | Russia (Omsk) |
| `Asia/Bishkek` | UTC+6 | Kyrgyzstan (Bishkek) |
| `Asia/Dhaka` | UTC+6 | Bangladesh (Dhaka) |
| `Asia/Yangon` | UTC+6:30 | Myanmar (Yangon) |
| `Asia/Novosibirsk` | UTC+7 | Russia (Novosibirsk) |
| `Asia/Krasnoyarsk` | UTC+7 | Russia (Krasnoyarsk) |
| `Asia/Irkutsk` | UTC+8 | Russia (Irkutsk) |
| `Asia/Yakutsk` | UTC+9 | Russia (Yakutsk) |
| `Asia/Vladivostok` | UTC+10 | Russia (Vladivostok) |
| `Asia/Magadan` | UTC+11 | Russia (Magadan) |
| `Asia/Kamchatka` | UTC+12 | Russia (Kamchatka) |

### Middle East & Caucasus

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Asia/Jerusalem` | UTC+2 / UTC+3 (DST) | Israel (Jerusalem) |
| `Asia/Beirut` | UTC+2 / UTC+3 (DST) | Lebanon (Beirut) |
| `Asia/Amman` | UTC+3 | Jordan (Amman) |
| `Asia/Riyadh` | UTC+3 | Saudi Arabia (Riyadh) |
| `Asia/Kuwait` | UTC+3 | Kuwait |
| `Asia/Qatar` | UTC+3 | Qatar (Doha) |
| `Asia/Baghdad` | UTC+3 | Iraq (Baghdad) |
| `Asia/Tehran` | UTC+3:30 | Iran (Tehran) |
| `Asia/Dubai` | UTC+4 | United Arab Emirates (Dubai) |
| `Asia/Muscat` | UTC+4 | Oman (Muscat) |
| `Asia/Baku` | UTC+4 | Azerbaijan (Baku) |
| `Asia/Tbilisi` | UTC+4 | Georgia (Tbilisi) |
| `Asia/Yerevan` | UTC+4 | Armenia (Yerevan) |
| `Asia/Kabul` | UTC+4:30 | Afghanistan (Kabul) |

---

## Americas

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Pacific/Honolulu` | UTC-10 | USA — Hawaii (Honolulu) |
| `America/Anchorage` | UTC-9 / UTC-8 (DST) | USA — Alaska (Anchorage) |
| `America/Los_Angeles` | UTC-8 / UTC-7 (DST) | USA — Pacific Time (Los Angeles) |
| `America/Vancouver` | UTC-8 / UTC-7 (DST) | Canada (Vancouver) |
| `America/Tijuana` | UTC-8 / UTC-7 (DST) | Mexico (Tijuana) |
| `America/Denver` | UTC-7 / UTC-6 (DST) | USA — Mountain Time (Denver) |
| `America/Phoenix` | UTC-7 | USA — Arizona (Phoenix, no DST) |
| `America/Edmonton` | UTC-7 / UTC-6 (DST) | Canada (Edmonton) |
| `America/Chicago` | UTC-6 / UTC-5 (DST) | USA — Central Time (Chicago) |
| `America/Winnipeg` | UTC-6 / UTC-5 (DST) | Canada (Winnipeg) |
| `America/Mexico_City` | UTC-6 | Mexico (Mexico City) |
| `America/Costa_Rica` | UTC-6 | Costa Rica |
| `America/New_York` | UTC-5 / UTC-4 (DST) | USA — Eastern Time (New York) |
| `America/Toronto` | UTC-5 / UTC-4 (DST) | Canada (Toronto) |
| `America/Havana` | UTC-5 / UTC-4 (DST) | Cuba (Havana) |
| `America/Bogota` | UTC-5 | Colombia (Bogota) |
| `America/Lima` | UTC-5 | Peru (Lima) |
| `America/Caracas` | UTC-4 | Venezuela (Caracas) |
| `America/Halifax` | UTC-4 / UTC-3 (DST) | Canada — Atlantic Time (Halifax) |
| `America/Santiago` | UTC-4 / UTC-3 (DST) | Chile (Santiago) |
| `America/St_Johns` | UTC-3:30 / UTC-2:30 (DST) | Canada — Newfoundland (St. John's) |
| `America/Sao_Paulo` | UTC-3 | Brazil (Sao Paulo) |
| `America/Argentina/Buenos_Aires` | UTC-3 | Argentina (Buenos Aires) |
| `America/Montevideo` | UTC-3 | Uruguay (Montevideo) |

---

## Oceania

| Identifier (`ZoneId`) | UTC Offset | Region / City |
|---|---|---|
| `Australia/Perth` | UTC+8 | Australia — Western (Perth) |
| `Australia/Darwin` | UTC+9:30 | Australia — Northern Territory (Darwin) |
| `Australia/Adelaide` | UTC+9:30 / UTC+10:30 (DST) | Australia — South (Adelaide) |
| `Australia/Brisbane` | UTC+10 | Australia — Queensland (Brisbane) |
| `Australia/Sydney` | UTC+10 / UTC+11 (DST) | Australia — New South Wales (Sydney) |
| `Australia/Melbourne` | UTC+10 / UTC+11 (DST) | Australia — Victoria (Melbourne) |
| `Australia/Hobart` | UTC+10 / UTC+11 (DST) | Australia — Tasmania (Hobart) |
| `Pacific/Guam` | UTC+10 | Guam |
| `Pacific/Fiji` | UTC+12 | Fiji |
| `Pacific/Auckland` | UTC+12 / UTC+13 (DST) | New Zealand (Auckland) |
| `Pacific/Apia` | UTC+13 | Samoa (Apia) |

---

## UTC / GMT

| Identifier (`ZoneId`) | UTC Offset | Description |
|---|---|---|
| `UTC` | UTC+0 | Coordinated Universal Time |
| `Etc/UTC` | UTC+0 | Canonical UTC identifier |
| `GMT` | UTC+0 | Greenwich Mean Time |
