# Third-Party Notices

Mobdysseus is a native Android app distributed as a whole under the
**GNU Affero General Public License, version 3.0 or later** (AGPL-3.0-or-later).
See [LICENSE](LICENSE) for the full text of that license.

This file lists the third-party components distributed with, linked into, or
bundled within Mobdysseus, together with their applicable licenses. Mobdysseus
itself remains AGPL-3.0-or-later; the per-component licenses below apply only
to the specific third-party component named.

## Direct dependencies

| Component | Group / Artifact | License | SPDX | Upstream |
|---|---|---|---|---|
| llmedge | `io.github.aatricks:llmedge` | Apache License 2.0 | `Apache-2.0` | <https://github.com/aatricks/llmedge> |
| multiplatform-markdown-renderer-m3 | `com.mikepenz:multiplatform-markdown-renderer-m3` | Apache License 2.0 | `Apache-2.0` | <https://github.com/mikepenz/multiplatform-markdown-renderer> |
| AndroidX Core KTX | `androidx.core:core-ktx` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/androidx> |
| AndroidX Lifecycle Runtime KTX | `androidx.lifecycle:lifecycle-runtime-ktx` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/androidx> |
| AndroidX Activity Compose | `androidx.activity:activity-compose` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/androidx> |
| Jetpack Compose BOM | `androidx.compose:compose-bom` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/compose> |
| Jetpack Compose UI | `androidx.compose.ui:ui` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/compose> |
| Jetpack Compose UI Graphics | `androidx.compose.ui:ui-graphics` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/compose> |
| Jetpack Compose Material 3 | `androidx.compose.material3:material3` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/compose> |
| Jetpack Compose Material Icons Core | `androidx.compose.material:material-icons-core` | Apache License 2.0 | `Apache-2.0` | <https://developer.android.com/jetpack/compose> |
| JUnit (test scope only) | `junit:junit` | Eclipse Public License 1.0 | `EPL-1.0` | <https://junit.org/junit4/> |

## Transitive dependencies

### Google ML Kit (OCR)

`com.google.mlkit:*` (pulled in transitively by `llmedge` for on-device OCR).

- **License:** Apache License 2.0
- **SPDX:** `Apache-2.0`
- **Upstream:** <https://developers.google.com/ml-kit>

### Bundled native engines (inside llmedge)

The `llmedge` artifact bundles the following native components. Each is
licensed under the **MIT License** (`MIT`):

| Component | License | SPDX | Upstream |
|---|---|---|---|
| llama.cpp / ggml | MIT License | `MIT` | <https://github.com/ggml-org/llama.cpp> |
| whisper.cpp | MIT License | `MIT` | <https://github.com/ggml-org/whisper.cpp> |
| bark.cpp | MIT License | `MIT` | <https://github.com/PABannier/bark.cpp> |

## License texts

The full text of the Apache License 2.0 is included at
[`licenses/APACHE-2.0.txt`](licenses/APACHE-2.0.txt).

The MIT License text for the bundled llama.cpp / ggml, whisper.cpp, and
bark.cpp components is distributed with the upstream `llmedge` sources and
is available at the upstream project URLs listed above.
