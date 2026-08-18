"""Dependency-free contract checks for the isolated Android model package.

These tests intentionally inspect the bundled catalog and source-level safety
contracts instead of reimplementing the Kotlin code. They can run on a build
host before Android/JUnit dependencies are introduced.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
MODEL = PROJECT / "app/src/main/java/com/jakemalby/odysseusmobile/model"


def source(name: str) -> str:
    return (MODEL / name).read_text(encoding="utf-8")


def bundled_catalog() -> dict:
    match = re.search(
        r'private const val JSON = """\s*(.*?)\s*"""',
        source("S25ModelCatalog.kt"),
        flags=re.DOTALL,
    )
    assert match, "The built-in catalog JSON constant was not found"
    return json.loads(match.group(1))


def test_bundled_s25_catalog_has_three_distinct_valid_profiles() -> None:
    catalog = bundled_catalog()
    assert catalog["schemaVersion"] == 1
    profiles = catalog["profiles"]
    assert len(profiles) == 3
    assert len({profile["id"] for profile in profiles}) == len(profiles)

    for profile in profiles:
        assert re.fullmatch(r"[a-z0-9][a-z0-9._-]*", profile["id"])
        assert profile["format"] == "LITERT_LM"
        assert profile["minimumApiLevel"] >= 26
        assert profile["minimumAvailableRamBytes"] > 0
        assert profile["minimumFreeStorageBytes"] > 0
        assert profile["preferredBackends"]
        assert profile["capabilities"]


def test_bundled_catalog_is_metadata_only_not_a_download_mechanism() -> None:
    catalog_text = source("S25ModelCatalog.kt")
    assert "https://" not in catalog_text
    assert "java.net" not in catalog_text
    assert "URLConnection" not in catalog_text
    assert '"sha256"' not in catalog_text


def test_artifact_activation_requires_validation_and_atomic_move() -> None:
    artifact_store = source("ModelArtifactStore.kt")
    assert "output.fd.sync()" in artifact_store
    assert "ATOMIC_MOVE" in artifact_store
    assert "Files.move(source.toPath(), target.toPath(), *options)" in artifact_store
    assert "check(validateStaged(entry) == ArtifactValidation.Valid)" in artifact_store
    assert "AtomicMoveNotSupportedException" in artifact_store


def test_catalog_entry_requires_https_canonical_hash_and_positive_limits() -> None:
    domain = source("ModelDomain.kt")
    assert 'artifactUri.startsWith("https://")' in domain
    assert "Sha256.isCanonical(sha256)" in domain
    assert "require(byteSize > 0)" in domain
    assert "require(minRamBytes > 0)" in domain
    assert "require(supportedBackends.isNotEmpty())" in domain
