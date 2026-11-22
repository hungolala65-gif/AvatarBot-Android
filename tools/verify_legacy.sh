#!/usr/bin/env bash
# Verify the user-supplied Java ME bytecode without network access or Android SDK.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$ROOT/android/app/libs/bot2x-headless.jar"
[[ -s "$JAR" ]] || { echo "Missing JAR: $JAR" >&2; exit 2; }
TEMP="$(mktemp -d)"; trap 'rm -rf "$TEMP"' EXIT
find "$ROOT/android/app/src/main/java/javax/microedition" -name '*.java' > "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/LegacyBotRuntime.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/LegacySessionInspector.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/LegacyAdvancedSetup.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/LegacyFarmCompatibility.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LegacyOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LoginFlowOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LoginProbeOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LiveSessionInspectorOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LiveSessionIntegrationOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/RmsPersistenceOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LifecycleOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/ConfigModeOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/TaskFlagsOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/RetryPolicyOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LoopIntervalOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/RetryPolicy.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/AuthWatchdogPolicy.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/AuthWatchdogPolicyOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/LoginCycleEvidenceOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/StartAdmissionPolicy.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/StartAdmissionPolicyOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/android/app/src/main/java/com/avatarbot/mobile/legacy/NetworkWaitPolicy.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/NetworkWaitPolicyOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/ProbeIdentityOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/SeedModeOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/AdvancedOptionsOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/AdvancedSetupV15Test.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/FarmTradingV18Test.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/StartTicketOfflineTest.java" >> "$TEMP/sources.txt"
echo "$ROOT/tools/legacy-tests/SocksHandshakeCancellationTest.java" >> "$TEMP/sources.txt"
javac -proc:none -encoding UTF-8 -cp "$JAR" -d "$TEMP" @"$TEMP/sources.txt"
java -cp "$TEMP:$JAR" LegacyOfflineTest
java -cp "$TEMP:$JAR" LoginFlowOfflineTest
java -cp "$TEMP:$JAR" LoginProbeOfflineTest
java -cp "$TEMP:$JAR" LiveSessionInspectorOfflineTest
java -cp "$TEMP:$JAR" LiveSessionIntegrationOfflineTest
java -cp "$TEMP:$JAR" RmsPersistenceOfflineTest
java -cp "$TEMP:$JAR" LifecycleOfflineTest
java -cp "$TEMP:$JAR" ConfigModeOfflineTest
java -cp "$TEMP:$JAR" TaskFlagsOfflineTest
java -cp "$TEMP:$JAR" RetryPolicyOfflineTest
java -cp "$TEMP:$JAR" AuthWatchdogPolicyOfflineTest
java -cp "$TEMP:$JAR" LoginCycleEvidenceOfflineTest
java -cp "$TEMP:$JAR" NetworkWaitPolicyOfflineTest
java -cp "$TEMP:$JAR" ProbeIdentityOfflineTest
java -cp "$TEMP:$JAR" SeedModeOfflineTest
java -cp "$TEMP:$JAR" AdvancedOptionsOfflineTest
java -cp "$TEMP:$JAR" AdvancedSetupV15Test
java -cp "$TEMP:$JAR" FarmTradingV18Test
java -cp "$TEMP:$JAR" LoopIntervalOfflineTest
java -cp "$TEMP:$JAR" StartTicketOfflineTest
java -cp "$TEMP:$JAR" StartAdmissionPolicyOfflineTest
java -cp "$TEMP:$JAR" SocksHandshakeCancellationTest
echo "PASS: Java ME headless integration tested offline"
