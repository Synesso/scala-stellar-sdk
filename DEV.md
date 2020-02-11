# Development

## Ideas

Looking for something to work on? Check the [issues list](https://github.com/Synesso/scala-stellar-sdk/issues). 

The milestone currently in focus is 
[closing the gap between current state and the SDF minimum SDK requirements](https://github.com/Synesso/scala-stellar-sdk/milestone/1).



## Compiling & Testing.

`sbt test it:test`


## Deployment Checklist

1. Update & push CHANGELOG.md & README.md (version number)
2. If necesary, update any code examples in the README.md for API changes.
3. Create pending release in github
4. Tag will have been created. Wait for successful build on travis.org
5. Update `gh-pages` branch, to reintroduce files that the paradox plugin erroneously deletes. (Git log will show a recent file that can be cherry-picked).
6. Sync to maven central from bintray.com
7. Check [documentation](https://synesso.github.io/scala-stellar-sdk/) has correct tag and no missing icons.
8. Check that the artifact can be fetched from maven central.