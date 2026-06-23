# OpenMRS Module Parent POM

A batteries-included Maven parent for OpenMRS modules. It wires up dependency
management, formatting, licensing, release tooling, and sensible defaults so
your module's `pom.xml` can stay small and focused.

```xml
<parent>
    <groupId>org.openmrs.maven.parents</groupId>
    <artifactId>maven-parent-openmrs-module</artifactId>
    <version><!-- latest release --></version>
</parent>
```

## Contents

- [What you get](#what-you-get)
- [Defaults you'll want to override](#defaults-youll-want-to-override)
- [Required configuration](#required-configuration)
- [Module metadata properties](#module-metadata-properties)
- [Dependency management](#dependency-management)
- [Resource filtering](#resource-filtering)
- [Spotless (code formatting)](#spotless-code-formatting)
- [License headers](#license-headers)
- [PGP signature verification](#pgp-signature-verification)
- [Build profiles](#build-profiles)

## What you get

- Managed versions for the OpenMRS platform artifacts and every plugin the
  build touches.
- Default test dependencies (`openmrs-api:tests`, `openmrs-test`) already on
  the classpath.
- Resource filtering for `*.xml`, `*.properties`, and `*.txt` under main and
  test sources, plus webapp resources relocated to `web/module`.
- `package-module` goal pre-wired via `maven-openmrs-plugin`.
- Spotless and the Mycila license plugin running automatically, with a CI
  profile that flips them from format-on-build to check-only.
- A release profile that blocks SNAPSHOT dependencies.
- A build number derived from git exposed as `revisionNumber`.
- PGP verification of `org.openmrs` dependencies (off by default — see
  [PGP signature verification](#pgp-signature-verification)).

## Defaults you'll want to override

This POM is pre-filled with OpenMRS's own coordinates. If you're publishing
something outside the OpenMRS organization, override these in your module:

| Section | Why you'd override it |
| --- | --- |
| `<organization>` | You're not OpenMRS LLC. |
| `<scm>` | Points at this repo; yours lives elsewhere. |
| `<issueManagement>` | Defaults to OpenMRS JIRA. |
| `<licenses>` | Defaults to MPL 2.0 + Healthcare Disclaimer. |
| `<distributionManagement>` | Defaults to `mavenrepo.openmrs.org`. Fine for OpenMRS modules; change it if you deploy elsewhere. |

If you keep the default license but your header text differs, see
[License headers](#license-headers) for how to swap the content without
touching the plugin config.

## Required configuration

### Java version

The enforcer rejects builds that don't set a Java version. Use **one** of:

```xml
<!-- Java 8 -->
<maven.compiler.source>8</maven.compiler.source>
<maven.compiler.target>8</maven.compiler.target>
```

```xml
<!-- Java 11+ -->
<maven.compiler.release>11</maven.compiler.release>
```

Setting `maven.compiler.release` below 11 will fail the build — use
`source`/`target` for Java 8.

## Module metadata properties

These are pre-set from `project.parent.*` and get filtered into resources
automatically. Override them only if your module's layout is unusual:

| Property | Default |
| --- | --- |
| `MODULE_ID` | `${project.parent.artifactId}` |
| `MODULE_NAME` | `${project.parent.name}` |
| `MODULE_VERSION` | `${project.parent.version}` |
| `MODULE_PACKAGE` | `${project.parent.groupId}.${project.parent.artifactId}` |
| `openmrsPlatformVersion` | The version of `openmrs-api`/`openmrs-web` to build against. Override to target a newer platform. |
| `openmrsPlatformToolsVersion` | Follows `openmrsPlatformVersion` unless set; used for Spotless's formatter artifact. |

## Dependency management

Managed (you pick which to use) and scoped for module builds:

- `org.openmrs.api:openmrs-api` — `provided`
- `org.openmrs.web:openmrs-web` — `provided`
- `org.openmrs.api:openmrs-api:tests` — `test`
- `org.openmrs.web:openmrs-web:tests` — `test`
- `org.openmrs.test:openmrs-test` (pom) — `test`

Added to every module automatically:

- `openmrs-api` (compile)
- `openmrs-api:tests` (test)
- `openmrs-test` (test)

Child modules that don't need these should exclude them explicitly.

## Resource filtering

`*.xml`, `*.properties`, and `*.txt` under `src/main/resources`,
`src/main/webapp`, and `src/test/resources` are filtered — other file types
are copied verbatim. Webapp resources land under `web/module` in the built
artifact.

## Spotless (code formatting)

Java sources are formatted against the OpenMRS Eclipse formatter bundled in
`openmrs-tools`, with UNIX line endings. Formatting runs on every build.

The Spotless plugin version is selected automatically based on the JDK you're
building with, so you don't need to think about it.

On CI (`env.CI` set), Spotless runs in `check` mode instead of `apply` — the
build fails on unformatted code rather than silently fixing it.

## License headers

The Mycila `license-maven-plugin` stamps a header into Java, XML (except
`pom.xml`), properties, JS, CSS, SCSS, and SASS files during
`process-sources`. In CI the goal flips to `check`.

### Change the header text

Two options:

- **Inline** — override the `openmrs.license.header.inline` property with the
  text you want. The default is the MPL 2.0 + Healthcare Disclaimer blurb.
- **From a file** — set `-Dopenmrs.license.header.file=path/to/header.txt`.
  This activates the `license-from-file` profile and supersedes the inline
  default.

### Skip specific file types

Each of these opts a file type out of the license check:

| Flag | Skips |
| --- | --- |
| `-Dopenmrs.license.skip.xml=true` | `*.xml` |
| `-Dopenmrs.license.skip.properties=true` | `*.properties` |
| `-Dopenmrs.license.skip.styles=true` | `*.css`, `*.scss`, `*.sass` |

### Exclude specific files

To exclude individual files or paths (e.g., third-party sources under a
different license), append to the plugin's `<excludes>` list in your
module's `pom.xml`. The `combine.children="append"` attribute is required —
without it, the child list replaces the parent's excludes and you'll lose
the inherited defaults (`**/pom.xml`, `**/target/**`, etc.).

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.mycila</groupId>
            <artifactId>license-maven-plugin</artifactId>
            <configuration>
                <licenseSets>
                    <licenseSet>
                        <excludes combine.children="append">
                            <exclude>src/main/resources/third-party/**</exclude>
                            <exclude>src/main/java/org/example/vendor/SomeFile.java</exclude>
                        </excludes>
                    </licenseSet>
                </licenseSets>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Patterns are Ant-style globs relative to the module's basedir.

## PGP signature verification

The parent wires in the `openmrs-pgpverify-maven-plugin`, which checks that the
`org.openmrs` dependencies a build resolves are signed by the OpenMRS GPG key.
Artifacts outside the verified groupIds are ignored, so the check is
version-independent and doesn't drag in third-party signing concerns.

**It is off by default.** The `org.openmrs` / `org.openmrs.module` namespace is
shared across the community — long-standing distributions such as kenyaemr and
ugandaemr publish under it without being signed by OpenMRS Inc. However, for
OpenMRS community managed artifacts, we should always turn this on.

### Enable it

Flip `openmrs.pgpverify.skip` to `false` — either per build:

```sh
mvn verify -Dopenmrs.pgpverify.skip=false
```

or permanently in your module's `pom.xml`:

```xml
<properties>
    <openmrs.pgpverify.skip>false</openmrs.pgpverify.skip>
</properties>
```

Once enabled, the `verify` goal runs in the `verify` phase and fails the build
if a checked artifact is unsigned or signed by an unexpected key.

### Tuning

The plugin reads these properties (all optional):

| Property | Default | Effect |
| --- | --- | --- |
| `openmrs.pgpverify.skip` | `true` (set by this parent) | Skip verification entirely. |
| `openmrs.pgpverify.failOnMissingSignature` | `true` | Fail the build when a checked artifact has no signature. Set to `false` to warn instead. |
| `openmrs.pgpverify.verifySnapshots` | `false` | Also verify SNAPSHOT artifacts. |
| `openmrs.pgpverify.keyServer` | plugin default | Key server to fetch public keys from. |
| `openmrs.pgpverify.keysFile` | plugin default | Override the map of allowed signing keys. |

## Build profiles

| Profile | Activation | Effect |
| --- | --- | --- |
| `ci` | `env.CI` is set | Spotless and the license plugin switch from `apply`/`format` to `check`. Sources and Javadoc jars are attached. |
| `release` | Wired into `maven-release-plugin` | Enforcer blocks SNAPSHOT dependencies. |
| `enforce-compiler-source-target` | `maven.compiler.release` **not** set | Requires `source` and `target` to be set and valid. |
| `enforce-compiler-release` | `maven.compiler.release` set | Requires `release` to be 11 or higher. |
| `license-jdk11` | JDK range | Picks the Maven License plugin version compatible with your JDK. |
| `spotless-jdk11` / `spotless-jdk17` | JDK range | Picks the Spotless plugin version compatible with your JDK. |
| `license-from-file` | `openmrs.license.header.file` is set | Loads the license header from a file. |
| `license-skip-xml` / `license-skip-properties` / `license-skip-styles` | Matching skip property is `true` | Excludes the file type from the license header. |
| `parent-pom` | Manually activated | This changes the distributionManagement repo. It is only intended to be used by this POM itself.  |
