# Análisis: ChallengeUala vs proyecto-android

## Repositorio analizado
[rodrigo103/ChallengeUala](https://github.com/rodrigo103/ChallengeUala) — Challenge de Ualá (2022)

---

## Stack técnico

| Aspecto | ChallengeUala | proyecto-android | Recomendación |
|---|---|---|---|
| UI | Fragments + XML | Compose | Compose (correcto) |
| Navigation | Navigation Component + Safe Args (`2.4.0`) | **navigation3** (`1.0.1`) | Usar Navigation Compose (`2.9.0`) |
| DI | Hilt/Dagger | Manual (`DefaultArticlesRepository()`) | Migrar a Hilt |
| Estado | LiveData | StateFlow | StateFlow (correcto) |
| Red | Retrofit + Gson | Retrofit + kotlinx-serialization | kotlinx-serialization (correcto) |
| Imágenes | Glide | Coil | Coil para Compose (correcto) |
| Tests Unitarios | Mockito + MockK + JUnit | Faltan tests | Agregar tests |
| Tests de Red | MockWebServer | No tiene | Agregar MockWebServer |
| Memoria | LeakCanary | No tiene | Agregar LeakCanary |
| Logging Network | HttpLoggingInterceptor dedicado | No tiene logging visible | Agregar |

## Lo que usa ChallengeUala (buenas prácticas)

### Navigation Component clásico
- XML nav graph (`nav_graph.xml`) con fragmentos
- Safe Args para paso de argumentos type-safe
- Animaciones de transición (`slide_in_right`, `slide_out_left`)
- Librería madura, estable, usada en producción

### Hilt para DI
- `@Module`, `@Provides`, `@InstallIn(SingletonComponent::class)`
- `@HiltViewModel` para inyectar dependencias en ViewModels
- `@AndroidEntryPoint` para Activities/Fragments
- Escalable y testeable

### Tests unitarios completos
- ViewModel tests con Mockito `mock()` / `whenever()`
- LiveData test utilities (`getOrAwaitValue()`)
- Test constants separados (data classes de prueba)
- Coroutines test support

### LeakCanary
- Detección automática de memory leaks en debug builds
- `LeakCanary.config = LeakCanary.config.copy(watchDurationMs = 1000)`

## Conclusiones

1. **navigation3 es experimental y trae problemas** — la app de producción de Ualá usó Navigation Component clásico, que es lo correcto.
2. **La migración a Navigation Compose** es el camino correcto: misma API moderna que Compose pero estable y madura.
3. **Falta Hilt** — la inyección manual funciona para un challenge chico, pero no escala.
4. **Faltan tests** — el ChallengeUala tiene tests de ViewModel y red; el proyecto actual no tiene coverage.
5. **LeakCanary** es un must-have para debug builds en cualquier app Android.
