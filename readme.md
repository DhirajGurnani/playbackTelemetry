# NewJavaProject

A plain Java project (no build tool), set up for IntelliJ IDEA on JDK 21.

## Layout

```
NewJavaProject/
├── src/
│   └── Main.java        entry point
├── NewJavaProject.iml   IntelliJ module file
└── readme.md
```

Compiled classes go to `out/`, which is git-ignored.

## Run

In IntelliJ: open this folder and run `Main`.

From a terminal:

```
javac -d out src/Main.java
java -cp out Main
```
