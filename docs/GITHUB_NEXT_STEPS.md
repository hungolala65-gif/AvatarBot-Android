# GitHub build: next steps

The connected GitHub account was accessible but its repository listing was empty on 2026-11-21. The assistant cannot publish code or trigger a workflow until a repository exists and is visible through the connection.

1. Log in to GitHub as the linked user, create a **private** repository at https://github.com/new (suggested name: `AvatarBot-Android`). Do not initialize it with sample code.
2. Provide the new GitHub repository URL in this chat. The assistant can then use the GitHub connection to upload the source and run supported operations (workflow dispatch availability depends on connector permissions).
3. To upload manually, extract this ZIP and upload **the contents** including hidden `.github`, `android`, `tools`, `go-mobilecore`, not the ZIP file itself.
4. Go to Actions > `Build AvatarBot Android (v0.6 Java ME core)` > Run workflow. If successful, download the `avatarbot-android-v0.6-debug` artifact and extract `app-debug.apk`. If unsuccessful, inspect `avatarbot-v0.6-build-diagnostics` and Actions logs.

Do not upload game account credentials.
