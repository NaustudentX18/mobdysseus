#!/usr/bin/env python3
"""AFK structural tests for the pure-Kotlin MOB-003 policy contract.

This intentionally uses only Python's standard library so it can run before
Android/Gradle dependencies are available in CI.
"""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/jakemalby/odysseusmobile/capability/CapabilityPolicy.kt"


def require(text: str, needle: str) -> None:
    assert needle in text, f"missing required policy contract: {needle}"


def main() -> None:
    text = SOURCE.read_text(encoding="utf-8")
    require(text, "package com.jakemalby.odysseusmobile.capability")
    for contract in (
        "enum class CapabilityId",
        "enum class RiskLevel",
        "enum class SideEffect",
        "enum class DataScope",
        "data class CapabilityDescriptor",
        "object CapabilityDenyRules",
        "fun isDenied(id: CapabilityId): Boolean",
        "sealed interface CapabilityCall",
        "enum class ApprovalDecision",
        "enum class ExecutionState",
        "class InMemoryCapabilityLedger",
        "class CapabilityExecutionPolicy",
        "fun request(call: CapabilityCall): PolicyOutcome",
        "fun approve(requestId: String, approved: Boolean): PolicyOutcome",
        "fun beginExecution(requestId: String)",
        "fun auditTrail(): List<PolicyRecord>",
    ):
        require(text, contract)

    # These values must remain defined but unregistered in CapabilityCatalog.
    catalog_body = text.split("object CapabilityCatalog", 1)[1].split("sealed interface CapabilityCall", 1)[0]
    for denied in ("SUBPROCESS", "ARBITRARY_FILESYSTEM", "SOCKET", "UNRESTRICTED_FETCH", "MCP_EXECUTION"):
        require(text, denied)
        assert f"CapabilityDescriptor(CapabilityId.{denied}" not in catalog_body, f"deny-listed capability became allowlisted: {denied}"
    require(text, "if (CapabilityDenyRules.isDenied(call.capability))")

    # The contract must stay platform-neutral and avoid generic/unbounded calls.
    for forbidden in ("android.", "java.net.Socket", "ProcessBuilder", "Runtime.getRuntime", "Map<String", "JSONObject"):
        assert forbidden not in text, f"forbidden non-policy dependency or untyped payload: {forbidden}"

    print("OK: MOB-003 capability policy contract is typed, auditable, and centrally deny-listed.")


if __name__ == "__main__":
    main()
