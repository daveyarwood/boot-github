# boot-github

[![Clojars Project](https://img.shields.io/clojars/v/io.djy/boot-github.svg)](https://clojars.org/io.djy/boot-github)

A library providing common Git and GitHub development tasks for [Boot](http://boot-clj.com).

## Caveats

This library was thrown together out of necessity for a project I'm working on.
I wanted to automate the process of pushing a new git version tag to the remote,
creating a new release with a description derived from the CHANGELOG, and upload
assets to the release.

 I initially explored doing this "the right way" by using [JGit][jgit] and
[tentacles][tentacles], filling in the gaps by making HTTP requests using
[clj-http][http], but I ended up running into issues that would take a
significant amount of development to work through:

- It turns out that pushing to a Git remote is difficult to do programmatically
  via JGit because of the way credentials work. Chances are that most developers
  already have command-line `git` configured properly, so I opted to shell out
  and use `git` instead.  There is no need to set up credentials for this if you
  already have them set up for normal use with `git`.

- It turns out that using the GitHub API to upload assets to a release is
  difficult to do programmatically via clj-http because the API requires
  [SNI][sni], and clj-http apparently doesn't support it, from what I can tell.
  Using `curl` works, though, so I opted to shell out and use `curl`.

[jgit]: https://eclipse.org/jgit
[tentacles]: https://github.com/irresponsible/tentacles
[http]: https://github.com/dakrone/clj-http
[sni]: https://en.wikipedia.org/wiki/Server_Name_Indication

Because we're cutting corners and shelling out, there is a small chance that
this library won't work for you if your environment happens to have a different
version of `curl`, or if you don't have `git` configured just right, etc.

If you run into any problems, please open an issue and we'll see if we can
figure it out.

## Usage

Add `io.djy/boot-github` to your `build.boot` dependencies and require/refer in the tasks:

```clojure
(set-env! :dependencies '[[io.djy/boot-github "X.Y.Z" :scope "test"]])

(require '[io.djy.boot-github :refer (push-version-tag create-release)])
```

### Tasks

#### `push-version-tag`

Creates a git version tag locally and pushes it to the remote.

Equivalent to `git tag -a "X.X.X" -m "X.X.X"; git push --tags`.

##### Example

Command line:

```bash
boot push-version-tag -v 0.0.1
```

build.boot (inside a task definition):

```clojure
(push-version-tag :version "0.0.1")
```

#### `create-release`

Creates a new release via GitHub's Releases API.

Optionally, you can provide the paths to any file assets you'd like to include
in the release.

##### `--changelog` option

There is a `--changelog` option, which will use the changes for this particular
version as the description of the release. Note that this requires your
CHANGELOG.md to follow this template:

```
# CHANGELOG

## 0.0.5 (optional: you can include any other text on this line, like the release date, etc.)

Any text can go here to describe the changes for this release.

This can have multiple lines, of course.

## 0.0.4

More changes here, etc.

## 0.0.3

...
```

Feedback is welcome on this format -- we can potentially tweak it. Open an
issue if you have ideas.

##### Example

Command line:

```bash
boot create-release -v 0.0.1 --changelog -a program1 -a program2.exe
```

build.boot (inside a task definition):

```clojure
(create-release :version   "0.0.1"
                :changelog true
                :assets    #{"program1" "program2.exe"})
```

## Contributing

If you're interested in helping to improve this library, feel free to submit a
Pull Request!

## License

Copyright © 2017 Dave Yarwood

Distributed under the Eclipse Public License version 1.0.
