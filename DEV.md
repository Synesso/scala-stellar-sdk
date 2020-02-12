# Development

## Ideas

Looking for something to work on? Check the [issues list](https://github.com/Synesso/scala-stellar-sdk/issues). 


## Compiling & Testing.

`sbt test it:test`

To generate a test coverage report:

`sbt coverage test it:test coverageReport`


## Deployment Checklist

1. Update & push CHANGELOG.md & README.md (version number). If necessary, update any code examples in the README.md for API changes.
2. Create pending release in github
3. Tag will have been created. Wait for successful build on travis.org
4. Update `gh-pages` branch, to reintroduce files that the paradox plugin erroneously deletes. (Git log will show a recent file that can be cherry-picked).
5. Sync to maven central from bintray.com
6. Check [documentation](https://synesso.github.io/scala-stellar-sdk/) has correct tag and no missing icons.
7. Check that the artifact can be fetched from maven central.