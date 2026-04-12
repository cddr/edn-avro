# 📜 Release Checklist

* [ ] All tests pass: `bin/kaocha-matrix`
* [ ] The working directory is clean: `git status` should show no uncommitted changes.
* [ ] You have a Clojars [Deploy Token](https://clojars.org/tokens) set in your environment as `CLOJARS_PASSWORD`.

## 1. Create an Annotated Tag

```bash
# Replace v1.0.x with the next logical version
git tag -a v1.0.x -m "Brief summary of changes and spec updates"
```

## 2. Push to Origin

```bash
git push origin vx.y.z
```

## 4. Deploy to Clojars

```bash
clojure -T:build deploy
```
