# Development

## Ideas

Looking for something to work on? Check the [issues list](https://github.com/Synesso/scala-stellar-sdk/issues). 


## Compiling & Testing.

`sbt test it:test`

To generate a test coverage report:

`sbt coverage test it:test coverageReport`


## Deployment Checklist

1. Update & push CHANGELOG.md & README.md (version number). If necessary, update any code examples in the README.md for API changes.
2. Create pending release in github.
3. `git pull` locally and on the tagged commit `sbt ghpagesPushSite`
4. Check [documentation](https://synesso.github.io/scala-stellar-sdk/) has correct tag and no missing icons. Check links (todo: automate)
5. Check that the artifact can be fetched from jitpack (todo: script this step)
6. Update `stellar-sdk-source-testing` project with newest version (todo: always use latest?)