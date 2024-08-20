#!/bin/bash
# stop all app instances

# INT is enough to trigger the registered shutdown hook, feel free to use KILL
ARG='INT'

ps -ef | grep 'distributed-url-shortener.*\.jar' | grep -v grep | awk '{print $2}' | xargs kill -$ARG

