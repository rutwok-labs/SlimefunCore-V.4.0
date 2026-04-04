# 🤝 Contributing to Slimefun 4.9-UNOFFICIAL

Thanks for your interest in contributing to this project.

This repository is an **unofficial compatibility fork** of Slimefun focused on keeping the plugin working on newer server environments, especially **Purpur/Paper 1.21.11** with **Java 21**.

## 📌 Project Scope

This fork is mainly focused on:

- 🟢 Minecraft `1.21.11` compatibility
- ⚙️ Purpur and Paper startup/runtime support
- 🧩 Dependency and API compatibility fixes
- 🛠️ Stability improvements for this unofficial build
- 📦 Packaging and release fixes for this fork

This repository is **not** intended to replace the original Slimefun project.

## ❤️ Original Project Credit

This project is based on the original **Slimefun** project.

- 👤 Original Creator: **[TheBusyBiscuit](https://github.com/TheBusyBiscuit)**
- 👥 Original Team: **Slimefun Team**
- 🔗 Original Repository: [https://github.com/Slimefun/Slimefun4](https://github.com/Slimefun/Slimefun4)

Please respect the original authors and do not report issues from this unofficial fork to the official Slimefun team unless the issue is confirmed to exist in the original upstream project as well.

## 🐞 Reporting Issues

If you find a bug in this fork, please open an issue in **this repository**, not the original Slimefun repository.

When reporting a bug, include:

- 🖥️ Server software and exact version
- 🎮 Minecraft version
- ☕ Java version
- 📦 Slimefun version from this repo
- 🧪 Steps to reproduce the issue
- 📄 Full console error or stack trace
- 🔌 Installed addons or related plugins

Good bug reports make fixes much faster.

## 🔧 Pull Requests

Pull requests are welcome for changes that fit this fork.

Good pull request targets:

- ✅ Compatibility fixes for `1.21.11`
- ✅ Runtime/API migration fixes
- ✅ Safer startup behavior
- ✅ Packaging and shading fixes
- ✅ Stability improvements
- ✅ Documentation updates for this repo

Please avoid unrelated large feature changes unless they are clearly useful for this fork.

## 🧠 Before Opening a PR

Before submitting a pull request:

1. Make sure your change is relevant to this repository
2. Keep the edit focused and easy to review
3. Test it on a real server when possible
4. Include logs or explanation if the fix is runtime-related
5. Do not remove original project credits

## 💻 Build Instructions

This project uses **Maven**.

To build:

```bash
mvn clean package
```

The built jar will be generated in:

```text
target/Slimefun v4.9-UNOFFICIAL.jar
```

## 🧪 Testing Expectations

If possible, test your changes with:

- 🖥️ Purpur or Paper
- 🎮 Minecraft `1.21.11`
- ☕ Java `21`

Helpful test areas:

- plugin startup
- item loading
- guide/menu opening
- custom heads or textures
- recipe loading
- machine placement and interaction

## 📝 Code Style

Try to keep changes consistent with the surrounding code.

General expectations:

- use clear and readable Java
- avoid unrelated refactors
- keep imports clean
- prefer small focused commits
- add comments only where they help explain non-obvious logic

## ⚠️ Important Notes

- This is an **unofficial build**
- Official Slimefun support does **not** apply to this repository
- Contributions here should stay within the license of the original project
- Give proper credit when reusing or modifying work

## 🙌 Thank You

Thanks to everyone helping improve this unofficial `1.21.11` compatibility build.

Your fixes, tests, and reports help keep the project usable on modern servers.
