package cn.mine.highlight.languages

import cn.mine.highlight.core.Language
import cn.mine.highlight.languages.bash.bash
import cn.mine.highlight.languages.c.c
import cn.mine.highlight.languages.cmake.cmake
import cn.mine.highlight.languages.cpp.cpp
import cn.mine.highlight.languages.csharp.csharp
import cn.mine.highlight.languages.css.css
import cn.mine.highlight.languages.dart.dart
import cn.mine.highlight.languages.diff.diff
import cn.mine.highlight.languages.dockerfile.dockerfile
import cn.mine.highlight.languages.go.go
import cn.mine.highlight.languages.glsl.glsl
import cn.mine.highlight.languages.ini.ini
import cn.mine.highlight.languages.java.java
import cn.mine.highlight.languages.javascript.javascript
import cn.mine.highlight.languages.json.json
import cn.mine.highlight.languages.kotlin.kotlin
import cn.mine.highlight.languages.latex.latex
import cn.mine.highlight.languages.lua.lua
import cn.mine.highlight.languages.markdown.markdown
import cn.mine.highlight.languages.php.php
import cn.mine.highlight.languages.powershell.powershell
import cn.mine.highlight.languages.properties.properties
import cn.mine.highlight.languages.python.python
import cn.mine.highlight.languages.rust.rust
import cn.mine.highlight.languages.ruby.ruby
import cn.mine.highlight.languages.sql.sql
import cn.mine.highlight.languages.swift.swift
import cn.mine.highlight.languages.typescript.typescript
import cn.mine.highlight.languages.xml.xml
import cn.mine.highlight.languages.yaml.yaml

/**
 * Every grammar bundled with the highlighter.
 *
 * Each entry builds a fresh mode tree: compilation mutates modes in place, mirroring `highlight.js`.
 */
internal fun builtinLanguages(): List<Language> = listOf(
    json(),
    ini(),
    cmake(),
    go(),
    glsl(),
    yaml(),
    bash(),
    dockerfile(),
    javascript(),
    typescript(),
    xml(),
    css(),
    dart(),
    java(),
    kotlin(),
    latex(),
    lua(),
    powershell(),
    properties(),
    python(),
    c(),
    cpp(),
    csharp(),
    sql(),
    diff(),
    markdown(),
    rust(),
    ruby(),
    php(),
    swift(),
)
