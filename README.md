# TDBOT by Nwolfhub

## Downloading

Just go to [releases](https://github.com/s0m31-hub/tdbot/releases) page on GitHub and download latest jarfile!

## Starting

On a first start file *token* will be created in a project directory. Insert your api id and api hash from [telegram](https://my.telegram.org/apps) in the following format:

```id:hash
id:hash
```

All other files will be created automatically.

Choose any login method you want and follow guidelines.

When you finish, just type !help in any chat

## Creating animations

### Syntax

* All commands should end with semicolon (;)

* Every command consists from action (before :) and params (after :)

### Action list

* `editTo:text` - edits text. 

* `delay:time` - waits some time before next command

* `print:text` - prints text into console

* `append:time:text` - appends text at the end of previous one. Will make a *time* delay between two characters

* `subtract:time:amount` - subtracts *amount* characters from the end of previous text. Will make a *time* delay between two characters

There is an [example](https://github.com/s0m31-hub/tdbot/blob/main/test.numar) in a project page. A better example for 1.1+ is available [here](https://github.com/s0m31-hub/tdbot/blob/main/test2.numar)


