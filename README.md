# AvatarBot Android APK build

This repository contains the build workflow for AvatarBot Android v0.17.

**Important:** The source archive `AvatarBot_Android_Native_v0.17_AuthGuard_Source.zip` must be uploaded to the repository root before the workflow can compile the APK. The archive is not committed yet.

For source privacy, set this repository to **Private** before uploading the Java ME library or any account-related configuration. Never commit account credentials or login logs.

After uploading the source ZIP, open **Actions → Build AvatarBot Android v0.17 from ZIP → Run workflow**. The workflow extracts the archive, tests the headless Java ME integration and compiles an Android debug APK. Build success does not prove real game login or Auto task success.
