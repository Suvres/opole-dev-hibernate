# Odczyt wypożyczeń: JVM, ORM i porównanie wariantów

---

## JVM i schemat pamięci

- JVM uruchamia bytecode Java i zarządza jego wykonaniem.
- **Heap** — obiekty i encje; współdzielony przez wątki.
- **Stosy wątków** — ramki metod, zmienne lokalne i odwołania.
- **Metaspace** — metadane klas; poza heapem.
- **PC i stos natywny** — stan wykonywania wątku oraz obsługa metod natywnych.
- Pamięć natywna obejmuje m.in. bufory bezpośrednie i zasoby JVM.

```text
                  JVM
  +--------------------------------------+
  | Współdzielone obszary                |
  |  Heap (obiekty i tablice; GC)        |
  |   +------------------------------+   |
  |   | Young generation             |   |
  |   |  Eden | Survivor 0 | Survivor 1|  |
  |   +------------------------------+   |
  |   | Old generation               |   |
  |   +------------------------------+   |
  |  Metaspace / obszar metod            |
  |   metadane klas                      |
  +--------------------------------------+
  | Wątek A              | Wątek B       |
  | Java stack           | Java stack    |
  | PC register          | PC register   |
  | Native method stack  | Native stack  |
  +--------------------------------------+
  | Pamięć natywna, np. bufory direct    |
  +--------------------------------------+
```

- Podział heapu na generacje jest typowy, ale szczegóły zależą od JVM i garbage collectora.

---

## Garbage Collector

- Odzyskuje pamięć obiektów na heapie, które nie są już osiągalne.
- Działa automatycznie; moment uruchomienia i odzyskania pamięci nie jest gwarantowany.
- Nie zwalnia zasobów zewnętrznych, takich jak połączenia czy strumienie — te należy zamykać.
- Pomiar zajętości heapu przed i po odczycie jest chwilowy, zależny m.in. od GC; nie pokazuje szczytowego zużycia.

---

## ORM

- ORM mapuje tabele i kolumny relacyjnej bazy na obiekty i pola w aplikacji.
- JPA definiuje standard mapowania; Hibernate jest jego implementacją używaną w projekcie.
- Relacje encji mogą być ładowane od razu (**EAGER**) lub na żądanie (**LAZY**).
- Persistence context śledzi encje i zarządza ich stanem w ramach pracy z bazą.
- Zaleta: praca na obiektach; koszt: dodatkowe zapytania, obiekty i pamięć.

---

## Stany encji Hibernate

- **Transient / new** — nowy obiekt, nieśledzony przez persistence context.
- **Managed / persistent** — encja śledzona; zmiany mogą zostać zapisane przy flush/commit.
- **Detached** — encja odłączona; zmiany nie są automatycznie śledzone.
- **Removed** — encja oznaczona do usunięcia przy flush/commit.
- Przykładowe przejścia: `persist` → managed, `detach`/`clear` → detached, `remove` → removed.

---

## JdbcTemplate

- Springowy interfejs do wykonywania jawnego SQL bez mapowania encji ORM.
- Aplikacja określa kolumny, zapytanie i sposób mapowania wiersza na obiekt.
- W projekcie `queryForStream` odczytuje strumieniowo kursor tylko do przodu.
- `fetchSize = 1000` jest wskazówką dla sterownika co do pobierania wierszy, nie liczbą zapytań SQL.
- Strumień należy zamknąć; odczyt kursora PostgreSQL odbywa się w transakcji.

---

## Założenia doświadczenia

- Porównanie pięciu sposobów odczytu całej tabeli `wypozyczenia`.
- Każdy request uruchamia osobną transakcję tylko do odczytu; warianty należy uruchamiać pojedynczo.
- Wspólny punkt porównania: liczba wierszy i suma kontrolna złożona z UUID oraz daty wypożyczenia.
- Rejestrowane są czas odczytu i chwilowa zajętość heapu przed i po.
- Wyniki zależą m.in. od JVM, GC, sterownika, bazy i rozgrzania aplikacji; pomiar pamięci nie jest maksimum.
- Czas nie obejmuje zatwierdzenia transakcji ani wysłania odpowiedzi HTTP.
- Odczyty nie są równoważne pod względem zakresu pobieranych danych: LAZY nie inicjalizuje relacji, a pozostałe warianty ładują również powiązane dane.

---

## EAGER

- `POST /api/wypozyczenia/odczyt/EAGER`
- `findAll()` z grafem: użytkownik, egzemplarz, książka i autorzy.
- Pokazuje koszt pobrania od razu powiązanego grafu encji.
- Złączenia mogą powielać wiersze SQL, np. dla książek z wieloma autorami.

---

## LAZY

- `POST /api/wypozyczenia/odczyt/LAZY`
- `findAll()` bez grafu encji; relacje pozostają niezaładowane.
- Ładuje listę encji wypożyczeń, ale nie inicjalizuje ich relacji.
- Porównanie z EAGER dotyczy innego zakresu pobieranych danych.

---

## HIBERNATE_STREAM

- `POST /api/wypozyczenia/odczyt/HIBERNATE_STREAM`
- Strumieniowy odczyt projekcji Hibernate obejmującej wypożyczenie, użytkownika, egzemplarz, książkę i autorów, z `readOnly` i `fetchSize = 1000`.
- Przetwarzanie wierszy w locie; projekcja udostępnia pełny powiązany graf danych.

---

## HIBERNATE_BATCH

- `POST /api/wypozyczenia/odczyt/HIBERNATE_BATCH`
- Jak w strumieniu Hibernate: pełna projekcja wypożyczenia i powiązanych danych, `readOnly`, `fetchSize = 1000`, przetwarzanie w locie.
- Dodatkowo `EntityManager.clear()` po każdych 1000 rekordów.
- Czyszczenie ogranicza narastanie persistence context; nazwa wariantu nie oznacza wsadowego zapisu.

---

## JDBC_BATCH

- `POST /api/wypozyczenia/odczyt/JDBC_BATCH`
- Jawny SQL przez `JdbcTemplate`; kursor tylko do przodu i `fetchSize = 1000`.
- Mapuje wypożyczenie, użytkownika, egzemplarz, książkę i autorów; wiersze są grupowane według pełnego klucza wypożyczenia.
- Metadane książki są tekstem JSON; sortowanie potrzebne do grupowania może obciążać bazę.
- Bez tworzenia encji Hibernate i persistence context.

---

## List + EAGER

Post http://localhost:8080/api/wypozyczenia/odczyt/EAGER

---

## List + LAZY

Post http://localhost:8080/api/wypozyczenia/odczyt/LAZY

---

## STREAM

Post http://localhost:8080/api/wypozyczenia/odczyt/HIBERNATE_STREAM

---

## STREAM BATCH

Post http://localhost:8080/api/wypozyczenia/odczyt/HIBERNATE_BATCH

---

## JDBC BATCH
Post http://localhost:8080/api/wypozyczenia/odczyt/JDBC_BATCH