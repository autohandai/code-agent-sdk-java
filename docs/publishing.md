# Publishing To Maven Central

The Java SDK publishes through the current Maven Central Portal flow. The
release workflow uses Sonatype's `central-publishing-maven-plugin`, signs the
release artifacts, and can auto-publish after Central Portal validation.

## What Gets Published

The `release` Maven profile attaches the artifacts Maven Central expects:

- Main jar
- Sources jar
- Javadocs jar
- POM metadata
- GPG signatures
- Central Portal deployment bundle

## Required GitHub Secrets

Configure these repository secrets in
`autohandai/code-agent-sdk-java -> Settings -> Secrets and variables -> Actions`:

- `CENTRAL_USERNAME`: Sonatype Central Portal user-token username.
- `CENTRAL_PASSWORD`: Sonatype Central Portal user-token password.
- `MAVEN_GPG_PRIVATE_KEY`: ASCII-armored private GPG key.
- `MAVEN_GPG_PASSPHRASE`: Passphrase for the GPG key.

The workflow writes no credentials into the repository. `actions/setup-java`
creates a temporary Maven `settings.xml` with server id `central` and imports the
GPG key only for the release job.

## Namespace Checklist

Before the first Maven Central release:

1. Create or use the Sonatype Central Portal account for Autohand.
2. Verify the `ai.autohand` namespace in Central Portal.
3. Generate a Central Portal user token.
4. Add the GitHub secrets above.
5. Publish a GitHub release tag such as `v1.0.0`.

## Release Flow

Create a GitHub release:

```bash
git tag v1.0.0
git push origin v1.0.0
gh release create v1.0.0 --title "Java SDK v1.0.0" --notes "Initial Maven Central release."
```

The release workflow will:

1. Resolve `v1.0.0` to Maven version `1.0.0`.
2. Run `mvn verify`.
3. Compile every example with `scripts/validate-examples.sh`.
4. Run `mvn -P release deploy`.
5. Upload to Maven Central Portal.
6. Auto-publish and wait until Central reports the deployment as published.

You can also start the workflow manually from GitHub Actions and choose whether
to auto-publish. When auto-publish is disabled, Central Portal validates the
deployment and waits for a maintainer to publish it in the web UI.

## Developer Consumption

After the first release is published to Maven Central:

```xml
<dependency>
    <groupId>ai.autohand</groupId>
    <artifactId>code-agent-sdk-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Maven Central propagation can take a few minutes after publication.
