# MultiMover

This program lets you rename files that match a specified first pattern, written using regular expressions, to a new name based on a designated second patterm.

Example:
  When file "helphelp.hlp" is existing, then it will be renamed to "help_help.hxt" by the following command.

```bash
% java -cp target/classes jp.hatano.multimover.MultiMover '%1{h.*p}%2{h.*}.hlp' '%1_%2.hxt'
```
or when running with jar file built with `mvn package`:

```bash
% java -jar target/multimover-1.0-SNAPSHOT.jar '%1{h.*p}%2{h.*}.hlp' '%1_%2.hxt'
```
## options

There are two options available:

- `-d` or `--dry-run`: This option allows you to run the program in dry-run mode, where it will only display the changes that would be made without actually renaming any files.
- `-v` or `--verbose`: This option enables verbose output, providing more detailed

In most cases, it is difficult to use patterns describing correctly in the first time, so dry-run options is provided to check the correctness of the pattern provided and new name criteria.

Like:

```bash
% java -cp target/classes jp.hatano.multimover.MultiMover -d '%1{h.*p}%2{h.*}.hlp' '%1_%2.hxt'
```

Then the output show you 

```text
MultiMover : Would rename: helphelp.hlp -> help_help.hxt
```

If the pattern does not match to any existing files, it will show:

```text
MultiMover : No files matched the pattern: '%1{h.*p}%2{h.*}.hlp'
```
