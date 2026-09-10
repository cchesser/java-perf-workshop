# Third-Party Notices

## Compliance assessment

The project is using the third-party components in a way that is generally
consistent with their published permissive/open-source license terms:

1. MAT is redistributed as unmodified vendor JARs. Their manifests identify
   EPL-2.0; the HPROF bundle also contains the separately identified MIT
   DEFLATE code. The corresponding license texts are retained in `licenses/`.
2. The MAT Calcite implementation is copied into this repository as source.
   It is attributed to the upstream project and identified as Apache-2.0 below.
3. Calcite and the other Maven libraries are declared as dependencies rather
   than copied into this repository or shaded into the application JAR. Maven
   resolves them at build/run time, and `scripts/run-server.sh` places those
   JARs on the runtime classpath. Their original artifact metadata and license
   files therefore remain with the resolved artifacts.
4. This file records the direct dependencies and the license families of their
   resolved transitive dependencies. If a release starts bundling those Maven
   JARs into a distribution archive, the release process must include the
   license/NOTICE files from each resolved artifact as well; this repository
   does not currently create such a bundle.

This is a practical project-level inventory, not legal advice. License terms
can change between dependency versions, so the exact resolved dependency tree
and each artifact's `META-INF/LICENSE*`/`META-INF/NOTICE*` files should be
checked as part of a release review.

## Maven dependencies

The following are the notable direct dependencies declared in `pom.xml`.
Links point to the upstream project or its license information.

| Component | Version | License / attribution | Why it is present |
| --- | --- | --- | --- |
| Jackson Databind, Core, Annotations, JSR310 | 2.18.3 | Apache-2.0 | MCP and JSON serialization |
| Eclipse Platform Runtime, Resources, Commands | 3.31.0 / 3.22.0 / 3.12.0 | EPL-2.0 | MAT runtime services |
| ICU4J | 76.1 | ICU License | Eclipse/MAT internationalization support |
| Apache Calcite Core | 1.41.0 | Apache-2.0 | SQL parser, planner, and execution engine |
| Guava | 33.5.0-jre | Apache-2.0 | Embedded MAT Calcite implementation |
| Janino and Commons Compiler | 3.1.12 | BSD-3-Clause | Calcite enumerable code generation |
| JUnit Jupiter | 5.12.2 | EPL-2.0 | Tests only |
| AssertJ Core | 3.27.3 | Apache-2.0 | Tests only |

Calcite brings additional runtime artifacts, including Avatica, JTS, Proj4J,
Jackson YAML, SnakeYAML, SLF4J, Apache Commons components, protobuf,
json-path/json-smart, ASM, and related support libraries. These are transitive
dependencies resolved from Maven Central; their license metadata must be
retained if the artifacts are redistributed. The versions actually resolved
can be inspected with:

```bash
mvn dependency:tree -Dscope=runtime
```

The project does not claim that one license applies to all Calcite
transitives: each artifact remains under its own upstream license.

Useful upstream license references:

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
- [ICU License](https://icu.unicode.org/license)
- [Apache Calcite](https://calcite.apache.org/) and its [license](https://github.com/apache/calcite/blob/main/LICENSE)
- [Guava](https://github.com/google/guava/blob/master/LICENSE)
- [Janino](https://github.com/janino-compiler/janino/blob/master/LICENSE)
- [JUnit](https://github.com/junit-team/junit5/blob/main/LICENSE.md)
- [AssertJ](https://github.com/assertj/assertj/blob/main/LICENSE)

## Redistributed source and artifacts

The Maven build downloads Eclipse Memory Analyzer (MAT) plugin JARs into `target/mat/`.

It also incorporates the headless MAT schema and function implementation from
[`vlsi/mat-calcite-plugin`](https://github.com/vlsi/mat-calcite-plugin), used under
the Apache License 2.0. The embedded sources are under
`src/main/java/com/github/vlsi/mat/calcite/`; the upstream project and license
are available at the linked repository.

## Included Artifacts

1. `org.eclipse.mat.api.jar` (Bundle-Version `1.16.1.202501091339`)
   - Declared license: `EPL-2.0`
2. `org.eclipse.mat.parser.jar` (Bundle-Version `1.16.1.202501091339`)
   - Declared license: `EPL-2.0`
3. `org.eclipse.mat.report.jar` (Bundle-Version `1.16.1.202501091339`)
   - Declared license: `EPL-2.0`
4. `org.eclipse.mat.hprof.jar` (Bundle-Version `1.16.1.202501091339`)
   - Declared licenses: `EPL-2.0`, `MIT` (for bundled DEFLATE library code)

License declarations above are sourced from each JAR's `META-INF/MANIFEST.MF` (`Bundle-License`) and `about.html`.

## License Texts and Links

1. Eclipse Public License 2.0 (EPL-2.0)
   - Local copy: `licenses/EPL-2.0.txt`
   - Upstream: https://www.eclipse.org/legal/epl-2.0/
2. MIT License notice for DEFLATE library code included in `org.eclipse.mat.hprof.jar`
   - Local copy: `licenses/MIT-DEFLATE.txt`
   - Upstream notice location in bundled artifact: `about_files/DEFLATE-mit.html`

## Upstream Project

- Eclipse Memory Analyzer (MAT): https://eclipse.dev/mat/
- MAT Calcite plugin: https://github.com/vlsi/mat-calcite-plugin
- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
