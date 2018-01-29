# CHANGELOG

## 0.1.4 (2018-01-29)

* Fixed a bug in the code in the assertion that a file to be used as an asset
  exists. ([#1](https://github.com/daveyarwood/boot-github/issues/1))

## 0.1.3 (2017-10-02)

* Before this release, simply requiring the `io.djy.boot-github` namespace would
  throw an exception if `GITHUB_TOKEN` is not defined in the environment. This
  is really something that should be isolated to the place where you _use_ a
  boot-github task that requires a GitHub API token.

  Now, you don't need to provide a GitHub API token until you use the
  `create-release` task.

* Added a `--github-token` option to the `create-release` task, allowing you to
  provide the token that way.

  If the option is missing, it falls back to `GITHUB_TOKEN`, for backwards
  compatibility.

## 0.1.2 (2017-07-16)

* Print "Uploading assets..." info message before starting to upload assets.

## 0.1.1 (2017-07-16)

* Minor fixes.

## 0.1.0 (2017-07-16)

* Initial release.
