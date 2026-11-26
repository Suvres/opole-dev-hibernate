# Odczyt wypozyczen i logi OTLP

Projekt wymaga JDK 27, PostgreSQL oraz collectora OTLP. Lokalny `compose.yaml`
uruchamia PostgreSQL i Grafana LGTM. Schemat z `init.sql` jest tworzony tylko
przy pierwszym uruchomieniu nowego wolumenu bazy; `seed_1m.sql` zawiera dane
testowe. Sprawdz jego zawartosc przed uruchomieniem.

Uruchom aplikacje przez `.\gradlew.bat bootRun`, a nastepnie wykonaj jeden z
ponizszych requestow w PowerShell:

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/wypozyczenia/odczyt/EAGER
Invoke-RestMethod -Method Post http://localhost:8080/api/wypozyczenia/odczyt/LAZY
Invoke-RestMethod -Method Post http://localhost:8080/api/wypozyczenia/odczyt/HIBERNATE_STREAM
Invoke-RestMethod -Method Post http://localhost:8080/api/wypozyczenia/odczyt/HIBERNATE_BATCH
Invoke-RestMethod -Method Post http://localhost:8080/api/wypozyczenia/odczyt/JDBC_BATCH
```

Kazde wywolanie czyta cala tabele i wykonuje wlasna transakcje tylko do odczytu:

| Wariant | Sposob odczytu |
|---|---|
| `EAGER` | JPA `findAll()` z grafem: uzytkownik, egzemplarz, ksiazka, powiazania z autorami i autorzy. |
| `LAZY` | JPA `findAll()` bez grafu; relacje pozostaja niezainicjalizowane. |
| `HIBERNATE_STREAM` | Strumien projekcji Hibernate obejmujacej wypozyczenie, uzytkownika, egzemplarz, ksiazke i autorow; hint `readOnly` i JDBC fetch size 1000. |
| `HIBERNATE_BATCH` | Ta sama projekcja; `EntityManager.clear()` po kazdych 1000 wypozyczeniach. |
| `JDBC_BATCH` | Kursor `JdbcTemplate`, fetch size 1000, mapowanie wypozyczenia, uzytkownika, egzemplarza, ksiazki i wszystkich autorow. |

Przetwarzanie demonstracyjne liczy rekordy i sume kontrolna z obu czesci klucza
wypozyczenia, bez logowania danych osobowych. To nie jest eksport danych ani
pelne porownanie rownowaznych grafow: LAZY nie inicjalizuje relacji, a JDBC,
warianty strumieniowe i EAGER pobieraja takze powiazania z autorami.
JDBC grupuje wiersze SQL wedlug obu czesci klucza wypozyczenia, zwracajac
jeden obiekt z lista autorow; metadane ksiazki sa tekstem JSON. Sortowanie
wymagane do grupowania moze wplywac na czas i zasoby bazy. Dla niezmieniajacych sie danych
liczba i suma powinny byc takie same. Warianty `findAll()` moga zajac duzo
pamieci, a EAGER
powiela wiersze SQL dla ksiazek z wieloma autorami. Nie uruchamiaj ich
rownolegle na duzym zbiorze bez odpowiedniego limitu pamieci.

Odpowiedz i log koncowy zawieraja identyfikator uruchomienia, liczbe rekordow,
sume kontrolna, czas oraz zajetosc sterty JVM przed i po odczycie. Zajetosc
sterty jest chwilowym pomiarem calej JVM, nie maksimum ani niezaleznym pomiarem
pojedynczego zadania; wplywaja na nia GC i inne watki. Czas nie obejmuje
zatwierdzenia transakcji ani przeslania odpowiedzi HTTP. Warianty paczkowe
loguja postep co 1000 rekordow; rozmiar fetch size to wskazowka sterownika,
nie osobne zapytanie SQL na kazda paczke. PostgreSQL wymaga transakcji z
wylaczonym autocommit, ktora zapewnia serwis; strumienie sa zamykane przed
koncem transakcji.

Logback wysyla logi tego odczytu na konsole i do skonfigurowanego eksportera
OTLP (`http://localhost:4318/v1/logs`); w Grafanie na `http://localhost:3000`
mozna wyszukiwac je w Loki po `service_name="biblioteka-api"` i tresci
`Odczyt wypozyczen`. Eksport jest asynchroniczny i moze miec opoznienie.
Endpointy sa przeznaczone do lokalnych pomiarow, nie maja zabezpieczen ani
ograniczenia wspolbieznosci i nie powinny byc wystawiane publicznie.
