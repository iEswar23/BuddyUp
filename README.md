# BuddyUp

**Find your people, nearby.** BuddyUp is a friendship app that helps you meet friendly people in your city who share your interests — for coffee walks, board-game nights and weekend hikes. Not a dating app: just good company.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-FF7A59)
![Hilt](https://img.shields.io/badge/DI-Hilt-14A79D)
![Room](https://img.shields.io/badge/Database-Room-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-informational)
![License](https://img.shields.io/badge/License-MIT-blue)

## Features

- **Onboarding & profile setup** — a three-page `HorizontalPager` intro with an animated page indicator, then a profile form (name, age range, city, bio, 3–8 interests) with validation. Everything is persisted in DataStore, and onboarding is skipped on later launches.
- **Discover** — a stack of profile cards with gradient initials avatars, distance, an "active now" status and highlighted shared interests.
  - Drag a card to swipe it: it rotates as it moves, springs back if you let go early, and shows **Wave** or **Pass** stamps. The Pass and Wave buttons play the same fly-out animation.
  - Each card shows a **compatibility ring** computed by `MatchScorer`, which combines Jaccard similarity of interests, distance falloff and recent activity.
  - A **filter bottom sheet** lets you set maximum distance (or "Anywhere"), an age range slider and must-have interests, and shows a live count of matching people.
  - You can undo your last pass, and the empty state offers "Search anywhere" and "Revisit passed profiles".
- **Waves (requests)** — Incoming and Sent tabs in a swipeable pager with count badges.
  - Wave back or dismiss incoming waves, or withdraw waves you sent. Each of these shows a snackbar with **Undo**.
  - Some people wave back on their own a few seconds after you wave at them. That makes you buddies and triggers an in-app alert with a "Say hi" shortcut.
- **Friends** — your buddies sorted by recent activity, with search (by name, neighborhood or interest), green online dots, last-message previews, unread badges and an "Online now" row.
- **Chat** — conversations stored in Room, with bubbles grouped by sender and day ("Today", "Yesterday"…) and timestamps.
  - An animated typing indicator appears before the other person replies. Replies are chosen from a small reply bank based on what you wrote (a plan, a question, a greeting…).
  - The conversation auto-scrolls to the newest message. The chat also offers quick icebreaker suggestions and a profile sheet for your buddy.
- **Meetups** — local group events (coffee walks, chess in the park, food walks, hikes…) grouped by day.
  - Filter by your city, events you're going to, all cities, or category.
  - Join or leave with an attendee count that stays consistent, a capacity bar and a "Full" state. Cards expand to show details, and you can pull to refresh.
- **Profile & settings** — profile header and stats, edit profile, a **System/Light/Dark** theme toggle and an in-app alerts toggle (both persisted in DataStore), community guidelines and an About dialog.
- **Polish** — splash screen, adaptive and monochrome launcher icon, edge-to-edge layout, Nunito rounded typography, coral/peach and teal light and dark color schemes, shimmer skeletons, animated empty states and item placement animations.

## Tech stack

| Layer | Libraries |
| --- | --- |
| Language | Kotlin 2.0, Coroutines, Flow / StateFlow |
| UI | Jetpack Compose (BOM 2024.12), Material 3, Navigation Compose, Foundation Pager, Material Icons Extended |
| Architecture | MVVM, Repository pattern, unidirectional data flow, sealed `UiState`s |
| DI | Hilt (`@HiltViewModel`, `@Binds` / `@Provides` modules, qualifiers for dispatchers and app scope) |
| Persistence | Room (entities, DAOs, JOIN + sub-query projections, transactions), DataStore Preferences |
| Networking | Retrofit 2 + Gson, OkHttp with a `MockInterceptor` that serves JSON from `assets/api` with 300–700 ms latency |
| App start | `core-splashscreen`, adaptive icons, edge-to-edge |
| Testing | JUnit 4, Google Truth, kotlinx-coroutines-test, hand-written fake repositories, Robolectric + Roborazzi screenshot tests (Hilt testing) |
| CI | GitHub Actions: `assembleDebug` + `testDebugUnitTest` on every push and pull request |

## Architecture

The app follows an offline-first MVVM architecture. **Room is the single source of truth**: on first launch, `DataSynchronizer` fetches people, incoming waves, existing friendships with chat history, and meetups from the Retrofit API. It caches all of this in a single transaction. Screens only ever observe Room through repository `Flow`s.

```mermaid
flowchart TD
    subgraph UI["UI layer (Compose)"]
        S[Screens] -->|events| VM[ViewModels]
        VM -->|StateFlow&lt;UiState&gt;| S
    end
    subgraph Domain["Domain"]
        MS[MatchScorer]
        M[Models · Filters · Validator]
    end
    subgraph Data["Data layer"]
        R[Repositories] --> DB[(Room)]
        R --> DS[(DataStore)]
        SYNC[DataSynchronizer] --> API[Retrofit API]
        API --> MI[OkHttp MockInterceptor<br/>assets/api/*.json]
        SYNC --> DB
        SIM[BuddySimulator] --> DB
    end
    VM --> R
    VM --> MS
    R --> SYNC
    R --> SIM
```

- **Repositories** (`PeopleRepository`, `RequestsRepository`, `FriendsRepository`, `ChatRepository`, `MeetupRepository`, `UserRepository`) are interfaces bound with Hilt `@Binds`, so ViewModels are tested against simple fakes.
- **`MatchScorer`** is pure Kotlin. The score is 60% interests (√Jaccard), 25% distance (linear falloff to 50 km) and 15% activity, rounded to a 0–100 percentage.
- **Relationships** live in a `connections` table (`PASSED`, `WAVE_SENT`, `WAVE_RECEIVED`, `FRIEND`, `DECLINED`). Discover simply shows people without a connection row, so undo is just restoring or deleting a row.
- **`BuddySimulator`** plays the other side of the community on an application-scoped coroutine scope, so it survives navigation. It handles waving back, typing indicators and debounced replies.
- **Distances** are computed with the haversine formula from the user's city centre (Bengaluru, Hyderabad, Chicago or Austin) to each member's neighborhood.

## Package structure

```
io.github.ieswar23.buddyup
├── BuddyUpApplication.kt / MainActivity.kt
├── data
│   ├── local          # Room database, entities, DAOs, type converters
│   ├── preferences    # DataStore-backed UserRepository
│   ├── remote         # Retrofit API, DTOs, MockInterceptor
│   ├── repository     # Repository interfaces, offline-first implementations, mappers, DataSynchronizer
│   └── simulation     # BuddySimulator, ReplyGenerator, AppEventBus
├── di                 # Hilt modules (app, database, network, repositories) + qualifiers
├── domain
│   ├── model          # Person, FriendRequest, Meetup, UserProfile, DiscoverFilters, ProfileValidator…
│   └── scoring        # MatchScorer
├── ui
│   ├── components     # Avatar, chips, shimmer, empty states, compatibility ring
│   ├── navigation     # Sealed Screen routes + bottom-nav destinations
│   ├── onboarding     # Pager intro
│   ├── setup          # Profile setup / edit form
│   ├── discover       # Card deck, swipe gestures, filter sheet
│   ├── requests       # Incoming / sent waves
│   ├── friends        # Friends list + search
│   ├── chat           # Conversation, grouping, typing indicator
│   ├── events         # Meetups
│   ├── profile        # Profile & settings
│   └── theme          # Colors, typography, shapes
└── util               # Formatters, haversine distance, TimeProvider
```

## Getting started

1. Install **Android Studio Ladybug (2024.2.1) or newer** and **JDK 17**.
2. Clone the repository and open the project folder in Android Studio.
3. Let Gradle sync, then run the `app` configuration on an emulator or device (API 24+).

No API keys, Firebase project or network connection are needed. All data comes from bundled JSON served through the mock interceptor.

To build from the command line:

```bash
./gradlew assembleDebug
```

## Testing

```bash
./gradlew testDebugUnitTest
```

The unit tests cover:

- **`MatchScorerTest`** — Jaccard similarity, distance and activity weighting, ordering guarantees and score bounds.
- **`DiscoverViewModelTest`** — card ordering, pass/wave, undo, filters, and the loading → error → empty states.
- **`RequestsViewModelTest`** — accept, decline and withdraw, plus snackbar undo restoring the original request.
- **`ChatViewModelTest`** — sending messages, ignoring blank input, mark-as-read, typing indicator and suggestions.
- **`ChatGroupingTest`**, **`ReplyGeneratorTest`** and **`DiscoverFiltersTest`** — day headers and bubble grouping, reply intent classification, filter matching and profile validation.
- **`AppScreenshotTest`** — launches the real `MainActivity` (Hilt graph, Room, DataStore, mock API) under Robolectric with native graphics, seeds a completed Bengaluru profile, navigates through the tabs and captures each screen.

### Screenshot tests

The screenshots below are generated on the JVM with [Robolectric](https://robolectric.org) and [Roborazzi](https://github.com/takahirom/roborazzi), so no emulator is needed:

```bash
./gradlew recordRoborazziDebug
```

This rewrites the PNGs in `docs/screenshots/`. A plain `./gradlew testDebugUnitTest` still runs the same tests as smoke tests (every screen must load real data without crashing) but does not write or compare images.

## Screenshots

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/01_onboarding.png" width="250" alt="Onboarding"/><br/><sub>Onboarding</sub></td>
    <td align="center"><img src="docs/screenshots/02_discover.png" width="250" alt="Discover card stack"/><br/><sub>Discover card stack</sub></td>
    <td align="center"><img src="docs/screenshots/03_waves.png" width="250" alt="Incoming waves"/><br/><sub>Incoming waves</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/04_friends.png" width="250" alt="Friends and online now"/><br/><sub>Friends and online now</sub></td>
    <td align="center"><img src="docs/screenshots/05_chat.png" width="250" alt="Chat with Kavya"/><br/><sub>Chat with Kavya</sub></td>
    <td align="center"><img src="docs/screenshots/06_meetups.png" width="250" alt="Local meetups"/><br/><sub>Local meetups</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/07_profile_dark.png" width="250" alt="Profile (dark theme)"/><br/><sub>Profile (dark theme)</sub></td>
  </tr>
</table>

## Roadmap

- Replace the mock interceptor with a real backend (Ktor or Firebase) and real-time messaging over WebSockets.
- Push notifications for waves and messages via WorkManager and FCM, honoring the in-app alerts setting.
- Profile photos with on-device moderation, plus verified-member badges for safety.
- Meetup creation flow and a map view of nearby meetups.

## Credits

Typography uses [Nunito](https://fonts.google.com/specimen/Nunito), licensed under the SIL Open Font License 1.1.

## License

This project is released under the [MIT License](LICENSE).

## Author

**Eswar Reddy Madhira** — Android Developer

[![LinkedIn](https://img.shields.io/badge/LinkedIn-eswar--reddy--android-0A66C2?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/eswar-reddy-android)
[![GitHub](https://img.shields.io/badge/GitHub-iEswar23-181717?logo=github&logoColor=white)](https://github.com/iEswar23)
