Agent Responsibilities
1. Code Changes
   Follow existing architecture (MVVM unless explicitly told otherwise)
   Keep functions small and readable
   Prefer Kotlin idioms (e.g., let, apply, sealed classes)
   Do not introduce unnecessary dependencies
   Maintain backward compatibility unless instructed
2. UI Changes
   Reuse existing components where possible
   Follow Material Design guidelines
   Ensure layouts are responsive across screen sizes
   For Compose:
   Keep composables stateless when possible
   Hoist state to ViewModel
3. Dependency Management
   Add dependencies only when necessary
   Always use stable versions unless specified
   Update build.gradle carefully to avoid conflicts
   Coding Standards
   Kotlin Style
   Use camelCase for variables and functions
   Use PascalCase for classes
   Prefer val over var
   Avoid nullable types unless necessary
   Use data classes for models
   Formatting
   Follow Android Studio default formatter
   Max line length: ~120 characters
   Use meaningful variable and function names
   Architecture Guidelines
   MVVM Pattern
   Model: Data layer (repositories, APIs, database)
   View: Activities/Fragments/Composables
   ViewModel: Business logic and UI state
   Rules
   Do not put business logic in Activities/Fragments
   ViewModels should not reference Views
   Use LiveData / StateFlow for UI updates
   Networking
   Use Retrofit (or existing networking layer)
   Handle errors gracefully
   Avoid blocking the main thread
   Testing
   Unit Tests
   Cover ViewModels and business logic
   Use mocks for dependencies
   UI Tests
   Use Espresso or Compose testing
   Focus on critical user flows
   Performance Guidelines
   Avoid unnecessary object allocations
   Use RecyclerView efficiently (or LazyColumn in Compose)
   Optimize images and resources
   Do not block UI thread
   Security Guidelines
   Never hardcode API keys
   Use secure storage (EncryptedSharedPreferences / Keystore)
   Validate all inputs